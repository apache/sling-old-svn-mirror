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
package org.apache.sling.ide.impl.vlt.serialization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.sling.ide.impl.vlt.RepositoryUtils;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;

public class VltSerializationManager implements SerializationManager {

    private VaultFsLocator fsLocator;

    @Override
    public boolean isSerializationFile(String filePath) {
        return new File(filePath).getName().equals(Constants.DOT_CONTENT_XML);
    }

    @Override
    public String getBaseResourcePath(String serializationFilePath) {
        // TODO
        return null;
    }

    @Override
    public String getSerializationFilePath(String baseFilePath) {
        // TODO - validate if this is correct
        return baseFilePath + File.separatorChar + Constants.DOT_CONTENT_XML;
    }

    protected void bindVaultFsLocator(VaultFsLocator fsLocator) {

        this.fsLocator = fsLocator;
    }

    protected void unbindVaultFsLocator(VaultFsLocator fsLocator) {

        this.fsLocator = null;
    }

    @Override
    public String buildSerializationData(ResourceProxy resource, RepositoryInfo repositoryInfo) throws IOException {

        // TODO - there might be a performance problem with getting the session on-demand each time
        Session session = null;
        try {
            
            Repository repo = RepositoryUtils.getRepository(repositoryInfo);
            Credentials credentials = RepositoryUtils.getCredentials(repositoryInfo);
            
            session = repo.login(credentials);

            RepositoryAddress address = RepositoryUtils.getRepositoryAddress(repositoryInfo);

            VaultFileSystem fs = fsLocator.getFileSystem(address, session);

            VaultFile vaultFile = fs.getFile(resource.getPath());
            if (vaultFile == null) {
                // TODO proper logging ; discover if this is expected or not
                System.err.println("No vaultFile at path " + resource.getPath());
                return null;
            }

            Aggregate aggregate = vaultFile.getAggregate();
            if (aggregate == null)
                throw new IllegalArgumentException("No aggregate found for path " + resource.getPath());

            DocViewSerializer s = new DocViewSerializer(aggregate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.writeContent(out);
            
            String stringResult = out.toString("UTF-8");
            if (stringResult.isEmpty())
                return null;
            return stringResult;

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public Map<String, Object> readSerializationData(InputStream source) throws IOException {
        return null;
    }
}
