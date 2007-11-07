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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Version;

/**
 * The <code>Repository</code> represents the collection of all bundles in the
 * repository. It is initialized with the location of the bundle store and may
 * be accessed by different accessors.
 *
 * @author fmeschbe
 * @version $Rev:25139 $, $Date:2007-02-12 16:37:26 +0100 (Mo, 12 Feb 2007) $
 */
public class Repository {

    /**
     * Pseudo bundle category to which all bundles belong by definition (value
     * is "[All Bundles]").
     */
    public static final String CATEGORY_ALL_BUNDLES = "[All Bundles]";

    /**
     * Pseudo bundle category to which all bundles belong which do not have a
     * <code>Bundle-Category</code> header in their bundle manifast (value is
     * "[None]").
     */
    public static final String CATEGORY_NONE = "[None]";

    private static final String REPO_PROPERTIES = "repository.properties";

    private static final String PROP_REPO_NAME = "org.apache.sling.osgi.obr.name";

    private static final String PROP_PREFIX_BUNDLE = "org.apache.sling.osgi.obr.bundle.";

    // 20060817025624.330
    private static final DateFormat REPO_DATE_FORMAT = new SimpleDateFormat(
        "yyyyMMddHHmmss.SSS");

    // 20060817025624
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
        "yyyyMMddHHmmss");

    private File repoLocation;

    private File repoPropertiesFile;

    private long repoLastModified;

    private String repoName;

    // sorted set of bundle URIs
    private SortedSet bundles;

    // true if a reload is requird in ensureLoaded
    private boolean needReload;

    // map of resource sets indexed by category
    private SortedMap resourcesByCategory;

    // list of resources
    private List resources;

    // map of resources indexed by file name
    private SortedMap resourcesByFileName;

    public Repository(String defaultRepoName, File repoLocation)
            throws IOException {
        this.repoName = (defaultRepoName != null)
                ? defaultRepoName
                : "untitled";
        this.repoLocation = repoLocation;
        this.repoPropertiesFile = new File(repoLocation, REPO_PROPERTIES);

        this.loadProperties();
        this.needReload = true;
    }

    public String getName() {
        return this.repoName;
    }

    public long getLastModified() {
        return this.repoLastModified;
    }

    public String getLastModifiedFormatted() {
        synchronized (REPO_DATE_FORMAT) {
            return REPO_DATE_FORMAT.format(new Date(this.getLastModified()));
        }
    }

    public void addResource(InputStream bundleStream) throws IOException {
        File bundle = null;
        try {
            // spool the bundle (throw if invalid)
            bundle = this.spoolModified(bundleStream);

            // Check bundle by trying to read the manifest, throws in
            // case of problems, never returns null
            Resource.create(bundle);

            this.bundles.add(bundle.toURI().toString());
            this.needReload = true;

            this.saveProperties();
        } catch (IOException ioe) {
            // bundle seems invalid, remove the file
            if (bundle != null) {
                bundle.delete();
            }
            throw ioe;
        }
    }

    public Resource getResource(String bundleName) {
        this.ensureLoaded();

        // try the bundleName as resource file name first
        Resource res = (Resource) this.resourcesByFileName.get(bundleName);

        // not found, assume a bundle symbolic name
        if (res == null) {
            for (Iterator ri = this.getResourcesById(); ri.hasNext();) {
                Resource testRes = (Resource) ri.next();
                if (bundleName.equals(testRes.getSymbolicName())) {
                    if (res == null
                        || testRes.getVersion().compareTo(res.getVersion()) > 0) {
                        res = testRes;
                    }
                }
            }
        }

        // return what we found (best match or nothing at all)
        return res;
    }

    public Iterator getResourcesById() {
        this.ensureLoaded();
        return this.resources.iterator();
    }

    public Iterator getResourcesByCategory(String categoryName) {
        this.ensureLoaded();

        Set resources = (Set) this.resourcesByCategory.get(categoryName);
        if (resources != null) {
            return resources.iterator();
        }
        return Collections.EMPTY_SET.iterator();
    }

    public void removeResource(String bundleName) throws IOException {
        this.needReload = true;
        File bundleFile = new File(this.repoLocation, bundleName);
        this.bundles.remove(bundleFile.toURI().toString());
        bundleFile.delete();
        this.saveProperties();
    }

    /**
     * Returns a sorted list of all categories to which the bundles stored in
     * this repository belong. This list also contains the special category
     * {@link #CATEGORY_ALL_BUNDLES}.
     *
     * @param comparator The <code>Comparator</code> to use to sort the bundle
     *            list. If <code>null</code> the natural ordering is used.
     * @return The sorted list of bundle categories of the bundles.
     */
    public List getBundleCategories(Comparator comparator) {
        this.ensureLoaded();

        List list = new ArrayList(this.resourcesByCategory.keySet());
        if (comparator != null) {
            Collections.sort(list, comparator);
        }
        return list;
    }

    private void ensureLoaded() {
        if (!this.needReload) {
            return;
        }

        // load the resources
        this.resources = new ArrayList();
        this.resourcesByFileName = new TreeMap();
        this.resourcesByCategory = new TreeMap();

        Iterator bi = this.bundles.iterator();
        for (int id = 0; bi.hasNext(); id++) {
            String mapping = (String) bi.next();
            try {
                URL bundle = new URL(mapping);
                Resource res = Resource.create(bundle);
                res.setId(String.valueOf(id));

                this.resources.add(res);
                this.resourcesByFileName.put(res.getResourceName(), res);

                this.registerResourceByCategory(CATEGORY_ALL_BUNDLES, res);
                if (res.getCategories().isEmpty()) {
                    this.registerResourceByCategory(CATEGORY_NONE, res);
                } else {
                    for (Iterator ci = res.getCategories().iterator(); ci.hasNext();) {
                        this.registerResourceByCategory(ci.next(), res);
                    }
                }

            } catch (MalformedURLException mue) {
                // TODO: log, but actually not expected ...
            } catch (IOException ioe) {
                // TODO: log
            }
        }

        // prevent further "reloads" until change
        this.needReload = false;
    }

    private void registerResourceByCategory(Object category, Resource res) {
        SortedSet resources = (SortedSet) this.resourcesByCategory.get(category);
        if (resources == null) {
            resources = new TreeSet();
            this.resourcesByCategory.put(category, resources);
        }
        resources.add(res);
    }

    private void loadProperties() throws IOException {
        Properties props = new Properties();
        if (this.repoPropertiesFile.exists()) {
            InputStream ins = null;
            try {
                ins = new FileInputStream(this.repoPropertiesFile);
                props.load(ins);
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
            this.repoLastModified = this.repoPropertiesFile.lastModified();
        }

        TreeSet bundles = new TreeSet();
        for (Iterator ei = props.entrySet().iterator(); ei.hasNext();) {
            Map.Entry entry = (Map.Entry) ei.next();
            String key = (String) entry.getKey();
            if (key.startsWith(PROP_PREFIX_BUNDLE)) {
                bundles.add(entry.getValue());
            }
        }
        this.bundles = bundles;

        String repoName = props.getProperty(PROP_REPO_NAME);
        if (repoName == null) {
            // save the properties immediately with the default repository Name
            this.saveProperties();
        } else {
            // use the configured name as the repository Name
            this.repoName = repoName;
        }
    }

    private void saveProperties() throws IOException {
        // prepare the properties to write
        Properties props = new Properties();
        props.setProperty(PROP_REPO_NAME, this.repoName);
        Iterator bi = this.bundles.iterator();
        for (int i = 0; bi.hasNext(); i++) {
            props.setProperty(PROP_PREFIX_BUNDLE + i, (String) bi.next());
        }

        // write the properties
        OutputStream out = null;
        try {
            out = new FileOutputStream(this.repoPropertiesFile);
            props.store(out, null);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // update the last modified value
        this.repoLastModified = this.repoPropertiesFile.lastModified();
    }

    static void spool(InputStream ins, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int rd;
        while ((rd = ins.read(buf)) > 0) {
            out.write(buf, 0, rd);
        }
    }

    File spoolModified(InputStream ins) throws IOException {
        JarInputStream jis = new JarInputStream(ins);

        // immediately handle the manifest
        JarOutputStream jos;
        Manifest manifest = jis.getManifest();
        if (manifest == null) {
            throw new IOException("Missing Manifest !");
        }

        String symbolicName = manifest.getMainAttributes().getValue(
            "Bundle-SymbolicName");
        if (symbolicName == null || symbolicName.length() == 0) {
            throw new IOException("Missing Symbolic Name in Manifest !");
        }

        String version = manifest.getMainAttributes().getValue("Bundle-Version");
        Version v = Version.parseVersion(version);
        if (v.getQualifier().indexOf("SNAPSHOT") >= 0) {
            String tStamp;
            synchronized (DATE_FORMAT) {
                tStamp = DATE_FORMAT.format(new Date());
            }
            version = v.getMajor() + "." + v.getMinor() + "." + v.getMicro()
                + "." + v.getQualifier().replaceAll("SNAPSHOT", tStamp);
            manifest.getMainAttributes().putValue("Bundle-Version", version);
        }

        File bundle = new File(this.repoLocation, symbolicName + "-" + v + ".jar");
        OutputStream out = null;
        try {
            out = new FileOutputStream(bundle);
            jos = new JarOutputStream(out, manifest);

            jos.setMethod(JarOutputStream.DEFLATED);
            jos.setLevel(Deflater.BEST_COMPRESSION);

            JarEntry entryIn = jis.getNextJarEntry();
            while (entryIn != null) {
                JarEntry entryOut = new JarEntry(entryIn.getName());
                entryOut.setTime(entryIn.getTime());
                entryOut.setComment(entryIn.getComment());
                jos.putNextEntry(entryOut);
                if (!entryIn.isDirectory()) {
                    spool(jis, jos);
                }
                jos.closeEntry();
                jis.closeEntry();
                entryIn = jis.getNextJarEntry();
            }

            // close the JAR file now to force writing
            jos.close();

        } finally {
            IOUtils.closeQuietly(out);
        }

        return bundle;
    }

}
