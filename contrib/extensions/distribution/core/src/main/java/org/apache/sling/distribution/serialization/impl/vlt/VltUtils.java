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
import org.apache.jackrabbit.vault.packaging.VaultPackage;

import java.util.List;
import java.util.Properties;

/**
 * Utility class for creating vlt filters and import/export options
 */
public class VltUtils {

    public static WorkspaceFilter createFilter(String[] paths, boolean deep) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();

        for (String path : paths) {
            PathFilterSet filterSet = createFilterSet(path, deep);
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

    private static PathFilterSet createFilterSet(String path, boolean deep) {
        PathFilterSet filterSet = new PathFilterSet(path);

        if (!deep) {
            filterSet.addInclude(new DefaultPathFilter(path));
        }
        return filterSet;
    }

    public static ExportOptions getExportOptions(WorkspaceFilter filter,
                                                 String packageGroup,
                                                 String packageName,
                                                 String packageVersion) {
        DefaultMetaInf inf = new DefaultMetaInf();
        ExportOptions opts = new ExportOptions();
        inf.setFilter(filter);

        Properties props = new Properties();
        props.setProperty(VaultPackage.NAME_GROUP, packageGroup);
        props.setProperty(VaultPackage.NAME_NAME, packageName);
        props.setProperty(VaultPackage.NAME_VERSION, packageVersion);
        inf.setProperties(props);

        opts.setMetaInf(inf);
        opts.setRootPath("/");

        return opts;
    }

    public static ImportOptions getImportOptions(AccessControlHandling aclHandling, ImportMode importMode) {
        ImportOptions opts = new ImportOptions();
        if (aclHandling != null) {
            opts.setAccessControlHandling(aclHandling);
        }
        else {
            // default to overwrite
            opts.setAccessControlHandling(AccessControlHandling.OVERWRITE);
        }
        if (importMode != null) {
            opts.setImportMode(importMode);
        }
        else {
            // default to update
            opts.setImportMode(ImportMode.UPDATE);
        }

        return opts;
    }
}
