/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.osgi.obr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.felix.bundlerepository.RepositoryAdminImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resolver;

/**
 * The <code>OSGiBundleRepositoryServlet</code> TODO
 *
 */
@Component(metatype=true, immediate=true)
@Properties({
    @Property(name="service.description", value="OSGi Bundle Repository (OBR)"),
    @Property(name="manager.root", value="/"),
    @Property(name="obrLocation", value="obr"),
    @Property(name="obrName", value="internal")
})
public class OSGiBundleRepositoryServlet extends HttpServlet {

    /**
     *
     */
    private static final long serialVersionUID = 7512543420493538557L;

    /**
     * Names of bundles, which are always included in downloaded repositories.
     */
    private static final String[] REQUIRED_BUNDLES = {
        "org.apache.sling.log",
        "org.apache.sling.assembly",
        "org.apache.felix.bundlerepository",
        "org.apache.felix.http.jetty",
        "org.eclipse.equinox.http.servlet",
        "org.apache.sling.sling-servlet-bridge"
    };

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
    private volatile LogService log;

    @Reference
    private HttpService httpService;

    private ComponentContext ctx;

    private ServiceReference ref;

    private Repository repository;

    private String webManagerRoot;

    @Override
    public void init() throws ServletException {
        String location = this.getServletConfig().getInitParameter("obrLocation");
        if (location == null || location.length() == 0) {
            location = "obr";
        }

        // ensure absolute path
        File locFile = new File(location);
        if (!locFile.isAbsolute()) {
            String parent = this.ctx.getBundleContext().getProperty("sling.home");
            if (parent == null || parent.length() == 0) {
                parent = System.getProperty("user.dir");
            }
            locFile = new File(parent, location).getAbsoluteFile();
        }

        if (locFile.exists()) {
            if (!locFile.isDirectory()) {
                throw new ServletException("Repository Location "
                    + locFile.getAbsolutePath() + " is not a directory");
            }
        } else {
            if (!locFile.mkdirs()) {
                throw new ServletException(
                    "Cannot create path to Repository Location "
                        + locFile.getAbsolutePath());
            }
        }

        try {
            String defaultRepoName = this.getServletConfig().getInitParameter(
                "obrName");
            this.repository = new Repository(defaultRepoName, locFile);
        } catch (IOException ioe) {
            throw new ServletException("Cannot load repository properties", ioe);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // send repository.xml if requested
        if (req.getRequestURI().endsWith("/repository.xml")) {
            this.printRepositoryXML(req, resp);
            return;
        }

        // check whether dumping the repository is requested
        if (req.getRequestURI().endsWith("/repository.zip")) {
            this.dumpRepository(req.getParameterValues("bundle"), resp);
            return;
        }

        // check whether a bundle is requested
        String bundle = this.getName(req.getRequestURI());
        try {
            Resource resource = this.repository.getResource(bundle);
            if (resource == null) {
                // TODO: log resource not found
                // throw to prevent returning, is this correct ??
                throw new IOException("Bundle " + bundle
                    + " not known to this repository");
            } else if (req.getParameter("info") != null) {
                this.dumpInfo(resp, req.getRequestURI(), resource);
                // resp.sendRedirect(getParent(req.getRequestURI()));
            } else if (req.getParameter("remove") != null) {
                this.repository.removeResource(bundle);
                resp.sendRedirect(this.getParent(req.getRequestURI()));
            } else {
                this.spoolBundle(resp, resource);
            }
            return;
        } catch (IOException ioe) {
            // TODO: log - might be "unknown" bundle, just list the bundles
        }

        // list the known bundles and present the form
        this.listBundles(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // check whether dumping the repository is requested
        if (req.getRequestURI().endsWith("/repository.zip")) {
            this.dumpRepository(req.getParameterValues("bundle"), resp);
            resp.sendRedirect(req.getRequestURI());
            return;
        }

        if (!ServletFileUpload.isMultipartContent(new ServletRequestContext(req))) {
            resp.sendRedirect(req.getRequestURI());
            return;
        }

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(256000);

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(-1);

        // Parse the request
        boolean noRedirect = false;
        String bundleLocation = null;
        InputStream bundleStream = null;
        try {
            List /* FileItem */items = upload.parseRequest(req);

            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();

                if (!item.isFormField()) {
                    bundleStream = item.getInputStream();
                    bundleLocation = item.getName();
                } else {
                    noRedirect |= "_noredir_".equals(item.getFieldName());
                }
            }

            if (bundleStream != null && bundleLocation != null) {
                try {
                    this.repository.addResource(bundleStream);
                } catch (IOException ioe) {
                    resp.sendError(500, "Cannot register file "
                        + bundleLocation + ". Reason: " + ioe.getMessage());
                }

            }
        } catch (FileUploadException fue) {
            throw new ServletException(fue.getMessage(), fue);
        } finally {
            if (bundleStream != null) {
                try {
                    bundleStream.close();
                } catch (IOException ignore) {
                }
            }
        }

        // only redirect if not instructed otherwise
        if (noRedirect) {
            resp.setContentType("text/plain");
            if (bundleLocation != null) {
                resp.getWriter().print("Bundle " + bundleLocation + " uploaded");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().print("No Bundle uploaded");
            }
        } else {
            resp.sendRedirect(req.getRequestURI());
        }
    }

    @Override
    public String getServletInfo() {
        return "OSGi Bundle Repository (OBR) Servlet";
    }

    private void listBundles(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String category = this.getParameter(req, "category", Repository.CATEGORY_ALL_BUNDLES);

        String regexp = this.getParameter(req, "regexp", "");
        Pattern pattern = null;
        if (regexp.length() > 0) {
            try {
                String regexpInt = regexp;

                // turn regexp into substring match if not bounded by ^/$
                if (!regexpInt.startsWith("^") && !regexpInt.startsWith(".*"))
                    regexpInt = ".*" + regexpInt;
                if (!regexpInt.endsWith("$") && !regexpInt.endsWith(".*"))
                    regexpInt = regexpInt + ".*";

                pattern = Pattern.compile(regexpInt);
            } catch (PatternSyntaxException pse) {
                // don't care, but clear the variable
                regexp = "";
            }
        }

        // set cookies
        this.setCookie(resp, "category", category);
        this.setCookie(resp, "regexp", regexp);

        PrintWriter pw = this.head(resp);

        pw.print("<h1>Welcome to the OSGi Bundle Repository ");
        pw.print(this.repository.getName());
        pw.println("</h1>");

        pw.print("<p>This repository was last updated: ");
        pw.println(new Date(this.repository.getLastModified()));


        pw.println("<hr />");
        pw.println("<h2>Tasks</h2>");

        pw.println("<table border='0' width='100%'>");

        pw.print("<tr><td nowrap>Repository Descriptor</td>");
        pw.print("<td width='99%'><a href='");
        pw.print(this.getRelativeURI(req, "repository.xml"));
        pw.print("'>");
        pw.print("repository.xml");
        pw.println("</a></td></tr>");

        pw.print("<tr><td nowrap>Repository Download (complete)</td>");
        pw.print("<td width='99%'><a href='");
        pw.print(this.getRelativeURI(req, "repository.zip"));
        pw.print("'>");
        pw.print("repository.zip");
        pw.println("</a></td><tr>");

        pw.print("<tr><td nowrap>Bundle Upload</td>");
        pw.print("<td width='99%'><form method='POST' enctype='multipart/form-data'>");
        pw.print("<input type='file' name='bundlefile'>");
        pw.print("<input type='submit' value='Upload Bundle'>");
        pw.print("</form>");
        pw.println("</td></tr>");

        pw.println("</table>");

        pw.println("<hr />");

        pw.println("<form name='selection'>");
        pw.println("<table width='100%' border='0' cellspacing='0' cellpadding='0'>");
        pw.println("<tr><td><h2>Available Resources</h2></td>");
        pw.println("<td align='right'>");
        pw.println("By Name (Reg Exp): <input type='text' name='regexp' value='"
            + regexp + "'>");
        pw.println("&nbsp;&nbsp;|&nbsp;&nbsp;");
        pw.println("By Category: <select name='category' onChange='form.submit();'>");
        Iterator categories = this.repository.getBundleCategories(null).iterator();
        while (categories.hasNext()) {
            Object cat = categories.next();
            String selected = category.equals(cat) ? "selected" : "";
            pw.print("<option value='" + cat + "' " + selected + ">");
            pw.print(cat);
            pw.println("</option>");
        }
        pw.println("</select>");
        pw.println("&nbsp;&nbsp;|&nbsp;&nbsp;");
        pw.println("<input type='submit' value='Show'/>");
        pw.println("&nbsp;&nbsp;|&nbsp;&nbsp;");
        pw.println("<input type='button' value='Download Selected' onClick='document.forms[\"selectiveDump\"].submit();'");
        pw.println("</td></tr>");
        pw.println("</table>");
        pw.println("</form>");


        pw.print("<form name='selectiveDump' method='post' action='" + this.getRelativeURI(req, "repository.zip") + "'>");

        pw.println("<center>");
        pw.println("<table width='80%' border='1' cellspacing='0' cellpadding='3'>");

        boolean haveBundles = false;
        Iterator bi = this.repository.getResourcesByCategory(category);
        while (bi.hasNext()) {
            Resource res = (Resource) bi.next();

            // ignore the resource if the name does not match the regexp
            if (pattern != null
                && !pattern.matcher(res.getSymbolicName()).matches()) {
                continue;
            }

            String uri = this.getRelativeURI(req, res.getResourceName());
            this.dumpRow(pw, "<input name='bundle' type='checkbox' value='"
                + res.getResourceName() + "'></td><td><a href='" + uri
                + "?info=info" + "'>" + res + "</a>", "<a href='" + uri
                + "?remove=remove" + "'>remove</a>");

            haveBundles = true;
        }

        // indicate no matching bundles
        if (!haveBundles) {
            StringBuffer msg = new StringBuffer();
            msg.append("No Bundle found ");
            if (pattern != null) {
                msg.append("whose Symbolic Name matches regular expression '");
                msg.append(pattern.pattern());
                msg.append("' and ");
            }
            msg.append("which has Category " + category);
            this.dumpRow(pw, msg.toString(), null);
        }

        pw.println("</table>");
        pw.println("</center>");

        pw.println("</form>");

        this.foot(pw);
    }

    private void printRepositoryXML(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        // resource URI prefix
        String uriPrefix = "";
        if (req.getParameter("relative") == null) {
            uriPrefix = this.getParent(req.getRequestURL().toString()) + "/";
        }

        resp.setContentType("text/xml; charset=UTF-8");
        this.printRepositoryXML(resp.getWriter(), uriPrefix, null);
    }

    private void dumpRepository(String[] bundleNames, HttpServletResponse resp) throws IOException {

        Set selectedBundles = this.getSelectedBundles(bundleNames);

        resp.setContentType("application/zip");
        ZipOutputStream jos = null;
        try {
            jos = new ZipOutputStream(resp.getOutputStream());

            // spool the repository.xml
            ZipEntry entry = new ZipEntry("repository.xml");
            jos.putNextEntry(entry);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(jos, "UTF-8"));
            this.printRepositoryXML(pw, "", selectedBundles);
            pw.flush();
            jos.closeEntry();

            for (Iterator ri = this.repository.getResourcesById(); ri.hasNext();) {
                Resource res = (Resource) ri.next();

                // spool the resource out if selected or global dump
                String resourceName = res.getResourceName();
                if (selectedBundles == null
                        || selectedBundles.contains(resourceName)) {
                    entry = new ZipEntry(resourceName);
                    jos.putNextEntry(entry);
                    res.spool(jos);
                    jos.closeEntry();
                }
            }

        } finally {
            IOUtils.closeQuietly(jos);
        }
    }

    private void printRepositoryXML(PrintWriter pw, String uriPrefix, Set selectedBundles) {

        // <repository lastmodified="20060817025624.330" name="Untitled">
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pw.print  ("<repository lastmodified=\"");
        pw.print  (this.repository.getLastModifiedFormatted());
        pw.print  ("\" name=\"");
        pw.print  (this.repository.getName());
        pw.println("\">");

        for (Iterator ri = this.repository.getResourcesById(); ri.hasNext();) {
            Resource res = (Resource) ri.next();
            String resourceName = res.getResourceName();
            if (selectedBundles == null
                    || selectedBundles.contains(resourceName)) {
                res.setUri(uriPrefix + resourceName);
                res.serialize(pw, "  ");
            }
        }

        // </repository>
        pw.println("</repository>");
    }

    private void dumpInfo(HttpServletResponse resp, String uri, Resource res) throws IOException {
        PrintWriter pw = this.head(resp);

        /**
         * Name Apache Commons Codec Identity org.apache.commons.codec -
         * 1.3.0.N20060628-1351 Download Download Apache Commons Codec
         * Repository Eclipse Description Copyright Source URL Size 42026
         * Categories Capabilities bundle; manifestversion=2;
         * presentationname=Apache Commons Codec;
         * symbolicname=org.apache.commons.codec; version=1.3.0.N20060628-1351
         * Export Package org.apache.commons.codec; version=0.0.0 Export Package
         * org.apache.commons.codec.binary; version=0.0.0 Export Package
         * org.apache.commons.codec.digest; version=0.0.0 Export Package
         * org.apache.commons.codec.language; version=0.0.0 Export Package
         * org.apache.commons.codec.net; version=0.0.0 Export Package
         * org.apache.commons.codec; version=0.0.0 Export Package
         * org.apache.commons.codec.binary; version=0.0.0 Export Package
         * org.apache.commons.codec.digest; version=0.0.0 Export Package
         * org.apache.commons.codec.language; version=0.0.0 Export Package
         * org.apache.commons.codec.net; version=0.0.0 Requirements Require
         * Bundle org.eclipse.core.runtime; 0.0.0 Require Bundle
         * org.eclipse.core.resources; 0.0.0 Require Bundle
         * org.eclipse.gmf.codegen; 0.0.0 Require Bundle org.eclipse.emf.edit;
         * 0.0.0 Require Bundle org.eclipse.emf.ecore.xmi; 0.0.0 Require Bundle
         * org.eclipse.emf.edit.ui; 0.0.0 Require Bundle org.eclipse.ui.ide;
         * 0.0.0 Require Bundle org.eclipse.emf.codegen.ecore.ui; 0.0.0
         */

        String ident = res.getSymbolicName() + " - " + res.getVersion();

        pw.println("<h1>Resource " + ident + "</h1>");
        pw.println("<center>");
        pw.println("<table width='80%' border='1' cellspacing='0' cellpadding='3'>");
        this.dumpRow(pw, "Name", res.getPresentationName());
        this.dumpRow(pw, "Identity", ident);
        this.dumpRow(pw, "Download", "<a href='"+uri+"'>"+res.getResourceName()+"</a>");
        this.dumpRow(pw, "Repository", "<a href='"+this.getParent(uri)+"'>"+this.repository.getName()+"</a>");

        this.dumpRow(pw, "Description", res.getDescription());
        this.dumpRow(pw, "Copyright", res.getCopyright());
        this.dumpRow(pw, "Documentation", res.getDocumentation());
        this.dumpRow(pw, "Source URL", res.getSource());
        this.dumpRow(pw, "Size", String.valueOf(res.getSize()));
        this.dumpRow(pw, "Categories", String.valueOf(res.getCategories()));

        String label = "Capabilities";
        for (Iterator ci=res.getCapabilities(); ci.hasNext(); ) {
            Object capObj = ci.next();
            if (capObj instanceof PackageCapability) {
                PackageCapability cap = (PackageCapability) capObj;
                StringBuffer buf = new StringBuffer("Export package ");
                buf.append(cap.getPackageName());
                buf.append("; version=");
                buf.append(cap.getVersion());
                this.dumpRow(pw, label, buf.toString());
            } else if (capObj instanceof BundleCapability) {
                BundleCapability cap = (BundleCapability) capObj;
                StringBuffer buf = new StringBuffer(cap.getName());
                buf.append("; manifestversion=").append(cap.getManifestVersion());
                buf.append("; presentationname=").append(cap.getPresentationName());
                buf.append("; symbolicname=").append(cap.getSymbolicName());
                buf.append("; version=").append(cap.getVersion());
                this.dumpRow(pw, label, buf.toString());
            }
            label = null;
        }

        label = "Requirements";
        for (Iterator ci=res.getRequirements(); ci.hasNext(); ) {
            Object reqObj = ci.next();
            if (reqObj instanceof PackageRequirement) {
                PackageRequirement req = (PackageRequirement) reqObj;
                StringBuffer buf = new StringBuffer("Import package ");
                buf.append(req.getPackageName());
                if (req.isOptional()) {
                    buf.append("; resolution:=optional");
                }
                buf.append("; version=").append(req.getVersionRange());
                this.dumpRow(pw, label, buf.toString());
            }
            label = null;
        }

        label = "Bundle Ref";
        BundleSpec[] specs = res.getBundleSpecs();
        for (int i=0; specs != null && i < specs.length; i++) {
            BundleSpec spec = specs[i];
            StringBuffer buf = new StringBuffer(spec.getSymbolicName());
            buf.append("; version=").append(spec.getVersion());
            buf.append("; startlevel=").append(spec.getStartLevel());
            if (spec.getEntry() != null) {
                buf.append("; embeddedEntry=").append(spec.getEntry());
            }
            this.dumpRow(pw, label, buf.toString());

            label = null;
        }

        pw.println("</table>");
        pw.println("</center>");

        this.foot(pw);
    }

    private void dumpRow(PrintWriter pw, String name, String value) {
        pw.print("<tr><td>");
        pw.print((name == null) ? "&nbsp;" : name);
        pw.print("</td><td>");
        pw.print((value == null) ? "&nbsp;" : value);
        pw.println("</td></tr>");
    }

    private void spoolBundle(HttpServletResponse res, Resource resource)
            throws IOException {
        res.setContentType("application/x-jar");
        resource.spool(res.getOutputStream());
    }

    private String getRelativeURI(HttpServletRequest req, String relPath) {
        StringBuffer url = req.getRequestURL();
        if (url.charAt(url.length() - 1) != '/') url.append('/');
        url.append(relPath);
        return url.toString();
    }

    private String getParent(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }

        return path;
    }

    private String getName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }

        return path;
    }

    private PrintWriter head(HttpServletResponse res) throws IOException {
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        pw.println("<html><head><title>Bundle List</title></head>");
        pw.println("<body bgcolor='white'>");
        return pw;
    }

    private void foot(PrintWriter pw) {
        pw.println("</body></html>");
    }

    private String getParameter(HttpServletRequest req, String name, String defaultValue) {
        // check parameter first
        String value = req.getParameter(name);

        // none, so check cookie
        if (value == null) {
            Cookie[] cookies = req.getCookies();
            for (int i=0; cookies != null && i < cookies.length; i++) {
                if (name.equals(cookies[i].getName())) {
                    // make the value URL safe
                    try {
                        value = URLDecoder.decode(cookies[i].getValue(), "UTF-8");
                    } catch (UnsupportedEncodingException uee) {
                        // not really !!
                    }

                    break;
                }
            }
        }

            // fall back to default
        if (value != null) {
            return value;
        }

        // fall back to default
        return defaultValue;
    }

    private void setCookie(HttpServletResponse res, String name, String value) {
        // make the value URL safe
        try {
            value = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            // not really !!
        }

        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(-1);
        res.addCookie(cookie);
    }

    // ---------- Logging ------------------------------------------------------

    private void debug(String message) {
        this.log(LogService.LOG_DEBUG, message);
    }

    private void info(String message) {
        this.log(LogService.LOG_INFO, message);
    }

    private void warn(String message) {
        this.log(LogService.LOG_WARNING, message);
    }

    private void error(String message) {
        this.log(LogService.LOG_ERROR, message);
    }

    private void log(int level, String message) {
        if (this.log != null) {
            this.log.log(this.ref, level, message);
        }
    }

    //---------- internal -----------------------------------------------------

    private Set getSelectedBundles(String[] bundleNames) throws IOException {

        // no selection, export everything
        if (bundleNames == null || bundleNames.length == 0) {
            return null;
        }

        // need the repository.xml file
        File repoFile = null;
        OutputStream out = null;
        try {

            // dump complete repository XML to be able to resolve
            repoFile = File.createTempFile("repository.", ".xml");
            out = new FileOutputStream(repoFile);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            this.printRepositoryXML(pw, "", null);
            pw.close();

            // prepare the repository resolver
            DummyBundleContext bc = new DummyBundleContext(this.ctx.getBundleContext());
            bc.setProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP,
                repoFile.toURI().toURL().toExternalForm());
            RepositoryAdmin ra = new RepositoryAdminImpl(bc);
            Set resources = new HashSet();

            // add the servlet container bundles first
            for (int i = 0; i < REQUIRED_BUNDLES.length; i++) {
                String filter = "(symbolicName=" + REQUIRED_BUNDLES[i]
                    + ")";
                this.resolveResource(ra, filter, resources);
            }

            // find all resources in the repository
            for (int i=0; i < bundleNames.length; i++) {
                Resource res = this.repository.getResource(bundleNames[i]);
                if (res != null) {
                    VersionRange vr = new VersionRange(res.getVersion());
                    String filter = "(&(symbolicName=" + res.getSymbolicName()
                        + ")" + vr.getFilter() + ")";
                    this.resolveResource(ra, filter, resources);
                }
            }

            // prepare the resolver
            Resolver resolver = ra.resolver();
            for (Iterator ri=resources.iterator(); ri.hasNext(); ) {
                resolver.add((org.osgi.service.obr.Resource) ri.next());
            }

            // resolve the resources
            if (!resolver.resolve()) {
                // TODO: dump why
            }

            HashSet bundles = new HashSet();
            org.osgi.service.obr.Resource[] list = resolver.getAddedResources();
            for (int i=0; list != null && i < list.length; i++) {
                bundles.add(this.getName(list[i].getURL().getPath()));
            }

            list = resolver.getRequiredResources();
            for (int i=0; list != null && i < list.length; i++) {
                bundles.add(this.getName(list[i].getURL().getPath()));
            }

            return bundles;

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }

            // only in case of an error !!
            if (repoFile != null) {
                repoFile.delete();
            }
        }
    }

    private void resolveResource(RepositoryAdmin ra, String filter,
            Set resources) {
        org.osgi.service.obr.Resource[] list = ra.discoverResources(filter);
        for (int j = 0; list != null && j < list.length; j++) {
            if (resources.add(list[j])) {
                String name = this.getName(list[j].getURL().getPath());
                Resource res = this.repository.getResource(name);
                if (res != null) {
                    BundleSpec[] specs = res.getBundleSpecs();
                    for (int s = 0; specs != null && s < specs.length; s++) {
                        this.resolveResource(ra, specs[s].toFilter(), resources);
                    }
                }
            }
        }
    }

    // ---------- SCR Management Support ---------------------------------------

    protected void activate(ComponentContext context) {
        this.ctx = context;
        this.ref = context.getServiceReference();

        // get the web manager root path
        Object wmr = context.getProperties().get("manager.root");
        this.webManagerRoot = (wmr instanceof String) ? (String) wmr : null;
        if (this.webManagerRoot == null) {
            this.webManagerRoot = "/";
        } else if (!this.webManagerRoot.startsWith("/")) {
            this.webManagerRoot = "/" + this.webManagerRoot;
        }

        // register the servlet and resources
        try {
            HttpContext httpContext = this.httpService.createDefaultHttpContext();
            this.httpService.registerServlet(this.webManagerRoot, this,
                context.getProperties(), httpContext);
        } catch (Exception e) {
            // TODO: handle
        }

        this.error("Service Active");
    }

    protected void deactivate(ComponentContext context) {
        this.httpService.unregister(this.webManagerRoot);
    }

    protected void bindLogService(LogService log) {
        this.log = log;
    }

    protected void unbindLogService(LogService log) {
        this.log = null;
    }

    protected void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }
}
