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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.lib.osgi.Analyzer;

/**
 * The <code>Resource</code> TODO
 *
 * @author fmeschbe
 * @version $Rev$, $Date: 2007-07-02 16:13:58 +0200 (Mon, 02 Jul 2007) $
 */
public class Resource implements Serializable, Comparable<Resource> {

    /**
     * The name of the bundle manifest header providing the specification(s) of
     * the bundle(s) to be installed along with this Assembly Bundle (value is
     * "Assembly-Bundles").
     */
    public static final String ASSEMBLY_BUNDLES = "Assembly-Bundles";

    private URL resourceURL;
    private String name;

    private String id;
    private String presentationName;
    private String symbolicName;
    private String uri;
    private String version;
    private String description;
    private long size;
    private String documentation;
    private String copyright;
    private String vendor;
    private String contact;
    private String license;
    private String source;
    private SortedSet<String> categories;

    private SortedMap<String, List<Object>> capabilities;
    private SortedMap<String, List<Object>> requirements;

    private BundleSpec[] bundleSpecs;

    public static Resource create(File file) throws IOException {
        return create(file.toURL());
    }

    public static Resource create(URL file) throws IOException {
        JarInputStream jar = null;
        try {
            URLConnection conn = file.openConnection();
            jar = new JarInputStream(conn.getInputStream());
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                throw new IOException(file + " is not a valid JAR file: Manifest not first entry");
            }
            return new Resource(file, manifest.getMainAttributes(),
                conn.getContentLength());
        } finally {
            IOUtils.closeQuietly(jar);
        }
    }

    private Resource(URL resourceURL, Attributes attrs, long size)
            throws IOException {

        this.capabilities = new TreeMap<String, List<Object>>();
        this.requirements = new TreeMap<String, List<Object>>();

        this.resourceURL = resourceURL;
        this.name = this.getName(resourceURL.getPath());

        this.presentationName = attrs.getValue(Constants.BUNDLE_NAME);
        this.symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
        this.uri = attrs.getValue(Constants.BUNDLE_UPDATELOCATION);
        this.version = attrs.getValue(Constants.BUNDLE_VERSION);
        this.description = attrs.getValue(Constants.BUNDLE_DESCRIPTION);
        this.size = size;
        this.documentation = attrs.getValue(Constants.BUNDLE_DOCURL);
        this.copyright = attrs.getValue(Constants.BUNDLE_COPYRIGHT);
        this.vendor = attrs.getValue(Constants.BUNDLE_VENDOR);
        this.contact = attrs.getValue(Constants.BUNDLE_CONTACTADDRESS);
        this.license = attrs.getValue("Bundle-LicenseURL");
        this.source = attrs.getValue("Bundle-SourceURL");
        String categoryString = attrs.getValue(Constants.BUNDLE_CATEGORY);

        // require symbolicName
        if (this.symbolicName == null || this.symbolicName.length() == 0) {
            throw new IOException("Missing Bundle-SymbolicName, not a valid Bundle");
        }

        // default version valu
        if (this.version == null || this.version.length() == 0) {
            this.version = "0.0.0";
        }

        // default value for the id
        this.id = this.symbolicName + ":" + this.version;

        this.categories = new TreeSet<String>();
        if (categoryString != null) {
            String[] catList = categoryString.split(",");
            for (int i=0; catList != null && i < catList.length; i++) {
                String cat = catList[i].trim();
                if (cat.length() > 0) {
                    this.categories.add(cat);
                }
            }
        }

        this.addCapability(new BundleCapability(attrs));

        this.addExportedServices(attrs.getValue(Constants.EXPORT_SERVICE));
        this.addExportedPackages(attrs.getValue(Constants.EXPORT_PACKAGE));
        this.addImportedServices(attrs.getValue(Constants.IMPORT_SERVICE));
        this.addImportedPackages(attrs.getValue(Constants.IMPORT_PACKAGE));

        // parse bundle specifications of Assembly Bundle
        this.addBundleSpecs(attrs.getValue(ASSEMBLY_BUNDLES));
    }

    void addCapability(Capability capability) {
        this.addToMapList(this.capabilities, capability.getName(), capability);
    }

    public Iterator<Object> getCapabilities() {
        return this.getMapListIterator(this.capabilities);
    }

    void addRequirement(Requirement requirement) {
        this.addToMapList(this.requirements, requirement.getName(), requirement);
    }

    public Iterator<Object> getRequirements() {
        return this.getMapListIterator(this.requirements);
    }

    public SortedSet<String> getCategories() {
        return this.categories;
    }

    public BundleSpec[] getBundleSpecs() {
        return this.bundleSpecs;
    }

    /**
     * @return the contact
     */
    public String getContact() {
        return this.contact;
    }

    /**
     * @return the copyright
     */
    public String getCopyright() {
        return this.copyright;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the documentation
     */
    public String getDocumentation() {
        return this.documentation;
    }

    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the id
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the license
     */
    public String getLicense() {
        return this.license;
    }

    /**
     * @return the presentationName
     */
    public String getPresentationName() {
        return this.presentationName;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return this.size;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return this.source;
    }

    /**
     * @return the symbolicName
     */
    public String getSymbolicName() {
        return this.symbolicName;
    }

    /**
     * @return the uri
     */
    public String getPath() {
        return this.uri;
    }

    /**
     * Sets the uri
     * @param uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return this.vendor;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * @return the URL location of the bundle file
     */
    public URL getResourceURL() {
        return this.resourceURL;
    }

    /**
     * @return the name part of the bundle file URL location
     */
    public String getResourceName() {
     return this.name;
    }

    /**
     * Returns an <code>InputStream</code> on the resource bundle file
     * @throws IOException If an error occurrs opening the stream.
     */
    public void spool(OutputStream out) throws IOException {
        URLConnection conn = this.getResourceURL().openConnection();
        InputStream ins = null;
        try {
            ins = conn.getInputStream();
            Repository.spool(ins, out);
        } finally {
            IOUtils.closeQuietly(ins);
        }
    }

    //---------- Serializable interface ---------------------------------------

    public void serialize(PrintWriter out, String indent) {
        out.print(indent);
        out.print("<resource");

        this.printAttribute(out, "id", this.id);
        this.printAttribute(out, "presentationname", this.presentationName);
        this.printAttribute(out, "symbolicname", this.symbolicName);
        this.printAttribute(out, "uri", this.uri);
        this.printAttribute(out, "version", this.version);
        out.println('>');

        String childIndent = indent + " ";
        this.printElement(out, childIndent, "description", this.description);
        this.printElement(out, childIndent, "size", String.valueOf(this.size));
        this.printElement(out, childIndent, "documentation", this.documentation);
        this.printElement(out, childIndent, "copyright", this.copyright);
        this.printElement(out, childIndent, "vendor", this.vendor);
        this.printElement(out, childIndent, "contact", this.contact);
        this.printElement(out, childIndent, "license", this.license);
        this.printElement(out, childIndent, "source", this.source);

        for (String cat : this.getCategories() ) {
            out.print("<category");
            this.printAttribute(out, "id", cat);
            out.println("/>");
        }

        this.printMap(out, childIndent, this.capabilities);
        this.printMap(out, childIndent, this.requirements);

        out.print(indent);
        out.println("</resource>");
    }

    //---------- Comparable interface -----------------------------------------

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Resource other) {
        if (this.equals(other)) {
            return 0;
        }

        if (this.getSymbolicName().equals(other.getSymbolicName())) {
            // compare version
            Version thisVersion = new Version(this.getVersion());
            Version otherVersion = new Version(other.getVersion());
            return thisVersion.compareTo(otherVersion);
        }

        return this.getSymbolicName().compareTo(other.getSymbolicName());
    }

    //---------- Object overwrite ---------------------------------------------

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Resource) {
            Resource other = (Resource) obj;
            return this.getSymbolicName().equals(other.getSymbolicName())
                && this.getVersion().equals(other.getVersion());
        }

        return false;
    }

    public int hashCode() {
        return this.getSymbolicName().hashCode() * 33 + this.getVersion().hashCode() * 17;
    }

    public String toString() {
        return this.getSymbolicName() + " - " + this.getVersion();
    }

    //---------- internal -----------------------------------------------------

    // helper
    private String getName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash+1);
        }
        return path;
    }

    private void addExportedServices(String exportService) {
        if (exportService == null || exportService.length() == 0) {
            return;
        }
    }

    private void addExportedPackages(String exportPackage) {
        if (exportPackage == null || exportPackage.length() == 0) {
            return;
        }

//        R4Package[] pkgs = ManifestParser.parseImportExportHeader(exportPackage);
//        for (int i=0; i < pkgs.length; i++) {
//            // <capability name="package">
//            // <p n="package" v="org.apache.felix.upnp.extra.controller"/>
//            // <p n="version" t="version" v="1.0.0"/>
//            // </capability>
//            addCapability(new PackageCapability(new R4Export(pkgs[i])));
//        }
        Map pkgs = Analyzer.parseHeader(exportPackage, null);
        for (Iterator pi=pkgs.entrySet().iterator(); pi.hasNext(); ) {
            // <capability name="package">
            // <p n="package" v="org.apache.felix.upnp.extra.controller"/>
            // <p n="version" t="version" v="1.0.0"/>
            // </capability>
            this.addCapability(new PackageCapability((Map.Entry) pi.next()));
        }
    }

    private void addImportedServices(String importService) {
        if (importService == null || importService.length() == 0) {
            return;
        }
    }

    private void addImportedPackages(String importPackage) {
        if (importPackage == null || importPackage.length() == 0) {
            return;
        }

//        R4Package[] pkgs = ManifestParser.parseImportExportHeader(importPackage);
//        for (int i=0; i < pkgs.length; i++) {
//            // <require extend="false"
//            //    filter="(&(package=org.xml.sax)(version>=0.0.0))"
//            //    multiple="false"
//            //    name="package"
//            //    optional="false">
//            //  Import package org.xml.sax
//            // </require>
//            addRequirement(new PackageRequirement(new R4Import(pkgs[i])));
//        }
        Map pkgs = Analyzer.parseHeader(importPackage, null);
        for (Iterator pi=pkgs.entrySet().iterator(); pi.hasNext(); ) {
            // <require extend="false"
            //    filter="(&(package=org.xml.sax)(version>=0.0.0))"
            //    multiple="false"
            //    name="package"
            //    optional="false">
            //  Import package org.xml.sax
            // </require>
            this.addRequirement(new PackageRequirement((Map.Entry) pi.next()));
        }
    }

    private void addToMapList(Map<String, List<Object>> map, String name, Object value) {
        List<Object> current = map.get(name);
        if (current == null) {
            current = new ArrayList<Object>();
            map.put(name, current);
        }
        current.add(value);
    }

    private void addBundleSpecs(String spec) {
        if (spec == null) {
            // this is not expected ...
            this.bundleSpecs = new BundleSpec[0];
        } else {
            spec = spec.trim();
            List<BundleSpec> specs = new ArrayList<BundleSpec>();
            boolean quoted = false;
            int start = 0;
            for (int i = 0; i < spec.length(); i++) {
                char c = spec.charAt(i);
                if (quoted) {
                    if (c == '\\') {
                        // escape skip to next
                        i++;
                    } else if (c == '"') {
                        quoted = false;
                    }
                } else {
                    if (c == '"') {
                        // start quoting
                        quoted = true;
                    } else if (c == ',') {
                        // spec separation
                        specs.add(new BundleSpec(spec.substring(start, i)));
                        start = i + 1;
                    }
                }
            }
            if (start < spec.length()) {
                specs.add(new BundleSpec(spec.substring(start)));
            }
            this.bundleSpecs = specs.toArray(new BundleSpec[specs.size()]);
        }
    }

    private Iterator<Object> getMapListIterator(final Map<String, List<Object>> map) {
        return new Iterator<Object>() {
            private Iterator<List<Object>> mapIter;
            private Iterator<Object> listIter;
            private Object next;

            {
                this.mapIter = map.values().iterator();
                this.next = this.seek();
            }

            public boolean hasNext() {
                return this.next != null;
            }

            public Object next() {
                if (this.next == null) {
                    throw new NoSuchElementException("next");
                }

                Object toReturn = this.next;
                this.next = this.seek();
                return toReturn;
            }

            private Object seek() {
                while (this.listIter == null || !this.listIter.hasNext()) {
                    if (this.mapIter.hasNext()) {
                        List<Object> nextlist = this.mapIter.next();
                        this.listIter = nextlist.iterator();
                    } else {
                        return null;
                    }
                }

                return this.listIter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    private void printAttribute(PrintWriter out, String name, String value) {
        if (value != null) {
            out.print(' ');
            out.print(name);
            out.print("=\"");
            out.print(value);
            out.print('"');
        }
    }

    private void printElement(PrintWriter out, String indent, String name, String value) {
        if (value != null) {
            out.print(indent);
            out.print('<');
            out.print(name);
            out.print('>');
            out.print(value);
            out.print("</");
            out.print(name);
            out.println('>');
        }
    }

    private void printMap(PrintWriter out, String indent, Map map) {
        for (Iterator mi=this.getMapListIterator(map); mi.hasNext(); ) {
            Serializable ser = (Serializable) mi.next();
            ser.serialize(out, indent);
        }
    }
}
