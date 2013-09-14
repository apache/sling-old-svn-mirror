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

import static org.apache.sling.ide.util.PathUtil.getName;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

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
import org.apache.jackrabbit.vault.fs.api.SerializationType;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.impl.aggregator.FileAggregator;
import org.apache.jackrabbit.vault.fs.impl.aggregator.GenericAggregator;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.fs.impl.io.XmlAnalyzer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.jackrabbit.vault.util.MimeTypes;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.sling.ide.impl.vlt.RepositoryUtils;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.serialization.SerializationData;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VltSerializationManager implements SerializationManager {

    private static final String EXTENSION_XML = ".xml";

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

            attempt = PlatformNameFormat.getPlatformPath(attempt) + EXTENSION_XML;

            VaultFile vaultFile = fs.getFile(attempt);

            System.out.println(attempt + " -> " + vaultFile);
        }

    }

    private VaultFsLocator fsLocator;
    private SerializationKindManager skm;

    @Override
    public void init(org.apache.sling.ide.transport.Repository repository, File contentSyncRoot)
            throws SerializationException {

        try {
            this.skm = new SerializationKindManager();
            this.skm.init(repository);
        } catch (org.apache.sling.ide.transport.RepositoryException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isSerializationFile(String filePath) {
        
        File file = new File(filePath);
        String fileName = file.getName();
        if (fileName.equals(Constants.DOT_CONTENT_XML)) {
            return true;
        }

        if (!fileName.endsWith(EXTENSION_XML)) {
            return false;
        }

        // TODO - refrain from doing I/O here
        // TODO - copied from TransactionImpl
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            SerializationType serType = XmlAnalyzer.analyze(new InputSource(in));
            return serType == SerializationType.XML_DOCVIEW;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // don't care
                }
            }
        }
    }

    @Override
    public String getBaseResourcePath(String serializationFilePath) {
        // TODO actually calculate the resource path, this fails for full coverage aggregates
        if (Constants.DOT_CONTENT_XML.equals(serializationFilePath)) {
            return "";
        }

        return serializationFilePath.substring(0, serializationFilePath.length()
                - (Constants.DOT_CONTENT_XML.length() + 1));
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
    public SerializationData buildSerializationData(File contentSyncRoot, ResourceProxy resource,
            RepositoryInfo repositoryInfo) throws SerializationException {

        // TODO - there is a small mismatch here since we're doing remote calls to the repository
        // but taking a resourceProxy - not sure if we'll run into problems down the road or not

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
            String platformPath = resource.getPath();
            if (vaultFile == null) {

                // TODO - not sure why we need to try both ... not a performance impact but ugly nonetheless
                platformPath = PlatformNameFormat.getPlatformPath(resource.getPath()) + EXTENSION_XML;
                vaultFile = fs.getFile(platformPath);

                if (vaultFile == null) {
                    platformPath = PlatformNameFormat.getPlatformPath(resource.getPath());
                    vaultFile = fs.getFile(platformPath);
                }

                if (vaultFile == null) {
                    // TODO proper logging ; discover if this is expected or not and fail hard if it's not
                    System.err.println("No vaultFile at path " + resource.getPath());
                    return null;
                }
            }

            String nameHint = getName(platformPath);
            String fileOrFolderPathHint = vaultFile.getPath();

            Aggregate aggregate = vaultFile.getAggregate();

            if (aggregate == null)
                throw new IllegalArgumentException("No aggregate found for path " + resource.getPath());

            SerializationKind serializationKind = skm.getSerializationKind(aggregate.getNode().getPrimaryNodeType()
                    .getName());

            if (resource.getPath().equals("/") || serializationKind == SerializationKind.METADATA_PARTIAL
                    || serializationKind == SerializationKind.FILE || serializationKind == SerializationKind.FOLDER) {
                nameHint = Constants.DOT_CONTENT_XML;
            }

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
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
            } else if (aggregator instanceof GenericAggregator) {
                // TODO - copy-pasted from GenericAggregator
                if (aggregate.getNode().getPrimaryNodeType().getName().equals("nt:folder")
                        && aggregate.getNode().getMixinNodeTypes().length == 0) {
                    return SerializationData.empty(fileOrFolderPathHint, serializationKind);
                }
            }


            DocViewSerializer s = new DocViewSerializer(aggregate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            s.writeContent(out);
            
            byte[] result = out.toByteArray();

            return new SerializationData(fileOrFolderPathHint, nameHint, result, serializationKind);

        } catch (RepositoryException e) {
            throw new SerializationException(e);
        } catch (ConfigurationException e) {
            throw new SerializationException(e);
        } catch (IOException e) {
            throw new SerializationException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public ResourceProxy readSerializationData(String filePath, InputStream source) throws IOException {

        if (source == null)
            return null;

        String repositoryPath;
        File file = new File(filePath);
        if (file.getName().equals(Constants.DOT_CONTENT_XML)) {
            repositoryPath = PlatformNameFormat.getRepositoryPath(file.getParent());
        } else {
            if (!filePath.endsWith(EXTENSION_XML)) {
                throw new IllegalArgumentException("Don't know how to extract resource path from file named "
                        + filePath);
            }
            repositoryPath = PlatformNameFormat.getRepositoryPath(filePath.substring(0,
                    filePath.length() - EXTENSION_XML.length()));
        }

        // TODO extract into PathUtils
        if (repositoryPath.length() > 0 && repositoryPath.charAt(0) != '/') {
            repositoryPath = '/' + repositoryPath;
        } else if (repositoryPath.length() == 0) {
            repositoryPath = "/";
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser parser = factory.newSAXParser();
            ContentXmlHandler handler = new ContentXmlHandler();
            parser.parse(source, handler);

            return new ResourceProxy(repositoryPath, handler.getProperties());
        } catch (SAXException e) {
            // TODO proper error handling
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            // TODO proper error handling
            throw new IOException(e);
        }
    }
}
