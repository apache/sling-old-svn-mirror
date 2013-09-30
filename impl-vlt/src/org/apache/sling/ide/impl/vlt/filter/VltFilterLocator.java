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
package org.apache.sling.ide.impl.vlt.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;

public class VltFilterLocator implements FilterLocator {

    private VaultFsLocator fsLocator;

    protected void bindVaultFsLocator(VaultFsLocator fsLocator) {
        this.fsLocator = fsLocator;
    }

    protected void unbindVaultFsLocator(VaultFsLocator fsLocator) {
        this.fsLocator = null;
    }

    @Override
    public File findFilterLocation(File syncDirectory) {

        return fsLocator.findFilterFile(syncDirectory);
    }

    @Override
    public Filter loadFilter(InputStream filterFileContents) throws IOException {

        try {
            return new VltFilter(filterFileContents);
        } catch (ConfigurationException e) {
            // TODO proper error handling
            throw new IOException(e);
        }
    }

}
