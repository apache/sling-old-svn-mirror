/*
 * $Url: $
 * $Id:Repository.java 25139 2007-02-12 15:37:26Z fmeschbe $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.obr;

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

    private static final String PROP_REPO_NAME = "org.apache.sling.obr.name";

    private static final String PROP_PREFIX_BUNDLE = "org.apache.sling.obr.bundle.";

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

        loadProperties();
        needReload = true;
    }

    public String getName() {
        return repoName;
    }

    public long getLastModified() {
        return repoLastModified;
    }

    public String getLastModifiedFormatted() {
        return REPO_DATE_FORMAT.format(new Date(getLastModified()));
    }

    public void addResource(InputStream bundleStream) throws IOException {
        File bundle = null;
        try {
            // spool the bundle (throw if invalid)
            bundle = spoolModified(bundleStream);

            // Check bundle by trying to read the manifest, throws in
            // case of problems, never returns null
            Resource.create(bundle);

            bundles.add(bundle.toURI().toString());
            needReload = true;

            saveProperties();
        } catch (IOException ioe) {
            // bundle seems invalid, remove the file
            if (bundle != null) {
                bundle.delete();
            }
            throw ioe;
        }
    }

    public Resource getResource(String bundleName) {
        ensureLoaded();

        // try the bundleName as resource file name first
        Resource res = (Resource) resourcesByFileName.get(bundleName);
        
        // not found, assume a bundle symbolic name
        if (res == null) {
            for (Iterator ri = getResourcesById(); ri.hasNext();) {
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
        ensureLoaded();
        return resources.iterator();
    }

    public Iterator getResourcesByCategory(String categoryName) {
        ensureLoaded();

        Set resources = (Set) resourcesByCategory.get(categoryName);
        if (resources != null) {
            return resources.iterator();
        }
        return Collections.EMPTY_SET.iterator();
    }

    public void removeResource(String bundleName) throws IOException {
        needReload = true;
        File bundleFile = new File(repoLocation, bundleName);
        bundles.remove(bundleFile.toURI().toString());
        bundleFile.delete();
        saveProperties();
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
        ensureLoaded();

        List list = new ArrayList(resourcesByCategory.keySet());
        if (comparator != null) {
            Collections.sort(list, comparator);
        }
        return list;
    }

    private void ensureLoaded() {
        if (!needReload) {
            return;
        }

        // load the resources
        resources = new ArrayList();
        resourcesByFileName = new TreeMap();
        resourcesByCategory = new TreeMap();

        Iterator bi = bundles.iterator();
        for (int id = 0; bi.hasNext(); id++) {
            String mapping = (String) bi.next();
            try {
                URL bundle = new URL(mapping);
                Resource res = Resource.create(bundle);
                res.setId(String.valueOf(id));

                resources.add(res);
                resourcesByFileName.put(res.getResourceName(), res);

                registerResourceByCategory(CATEGORY_ALL_BUNDLES, res);
                if (res.getCategories().isEmpty()) {
                    registerResourceByCategory(CATEGORY_NONE, res);
                } else {
                    for (Iterator ci = res.getCategories().iterator(); ci.hasNext();) {
                        registerResourceByCategory(ci.next(), res);
                    }
                }

            } catch (MalformedURLException mue) {
                // TODO: log, but actually not expected ...
            } catch (IOException ioe) {
                // TODO: log
            }
        }

        // prevent further "reloads" until change
        needReload = false;
    }

    private void registerResourceByCategory(Object category, Resource res) {
        SortedSet resources = (SortedSet) resourcesByCategory.get(category);
        if (resources == null) {
            resources = new TreeSet();
            resourcesByCategory.put(category, resources);
        }
        resources.add(res);
    }

    private void loadProperties() throws IOException {
        Properties props = new Properties();
        if (repoPropertiesFile.exists()) {
            InputStream ins = null;
            try {
                ins = new FileInputStream(repoPropertiesFile);
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
            repoLastModified = repoPropertiesFile.lastModified();
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
            saveProperties();
        } else {
            // use the configured name as the repository Name
            this.repoName = repoName;
        }
    }

    private void saveProperties() throws IOException {
        // prepare the properties to write
        Properties props = new Properties();
        props.setProperty(PROP_REPO_NAME, repoName);
        Iterator bi = bundles.iterator();
        for (int i = 0; bi.hasNext(); i++) {
            props.setProperty(PROP_PREFIX_BUNDLE + i, (String) bi.next());
        }

        // write the properties
        OutputStream out = null;
        try {
            out = new FileOutputStream(repoPropertiesFile);
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
        repoLastModified = repoPropertiesFile.lastModified();
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
            version = v.getMajor()
                + "."
                + v.getMinor()
                + "."
                + v.getMicro()
                + "."
                + v.getQualifier().replaceAll("SNAPSHOT",
                    DATE_FORMAT.format(new Date()));
            manifest.getMainAttributes().putValue("Bundle-Version", version);
        }

        File bundle = new File(repoLocation, symbolicName + "-" + v + ".jar");
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
