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
package org.apache.sling.maven.jspc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.xmlparser.TreeNode;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * The <code>JspcMojo</code> is implements the Sling Maven JspC goal
 * <code>jspc</code> compiling JSP into the target and creating a component
 * descriptor for Declarative Services to use the JSP with the help of the
 * appropriate adapter as component.
 *  
 * @goal jspc
 * @phase compile
 * @description Compile JSP Files into Servlet Classes using the same JSP
 *              Compiler as is used at runtime to compile Repository based JSP
 *              into classes.
 * @requiresDependencyResolution compile
 */
public class JspcMojo extends AbstractMojo implements Options {

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of JSP source files.
     *
     * @parameter expression="${jspc.sourceDirectory}"
     *            default-value="${project.build.scriptSourceDirectory}"
     */
    private String sourceDirectory;

    /**
     * Target folder for the compiled classes.
     *
     * @parameter expression="${jspc.outputDirectory}"
     *            default-value="${project.build.directory}/jspc-plugin-generated"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter expression="${jspc.jasper.classdebuginfo}"
     *            default-value="true"
     */
    private boolean jasperClassDebugInfo;

    /**
     * @parameter expression="${jspc.jasper.enablePooling}" default-value="true"
     */
    private boolean jasperEnablePooling;

    /**
     * @parameter expression="${jspc.jasper.ieClassId}"
     *            default-value="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
     */
    private String jasperIeClassId;

    /**
     * @parameter expression="${jspc.jasper.genStringAsCharArray}"
     *            default-value="false"
     */
    private boolean jasperGenStringAsCharArray;

    /**
     * @parameter expression="${jspc.jasper.keepgenerated}" default-value="true"
     */
    private boolean jasperKeepGenerated;

    /**
     * @parameter expression="${jspc.jasper.mappedfile}" default-value="true"
     */
    private boolean jasperMappedFile;

    /**
     * @parameter expression="${jspc.jasper.trimSpaces}" default-value="false"
     */
    private boolean jasperTrimSpaces;

    /**
     * @parameter expression="${jspc.failOnError}" default-value="false"
     */
    private boolean failOnError;

    /**
     * @parameter expression="${jspc.showSuccess}" default-value="false"
     */
    private boolean showSuccess;

    /**
     * @parameter expression="${jspc.compilerTargetVM}" default-value="1.4"
     */
    private String compilerTargetVM;

    /**
     * @parameter expression="${jspc.compilerSourceVM}" default-value="1.4"
     */
    private String compilerSourceVM;

    /**
     * Comma separated list of extensions of files to be compiled by the plugin.
     *
     * @parameter expression="${jspc.jspFileExtensions}"
     *            default-value="jsp,jspx"
     */
    private String jspFileExtensions;

    /**
     * @parameter expression="${jspc.servletPackage}"
     *            default-value="org.apache.jsp"
     */
    private String servletPackage;

    private Set<String> jspFileExtensionSet;

    private boolean compile = true;

    private String uriSourceRoot;

    private List<String> pages = new ArrayList<String>();

    private ServletContext context;

    private JspRuntimeContext rctxt;

    private URLClassLoader loader = null;

    private Map<String, TreeNode> tldCache;

    /**
     * Cache for the TLD locations
     */
    private TldLocationsCache tldLocationsCache = null;

    private JspConfig jspConfig = null;

    private TagPluginManager tagPluginManager = null;

    /*
     * (non-Javadoc)
     *
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException {

        try {
            uriSourceRoot = new File(sourceDirectory).getCanonicalPath();
        } catch (Exception e) {
            uriSourceRoot = new File(sourceDirectory).getAbsolutePath();
        }

        // scan all JSP file
        // scanFiles(new File(sourceDirectory));

        // have the files compiled
        try {
            executeInternal();
        } catch (JasperException je) {
            getLog().error("Compilation Failure", je);
            throw new MojoExecutionException(je.getMessage(), je);
        }

        project.addCompileSourceRoot(outputDirectory);
    }

    /**
     * Locate all jsp files in the webapp. Used if no explicit jsps are
     * specified.
     */
    public void scanFiles(File base) {
        Stack<File> dirs = new Stack<File>();
        dirs.push(base);

        while (!dirs.isEmpty()) {
            File f = dirs.pop();
            if (f.exists() && f.isDirectory()) {
                String[] files = f.list();
                String ext;
                for (int i = 0; (files != null) && i < files.length; i++) {
                    File f2 = new File(f, files[i]);
                    if (f2.isDirectory()) {
                        dirs.push(f2);
                    } else {
                        ext = files[i].substring(files[i].lastIndexOf('.') + 1);
                        if (getExtensions().contains(ext)) {
                            pages.add(f2.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the compilation.
     *
     * @throws JasperException If an error occurs
     */
    private void executeInternal() throws JasperException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("execute() starting for " + pages.size() + " pages.");
        }

        StringWriter serviceComponentWriter = new StringWriter();

        try {
            if (context == null) {
                initServletContext();
            }

            // No explicit pages, we'll process all .jsp in the webapp
            if (pages.size() == 0) {
                scanFiles(new File(sourceDirectory));
            }

            File uriRootF = new File(uriSourceRoot);
            if (!uriRootF.exists() || !uriRootF.isDirectory()) {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.jspc.uriroot_not_dir"));
            }

            for (String nextjsp : pages) {
                File fjsp = new File(nextjsp);
                if (!fjsp.isAbsolute()) {
                    fjsp = new File(uriRootF, nextjsp);
                }
                if (!fjsp.exists()) {
                    if (getLog().isWarnEnabled()) {
                        getLog().warn(
                            Localizer.getMessage("jspc.error.fileDoesNotExist",
                                fjsp.toString()));
                    }
                    continue;
                }
                String s = fjsp.getAbsolutePath();
                if (s.startsWith(uriSourceRoot)) {
                    nextjsp = s.substring(uriSourceRoot.length());
                }
                if (nextjsp.startsWith("." + File.separatorChar)) {
                    nextjsp = nextjsp.substring(2);
                }

                processFile(nextjsp, serviceComponentWriter);
            }

            printServiceComponents(serviceComponentWriter);

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                rootCause.printStackTrace();
            }
            throw je;

        } catch (/* IO */Exception ioe) {
            throw new JasperException(ioe);
        }
    }

    private void processFile(String file, Writer serviceComponentWriter)
            throws JasperException {
        ClassLoader originalClassLoader = null;

        try {
            String jspUri = file.replace('\\', '/');
            JspCompilationContext clctxt = new JspCompilationContext(jspUri,
                false, this, context, null, rctxt);

            // write to a specific servlet package
            clctxt.setServletPackageName(servletPackage);

            originalClassLoader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                initClassLoader();
            }
            Thread.currentThread().setContextClassLoader(loader);

            // we only use the class loader and do not need the class path
            clctxt.setClassLoader(loader);
            clctxt.setClassPath(null);

            Compiler clc = clctxt.createCompiler();

            // If compile is set, generate both .java and .class, if
            // .jsp file is newer than .class file;
            // Otherwise only generate .java, if .jsp file is newer than
            // the .java file
            if (clc.isOutDated(compile)) {
                clc.compile(compile, true);

                if (showSuccess) {
                    getLog().info("Built File: " + file);
                }
            } else if (showSuccess) {
                getLog().info("File up to date: " + file);
            }

            // write the OSGi component descriptor
            writeJspServiceComponent(serviceComponentWriter,
                clctxt.getServletPackageName() + "."
                    + clctxt.getServletClassName());

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                getLog().error(
                    Localizer.getMessage("jspc.error.generalException", file),
                    rootCause);
            }

            // Bugzilla 35114.
            if (failOnError) {
                throw je;
            }

            // just log otherwise
            getLog().error(je.getMessage());

        } catch (Exception e) {
            if ((e instanceof FileNotFoundException)
                && getLog().isWarnEnabled()) {
                getLog().warn(
                    Localizer.getMessage("jspc.error.fileDoesNotExist",
                        e.getMessage()));
            }
            throw new JasperException(e);
        } finally {
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(
                    originalClassLoader);
            }
        }
    }

    // ---------- Additional Settings ------------------------------------------

    private Set<String> getExtensions() {
        if (jspFileExtensionSet == null) {
            jspFileExtensionSet = new HashSet<String>();

            // fallback default value, should actually be set by Maven
            if (jspFileExtensions == null) {
                jspFileExtensions = "jsp,jspx";
            }

            StringTokenizer st = new StringTokenizer(jspFileExtensions, ",");
            while (st.hasMoreTokens()) {
                String ext = st.nextToken().trim();
                if (ext.length() > 0) {
                    jspFileExtensionSet.add(ext);
                }
            }
        }

        return jspFileExtensionSet;
    }

    private void initServletContext() {
        try {
            context = new JspCServletContext(getLog(), new URL("file:"
                + uriSourceRoot.replace('\\', '/') + '/'));
            tldLocationsCache = new TldLocationsCache(context, true);
        } catch (MalformedURLException me) {
            getLog().error("Cannot setup ServletContext", me);
        }

        rctxt = new JspRuntimeContext(context, this);
        jspConfig = new JspConfig(context);
        tagPluginManager = new TagPluginManager(context);
    }

    /**
     * Initializes the classloader as/if needed for the given compilation
     * context.
     *
     * @param clctxt The compilation context
     * @throws IOException If an error occurs
     */
    private void initClassLoader() throws IOException,
            DependencyResolutionRequiredException {

        // Turn the classPath into URLs
        List<?> classPath = project.getCompileClasspathElements();
        ArrayList<URL> urls = new ArrayList<URL>();
        for (Iterator<?> cpi = classPath.iterator(); cpi.hasNext();) {
            String path = (String) cpi.next();
            urls.add(new File(path).toURI().toURL());
        }

        URL urlsA[] = new URL[urls.size()];
        urls.toArray(urlsA);
        loader = new URLClassLoader(urlsA, getClass().getClassLoader());
    }

    private void writeJspServiceComponent(Writer out, String className) {

        String id = className;
        if (id.startsWith(servletPackage)) {
            // account for trailing dot of the package
            id = id.substring(servletPackage.length() + 1);
        }

        try {
            out.write("<scr:component enabled=\"true\" immediate=\"true\" name=\"");
            out.write(id);
            out.write("\">\r\n");
            
            // the implementation is of course the compiled JSP
            out.write("<scr:implementation class=\"");
            out.write(className);
            out.write("\"/>\r\n");
            
            // the JSP registers as a Servlet
            out.write("<scr:service>\r\n");
            out.write("<scr:provide interface=\"javax.servlet.Servlet\"/>\r\n");
            out.write("</scr:service>\r\n");
            
            // use the JSP's id as the service.pid
            out.write("<scr:property name=\"service.pid\" value=\"");
            out.write(id);
            out.write("\"/>\r\n");

            // if the project defines an organization name, add it
            if (project.getOrganization() != null
                && project.getOrganization().getName() != null) {
                out.write("<scr:property name=\"service.vendor\" value=\"");
                out.write(project.getOrganization().getName());
                out.write("\"/>\r\n");
            }
            
            out.write("</scr:component>\r\n");

            out.flush();
        } catch (IOException ignore) {
            // don't care
        }
    }

    private void printServiceComponents(StringWriter serviceComponentWriter)
            throws IOException {
        FileOutputStream out = null;
        try {

            String target = "OSGI-INF/jspServiceComponents.xml";
            File targetFile = new File(outputDirectory, target);
            targetFile.getParentFile().mkdirs();

            out = new FileOutputStream(targetFile);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
                "UTF-8"));

            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<components xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.0.0\">");

            pw.print(serviceComponentWriter.toString());

            pw.println("</components>");

            pw.flush();
            pw.close();

            // now add the descriptor file to the maven resources
            final String ourRsrcPath = new File(outputDirectory).getAbsolutePath();
            boolean found = false;
            final Iterator<?> rsrcIterator = project.getResources().iterator();
            while (!found && rsrcIterator.hasNext()) {
                final Resource rsrc = (Resource) rsrcIterator.next();
                found = rsrc.getDirectory().equals(ourRsrcPath);
            }
            if (!found) {
                final Resource resource = new Resource();
                resource.setDirectory(new File(outputDirectory).getAbsolutePath());
                project.addResource(resource);
            }

            // and set include accordingly
            String svcComp = project.getProperties().getProperty("Service-Component");
            svcComp= (svcComp == null) ? target : svcComp + ", " + target;
            project.getProperties().setProperty("Service-Component", svcComp);

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                    // don't care
                }
            }
        }
    }

    // ---------- Options interface --------------------------------------------

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#genStringAsCharArray()
     */
    public boolean genStringAsCharArray() {
        return jasperGenStringAsCharArray;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isCaching()
     */
    public boolean isCaching() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCache()
     */
    public Map<String, TreeNode> getCache() {
        if (tldCache == null) {
            tldCache = new HashMap<String, TreeNode>();
        }

        return tldCache;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCheckInterval()
     */
    public int getCheckInterval() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getClassDebugInfo()
     */
    public boolean getClassDebugInfo() {
        return jasperClassDebugInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getClassPath()
     */
    public String getClassPath() {
        // no extra classpath
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompiler()
     */
    public String getCompiler() {
        // use JDTCompiler, which is the default
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerSourceVM()
     */
    public String getCompilerSourceVM() {
        return compilerSourceVM;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getCompilerTargetVM()
     */
    public String getCompilerTargetVM() {
        return compilerTargetVM;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getDevelopment()
     */
    public boolean getDevelopment() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getErrorOnUseBeanInvalidClassAttribute()
     */
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        // not configurable
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getFork()
     */
    public boolean getFork() {
        // certainly don't fork (not required anyway)
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getIeClassId()
     */
    public String getIeClassId() {
        return jasperIeClassId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJavaEncoding()
     */
    public String getJavaEncoding() {
        return "UTF-8";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJspClassLoader()
     */
    public ClassLoader getJspClassLoader() {
        // no JSP ClassLoader, use default
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getJspConfig()
     */
    public JspConfig getJspConfig() {
        return jspConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getKeepGenerated()
     */
    public boolean getKeepGenerated() {
        return jasperKeepGenerated;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getMappedFile()
     */
    public boolean getMappedFile() {
        return jasperMappedFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getModificationTestInterval()
     */
    public int getModificationTestInterval() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getScratchDir()
     */
    public String getScratchDir() {
        return outputDirectory;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getSendErrorToClient()
     */
    public boolean getSendErrorToClient() {
        // certainly output any problems
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTagPluginManager()
     */
    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTldLocationsCache()
     */
    public TldLocationsCache getTldLocationsCache() {
        return tldLocationsCache;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#getTrimSpaces()
     */
    public boolean getTrimSpaces() {
        return jasperTrimSpaces;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isPoolingEnabled()
     */
    public boolean isPoolingEnabled() {
        return jasperEnablePooling;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isSmapDumped()
     */
    public boolean isSmapDumped() {
        // always include the SMAP (optionally, limit to if debugging)
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isSmapSuppressed()
     */
    public boolean isSmapSuppressed() {
        // require SMAP
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.jasper.Options#isXpoweredBy()
     */
    public boolean isXpoweredBy() {
        // no XpoweredBy setting please
        return false;
    }
}
