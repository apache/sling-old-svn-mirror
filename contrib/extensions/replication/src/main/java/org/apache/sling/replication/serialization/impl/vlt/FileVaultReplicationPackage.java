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
package org.apache.sling.replication.serialization.impl.vlt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * a FileVault {@link ReplicationPackage}
 */
public class FileVaultReplicationPackage implements ReplicationPackage {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final String[] paths;

    private final VaultPackage pkg;

    private final String action;

    public FileVaultReplicationPackage(VaultPackage pkg) {
        this.pkg = pkg;
        List<PathFilterSet> filterSets = pkg.getMetaInf().getFilter().getFilterSets();
        String[] paths = new String[filterSets.size()];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = filterSets.get(i).getRoot();
        }
        this.paths = paths;
        this.id = pkg.getFile().getAbsolutePath();
        this.action = ReplicationActionType.ADD.toString();
    }

    public String getId() {
        return id;
    }

    public String[] getPaths() {
        return paths;
    }

    public InputStream createInputStream() throws IOException {
        return new FileInputStream(pkg.getFile());
    }

    public long getLength() {
        return pkg.getFile().length();
    }

    public String getType() {
        return FileVaultReplicationPackageBuilder.NAME;
    }

    public String getAction() {
        return action;
    }

}
