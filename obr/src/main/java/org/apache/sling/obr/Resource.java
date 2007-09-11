/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.obr;

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
class Resource implements Serializable, Comparable {

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
    private SortedSet categories;
    
    private SortedMap capabilities;
    private SortedMap requirements;
    
    private BundleSpec[] bundleSpecs;
    
    static Resource create(File file) throws IOException {
        return create(file.toURL());
    }
    
    static Resource create(URL file) throws IOException {
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
        
        capabilities = new TreeMap();
        requirements = new TreeMap();

        this.resourceURL = resourceURL;
        this.name = getName(resourceURL.getPath());
        
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
        if (symbolicName == null || symbolicName.length() == 0) {
            throw new IOException("Missing Bundle-SymbolicName, not a valid Bundle");
        }
        
        // default version valu
        if (version == null || version.length() == 0) {
            version = "0.0.0";
        }
        
        // default value for the id
        this.id = symbolicName + ":" + version;

        this.categories = new TreeSet();
        if (categoryString != null) {
            String[] catList = categoryString.split(",");
            for (int i=0; catList != null && i < catList.length; i++) {
                String cat = catList[i].trim();
                if (cat.length() > 0) {
                    this.categories.add(cat);
                }
            }
        }
        
        addCapability(new BundleCapability(attrs));

        addExportedServices(attrs.getValue(Constants.EXPORT_SERVICE));
        addExportedPackages(attrs.getValue(Constants.EXPORT_PACKAGE));
        addImportedServices(attrs.getValue(Constants.IMPORT_SERVICE));
        addImportedPackages(attrs.getValue(Constants.IMPORT_PACKAGE));
        
        // parse bundle specifications of Assembly Bundle
        addBundleSpecs(attrs.getValue(ASSEMBLY_BUNDLES));
    }
    
    void addCapability(Capability capability) {
        addToMapList(capabilities, capability.getName(), capability);
    }

    Iterator getCapabilities() {
        return getMapListIterator(capabilities);
    }
    
    void addRequirement(Requirement requirement) {
        addToMapList(requirements, requirement.getName(), requirement);
    }
    
    Iterator getRequirements() {
        return getMapListIterator(requirements);
    }
    
    SortedSet getCategories() {
        return categories;
    }
    
    BundleSpec[] getBundleSpecs() {
        return bundleSpecs;
    }
    
    /**
     * @return the contact
     */
    String getContact() {
        return contact;
    }

    /**
     * @return the copyright
     */
    String getCopyright() {
        return copyright;
    }

    /**
     * @return the description
     */
    String getDescription() {
        return description;
    }

    /**
     * @return the documentation
     */
    String getDocumentation() {
        return documentation;
    }

    /**
     * @return the id
     */
    String getId() {
        return id;
    }

    /**
     * Sets the id
     * @param id
     */
   void setId(String id) {
        this.id = id;
    }
    
    /**
     * @return the license
     */
    String getLicense() {
        return license;
    }

    /**
     * @return the presentationName
     */
    String getPresentationName() {
        return presentationName;
    }

    /**
     * @return the size
     */
    long getSize() {
        return size;
    }

    /**
     * @return the source
     */
    String getSource() {
        return source;
    }

    /**
     * @return the symbolicName
     */
    String getSymbolicName() {
        return symbolicName;
    }

    /**
     * @return the uri
     */
    String getUri() {
        return uri;
    }
    
    /**
     * Sets the uri
     * @param uri
     */
    void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the vendor
     */
    String getVendor() {
        return vendor;
    }

    /**
     * @return the version
     */
    String getVersion() {
        return version;
    }

    /**
     * @return the URL location of the bundle file
     */
    URL getResourceURL() {
        return resourceURL;
    }
    
    /**
     * @return the name part of the bundle file URL location
     */
    String getResourceName() {
     return name;
    }

    /**
     * Returns an <code>InputStream</code> on the resource bundle file
     * @throws IOException If an error occurrs opening the stream.
     */
    void spool(OutputStream out) throws IOException {
        URLConnection conn = getResourceURL().openConnection();
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
        
        printAttribute(out, "id", id);
        printAttribute(out, "presentationname", presentationName);
        printAttribute(out, "symbolicname", symbolicName);
        printAttribute(out, "uri", uri);
        printAttribute(out, "version", version);
        out.println('>');
        
        String childIndent = indent + " ";
        printElement(out, childIndent, "description", description);
        printElement(out, childIndent, "size", String.valueOf(size));
        printElement(out, childIndent, "documentation", documentation);
        printElement(out, childIndent, "copyright", copyright);
        printElement(out, childIndent, "vendor", vendor);
        printElement(out, childIndent, "contact", contact);
        printElement(out, childIndent, "license", license);
        printElement(out, childIndent, "source", source);
        
        for (Iterator ci=getCategories().iterator(); ci.hasNext(); ) {
            String cat = (String) ci.next();
            out.print("<category");
            printAttribute(out, "id", cat);
            out.println("/>");
        }
        
        printMap(out, childIndent, capabilities);
        printMap(out, childIndent, requirements);
        
        out.print(indent);
        out.println("</resource>");
    }
    
    //---------- Comparable interface -----------------------------------------
    
    /**
     * @throws ClassCastException if <code>obj</code> is not <code>Resource</code>
     */
    public int compareTo(Object obj) {
        if (equals(obj)) {
            return 0;
        }
        
        Resource other = (Resource) obj;
        if (getSymbolicName().equals(other.getSymbolicName())) {
            // compare version
            Version thisVersion = new Version(getVersion());
            Version otherVersion = new Version(other.getVersion());
            return thisVersion.compareTo(otherVersion);
        }
        
        return getSymbolicName().compareTo(other.getSymbolicName());
    }
    
    //---------- Object overwrite ---------------------------------------------

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj instanceof Resource) {
            Resource other = (Resource) obj;
            return getSymbolicName().equals(other.getSymbolicName())
                && getVersion().equals(other.getVersion());
        }
        
        return false;
    }
    
    public int hashCode() {
        return getSymbolicName().hashCode() * 33 + getVersion().hashCode() * 17;
    }
    
    public String toString() {
        return getSymbolicName() + " - " + getVersion();
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
            addCapability(new PackageCapability((Map.Entry) pi.next()));
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
            addRequirement(new PackageRequirement((Map.Entry) pi.next()));
        }
    }
    
    private void addToMapList(Map map, String name, Object value) {
        List current = (List) map.get(name);
        if (current == null) {
            current = new ArrayList();
            map.put(name, current);
        }
        current.add(value);
    }
    
    private void addBundleSpecs(String spec) {
        if (spec == null) {
            // this is not expected ...
            bundleSpecs = new BundleSpec[0];
        } else {
            spec = spec.trim();
            List specs = new ArrayList();
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
            bundleSpecs = (BundleSpec[]) specs.toArray(new BundleSpec[specs.size()]);
        }
    }

    private Iterator getMapListIterator(final Map map) {
        return new Iterator() {
            private Iterator mapIter;
            private Iterator listIter;
            private Object next;
            
            {
                mapIter = map.values().iterator();
                next = seek();
            }
            
            public boolean hasNext() {
                return next != null;
            }
            
            public Object next() {
                if (next == null) {
                    throw new NoSuchElementException("next");
                }
                
                Object toReturn = next;
                next = seek();
                return toReturn;
            }
            
            private Object seek() {
                while (listIter == null || !listIter.hasNext()) {
                    if (mapIter.hasNext()) {
                        List nextlist = (List) mapIter.next();
                        listIter = nextlist.iterator();
                    } else {
                        return null;
                    }
                }
                
                return listIter.next();
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
        for (Iterator mi=getMapListIterator(map); mi.hasNext(); ) {
            Serializable ser = (Serializable) mi.next();
            ser.serialize(out, indent);
        }
    }
}
