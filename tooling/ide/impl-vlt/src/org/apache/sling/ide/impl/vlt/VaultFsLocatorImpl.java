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
package org.apache.sling.ide.impl.vlt;

import java.io.File;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.Mounter;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.api.VaultFsConfig;
import org.apache.jackrabbit.vault.fs.config.AbstractVaultFsConfig;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.util.Constants;

public class VaultFsLocatorImpl implements VaultFsLocator {

    @Override
    public VaultFileSystem getFileSystem(RepositoryAddress repositoryAddress, File contentSyncRoot, Session session)
            throws RepositoryException, IOException, ConfigurationException {

        // TODO - should not use File to read from FS, rather input streams
        VaultFsConfig config = null;
        DefaultWorkspaceFilter filter = null;

        File filterFile = findFilterFile(contentSyncRoot);
        if (filterFile != null) {
            filter = new DefaultWorkspaceFilter();
            filter.load(filterFile);
        }

        File metaInfDir = new File(contentSyncRoot.getParent(), Constants.META_INF);

        if (metaInfDir.isDirectory()) {
            File vaultDir = new File(metaInfDir, Constants.VAULT_DIR);
            if (vaultDir.isDirectory()) {

                File configFile = new File(vaultDir, Constants.CONFIG_XML);
                if (configFile.exists()) {
                    config = AbstractVaultFsConfig.load(configFile);
                }
            }
        }

        return Mounter.mount(config, filter, repositoryAddress, "/", session);
    }

    @Override
    public File findFilterFile(File contentSyncRoot) {

        File metaInfDir = new File(contentSyncRoot.getParent(), Constants.META_INF);
        if (metaInfDir.isDirectory()) {
            File vaultDir = new File(metaInfDir, Constants.VAULT_DIR);
            if (vaultDir.isDirectory()) {

                File filterFile = new File(vaultDir, Constants.FILTER_VLT_XML);
                if (filterFile.isFile()) {
                    return filterFile;
                } else {
                    filterFile = new File(vaultDir, Constants.FILTER_XML);
                    if (filterFile.isFile()) {
                        return filterFile;
                    }
                }
            }
        }

        return null;
    }

}
