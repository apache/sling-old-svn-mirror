/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating vlt filters and import/export options
 */
public class VltUtils {

    private final static Logger log = LoggerFactory.getLogger(VltUtils.class);

    public static WorkspaceFilter createFilter(DistributionRequest distributionRequest, NavigableMap<String, List<String>> filters) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();

        for (String path : distributionRequest.getPaths()) {

            PathFilterSet filterSet = createFilterSet(path, filters, distributionRequest);
            filter.add(filterSet);
        }

        return filter;
    }

    public static String[] getPaths(MetaInf metaInf) {
        if (metaInf == null) {
            return null;
        }

        WorkspaceFilter filter = metaInf.getFilter();
        if (filter == null) {
            filter = new DefaultWorkspaceFilter();
        }
        List<PathFilterSet> filterSets = filter.getFilterSets();
        String[] paths = new String[filterSets.size()];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = filterSets.get(i).getRoot();
        }

        return paths;
    }

    private static PathFilterSet createFilterSet(String path, NavigableMap<String, List<String>> globalFilters, DistributionRequest distributionRequest) {
        boolean deep = distributionRequest.isDeep(path);
        PathFilterSet filterSet = new PathFilterSet(path);

        if (!deep) {
            filterSet.addInclude(new DefaultPathFilter(path));
        }

        List<String> patterns = new ArrayList<String>();

        // add the most specific filter rules
        for (String key : globalFilters.descendingKeySet()) {
            if (path.startsWith(key)) {
                patterns.addAll(globalFilters.get(key));
                break;
            }
        }

        patterns.addAll(Arrays.asList(distributionRequest.getFilters(path)));

        for (String pattern : patterns) {
            PathFilterSet.Entry<DefaultPathFilter> entry = extractPathPattern(pattern);

            if (entry.isInclude()) {
                filterSet.addInclude(entry.getFilter());
            } else {
                filterSet.addExclude(entry.getFilter());
            }
        }

        return filterSet;
    }


    public static ExportOptions getExportOptions(WorkspaceFilter filter, String[] packageRoots,
                                                 String packageGroup,
                                                 String packageName,
                                                 String packageVersion,
                                                 boolean useBinaryReferences) {
        DefaultMetaInf inf = new DefaultMetaInf();
        ExportOptions opts = new ExportOptions();
        inf.setFilter(filter);

        Properties props = new Properties();
        props.setProperty(VaultPackage.NAME_GROUP, packageGroup);
        props.setProperty(VaultPackage.NAME_NAME, packageName);
        props.setProperty(VaultPackage.NAME_VERSION, packageVersion);
    	props.setProperty(PackageProperties.NAME_USE_BINARY_REFERENCES, String.valueOf(useBinaryReferences));
        inf.setProperties(props);

        opts.setMetaInf(inf);

        String root = getPackageRoot(filter.getFilterSets(), packageRoots);
        opts.setRootPath(root);
        opts.setMountPath(root);

        return opts;
    }


    /**
     * Picks a package root that dominates all filter sets. If there is none then "/" is returned.
     */
    private static String getPackageRoot(List<PathFilterSet> filterSets, String[] packageRoots) {

        String packageRoot = null;

        if (packageRoots != null && packageRoots.length > 0) {
            for (String currentRoot : packageRoots) {
                boolean filtersHaveCommonRoot = true;


                for (PathFilterSet filterSet : filterSets) {
                    String filterSetRoot = filterSet.getRoot();

                    if (!filterSetRoot.startsWith(currentRoot)) {
                        filtersHaveCommonRoot = false;
                    }
                }

                if (filtersHaveCommonRoot) {
                    packageRoot = currentRoot;
                    break;
                }
            }

        }


        if (packageRoot == null || !packageRoot.startsWith("/")) {
            packageRoot = "/";
        }

        return packageRoot;

    }

    public static ImportOptions getImportOptions(AccessControlHandling aclHandling, ImportMode importMode, int autosaveThreshold) {
        ImportOptions opts = new ImportOptions();
        if (aclHandling != null) {
            opts.setAccessControlHandling(aclHandling);
        } else {
            // default to overwrite
            opts.setAccessControlHandling(AccessControlHandling.OVERWRITE);
        }
        if (importMode != null) {
            opts.setImportMode(importMode);
        } else {
            // default to update
            opts.setImportMode(ImportMode.UPDATE);
        }

        if (autosaveThreshold >= 0) {
            opts.setAutoSaveThreshold(autosaveThreshold);
        }

        return opts;
    }

    public static VaultPackage createPackage(PackageManager packageManager, Session session, ExportOptions options, File tempFolder) throws IOException, RepositoryException {
        File file = File.createTempFile("distr-vault-create-" + System.nanoTime(), ".zip", tempFolder);

        try {
            return packageManager.assemble(session, options, file);
        } catch (RepositoryException e) {
            FileUtils.deleteQuietly(file);
            throw e;
        }
    }

    public static VaultPackage readPackage(PackageManager packageManager, InputStream stream, File tempFolder) throws IOException {
        File file = File.createTempFile("distr-vault-read-" + System.nanoTime(), ".zip", tempFolder);
        OutputStream out = FileUtils.openOutputStream(file);
        try {
            IOUtils.copy(stream, out);
            return packageManager.open(file);
        } catch (IOException e) {
            FileUtils.deleteQuietly(file);
            throw e;
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
    }

    public static void deletePackage(VaultPackage vaultPackage) {
        if (vaultPackage == null) {
            return;
        }

        File file = vaultPackage.getFile();
        vaultPackage.close();

        FileUtils.deleteQuietly(file);
    }

    public static void deletePackage(JcrPackage jcrPackage) {
        if (jcrPackage == null) {
            return;
        }

        Node node = jcrPackage.getNode();
        jcrPackage.close();

        try {
            if (node != null) {
                node.remove();
            }
        } catch (RepositoryException e) {
            // do nothing
        }
    }

    public static File getTempFolder(String tempFolderPath) {
        File directory = null;
        try {
            directory = new File(tempFolderPath);
            if (!directory.exists() || !directory.isDirectory()) {
                directory = null;
            }
        } catch (Throwable e) {
            directory = null;
        }

        return directory;
    }

    public static String findParent(String path, String nodeName) {
        path = path.endsWith("/") ? path : path + "/";

        nodeName = "/" + nodeName + "/";

        int idx = path.indexOf(nodeName);

        if (idx < 0) {
            return null;
        }

        return path.substring(0, idx);
    }

    public static String appendMatchAll(String path) {
        path = path.endsWith("/") ? path : path + "/";
        path = path + ".*";
        return path;
    }

    public static TreeMap<String, List<String>> parseFilters(String[] filters) {

        TreeMap<String, List<String>> result = new TreeMap<String, List<String>>();

        if (filters == null || filters.length == 0) {
            return result;
        }

        for (String filter : filters) {
            String[] filterParts = filter.split("\\|");
            if (filterParts.length > 1) {
                String path = SettingsUtils.removeEmptyEntry(filterParts[0]);
                if (path == null) {
                    continue;
                }

                List<String> filterSet = new ArrayList<String>();

                for (int i = 1; i < filterParts.length; i++) {
                    String filterPart = SettingsUtils.removeEmptyEntry(filterParts[i]);
                    if (filterPart == null) {
                        continue;
                    }

                    filterSet.add(filterPart);
                }

                result.put(path, filterSet);

            }
        }

        return result;
    }

    public static DistributionRequest sanitizeRequest(DistributionRequest request) {

        DistributionRequestType requestType = request.getRequestType();

        if (!DistributionRequestType.ADD.equals(requestType) && !DistributionRequestType.DELETE.equals(requestType)) {
            return request;
        }

        Set<String> deepPaths = new HashSet<String>();
        List<String> paths = new ArrayList<String>();
        Map<String, String[]> filters = new HashMap<String, String[]>();

        for (String path : request.getPaths()) {
            if (VltUtils.findParent(path, "rep:policy") != null) {
                if (DistributionRequestType.DELETE.equals(requestType)) {
                    // vlt cannot properly install delete of rep:policy subnodes
                    throw new IllegalArgumentException("cannot distribute DELETE node " + path);
                } else if (DistributionRequestType.ADD.equals(requestType)) {
                    String newPath = VltUtils.findParent(path, "rep:policy") + "/rep:policy";
                    paths.add(newPath);
                    deepPaths.add(newPath);
                    log.debug("changed distribution path {} to deep path {}", path, newPath);
                }
            } else if (request.isDeep(path)) {
                paths.add(path);
                deepPaths.add(path);
            } else {
                paths.add(path);
            }

            filters.put(path, request.getFilters(path));
        }

        return new SimpleDistributionRequest(requestType, paths.toArray(new String[paths.size()]), deepPaths, filters);
    }

    private static PathFilterSet.Entry<DefaultPathFilter> extractPathPattern(String pattern) {
        PathFilterSet.Entry<DefaultPathFilter> result = null;
        if (pattern.startsWith("+")) {
            result = new PathFilterSet.Entry<DefaultPathFilter>(new DefaultPathFilter(pattern.substring(1)), true);
        } else if (pattern.startsWith("-")) {
            result = new PathFilterSet.Entry<DefaultPathFilter>(new DefaultPathFilter(pattern.substring(1)), false);
        } else {
            result = new PathFilterSet.Entry<DefaultPathFilter>(new DefaultPathFilter(pattern), true);
        }

        return result;
    }
}
