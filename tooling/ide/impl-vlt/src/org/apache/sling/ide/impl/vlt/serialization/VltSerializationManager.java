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
import java.net.URISyntaxException;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.vault.fs.Mounter;
import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.Aggregator;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.impl.aggregator.FileAggregator;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.jackrabbit.vault.util.MimeTypes;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.sling.ide.impl.vlt.RepositoryUtils;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.SAXException;

public class VltSerializationManager implements SerializationManager {

    public static void main(String[] args) throws RepositoryException, URISyntaxException, IOException {
        RepositoryAddress address = new RepositoryAddress("http://localhost:8080/server/root");
        Repository repo = new RepositoryProvider().getRepository(address);
        Session session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

        VaultFileSystem fs = Mounter.mount(null, null, address, "/", session);

        String[] attempts = new String[] { "/rep:policy", "/var" };

        for (String attempt : attempts) {
            VaultFile vaultFile = fs.getFile(attempt);

            System.out.println(attempt + " -> " + vaultFile);
        }

        for (String attempt : attempts) {

            attempt = PlatformNameFormat.getPlatformPath(attempt) + ".xml";

            VaultFile vaultFile = fs.getFile(attempt);

            System.out.println(attempt + " -> " + vaultFile);
        }

    }

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

    // TODO - the return type could look like (byte[] contents, String nameHint, SerializationKind sk)

    @Override
    public String buildSerializationData(File contentSyncRoot, ResourceProxy resource, RepositoryInfo repositoryInfo) throws IOException {

        // TODO - there might be a performance problem with getting the session on-demand each time
        // the resolution might be to have a SerializationManager instance kept per 'transaction'
        // which is stateful, with init(RepositoryInfo) and destroy() methods
        Session session = null;
        try {
            
            Repository repo = RepositoryUtils.getRepository(repositoryInfo);
            Credentials credentials = RepositoryUtils.getCredentials(repositoryInfo);
            
            session = repo.login(credentials);

            RepositoryAddress address = RepositoryUtils.getRepositoryAddress(repositoryInfo);

            VaultFileSystem fs = fsLocator.getFileSystem(address, contentSyncRoot, session);

            VaultFile vaultFile = fs.getFile(resource.getPath());
            if (vaultFile == null) {

                // TODO - not sure why we need to try both ... not a performance impact but ugly nonetheless
                String platformPath = PlatformNameFormat.getPlatformPath(resource.getPath()) + ".xml";
                vaultFile = fs.getFile(platformPath);

                if (vaultFile == null) {
                    // TODO proper logging ; discover if this is expected or not
                    System.err.println("No vaultFile at path " + resource.getPath());
                    return null;
                }
            }

            Aggregate aggregate = vaultFile.getAggregate();

            if (aggregate == null)
                throw new IllegalArgumentException("No aggregate found for path " + resource.getPath());

            Aggregator aggregator = fs.getAggregateManager().getAggregator(aggregate.getNode(), null);
            if (aggregator instanceof FileAggregator) {
                // TODO - copy-pasted from FileAggregator, and really does not belong here...
                Node content = aggregate.getNode();
                if (content.isNodeType(JcrConstants.NT_FILE)) {
                    content = content.getNode(JcrConstants.JCR_CONTENT);
                }
                String mimeType = null;
                if (content.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                    try {
                        mimeType = content.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                    } catch (RepositoryException e) {
                        // ignore
                    }
                }
                if (mimeType == null) {
                    // guess mime type from name
                    mimeType = MimeTypes.getMimeType(aggregate.getNode().getName(), MimeTypes.APPLICATION_OCTET_STREAM);
                }

                boolean needsDir = !MimeTypes.matches(aggregate.getNode().getName(), mimeType,
                        MimeTypes.APPLICATION_OCTET_STREAM);
                if (!needsDir) {
                    return null;
                }
            }

            DocViewSerializer s = new DocViewSerializer(aggregate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.writeContent(out);
            
            String stringResult = out.toString("UTF-8");
            if (stringResult.isEmpty())
                return null;
            return stringResult;

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public Map<String, Object> readSerializationData(InputStream source) throws IOException {

        if (source == null)
            return null;

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser parser = factory.newSAXParser();
            ContentXmlHandler handler = new ContentXmlHandler();
            parser.parse(source, handler);

            return handler.getProperties();
        } catch (SAXException e) {
            // TODO proper error handling
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            // TODO proper error handling
            throw new IOException(e);
        }

    }
}
