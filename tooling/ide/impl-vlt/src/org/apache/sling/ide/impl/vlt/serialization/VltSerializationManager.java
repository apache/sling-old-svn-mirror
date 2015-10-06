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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.vault.fs.Mounter;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.SerializationType;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.impl.io.XmlAnalyzer;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.ResourceProxy;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VltSerializationManager implements SerializationManager {

    static final String EXTENSION_XML = ".xml";

    private VltSerializationDataBuilder builder;

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
        
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            SerializationType serType = XmlAnalyzer.analyze(new InputSource(in));
            return serType == SerializationType.XML_DOCVIEW;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseResourcePath(String serializationFilePath) {

        File file = new File(serializationFilePath);
        String fileName = file.getName();
        if (fileName.equals(Constants.DOT_CONTENT_XML)) {
            return file.getParent();
        }

        if (!fileName.endsWith(EXTENSION_XML)) {
            return file.getAbsolutePath();
        }

        // assume that delete file with the xml extension is a full serialization aggregate
        // TODO - this can generate false results
        if (!file.exists()) {
            return getPathWithoutXmlExtension(file);
        }

        // TODO - refrain from doing I/O here
        // TODO - copied from TransactionImpl
        
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            SerializationType serType = XmlAnalyzer.analyze(new InputSource(in));
            if (serType == SerializationType.XML_DOCVIEW) {
                return getPathWithoutXmlExtension(file);
            }

            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getPathWithoutXmlExtension(File file) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - EXTENSION_XML.length());
    }

    @Override
    public String getSerializationFilePath(String baseFilePath, SerializationKind serializationKind) {

        switch (serializationKind) {
            case FOLDER:
            case METADATA_PARTIAL:
                return baseFilePath + File.separatorChar + Constants.DOT_CONTENT_XML;
            case METADATA_FULL:
                return baseFilePath;
            case FILE:
                return baseFilePath + ".dir" + File.separatorChar + Constants.DOT_CONTENT_XML;
        }

        throw new IllegalArgumentException("Unsupported serialization kind " + serializationKind);
    }

    @Override
    public String getRepositoryPath(String osPath) {

        String repositoryPath;
        String name = Text.getName(osPath);
        if (name.equals(Constants.DOT_CONTENT_XML)) {
            // TODO - this is a bit risky, we might clean legitimate directories which contain '.dir'
            String parentPath = Text.getRelativeParent(osPath, 1);
            if (parentPath != null && parentPath.endsWith(".dir")) {
                parentPath = parentPath.substring(0, parentPath.length() - ".dir".length());
            }
            repositoryPath = PlatformNameFormat.getRepositoryPath(parentPath);
        } else {
            // TODO - we assume here that it's a full coverage aggregate but it might not be
            if (osPath.endsWith(EXTENSION_XML)) {
                repositoryPath = PlatformNameFormat.getRepositoryPath(osPath.substring(0, osPath.length()
                        - EXTENSION_XML.length()));
            } else {
                repositoryPath = PlatformNameFormat.getRepositoryPath(osPath).replace(".dir/", "/");
            }
        }

        // TODO extract into PathUtils
        if (repositoryPath.length() > 0 && repositoryPath.charAt(0) != '/') {
            repositoryPath = '/' + repositoryPath;
        } else if (repositoryPath.length() == 0) {
            repositoryPath = "/";
        }

        return repositoryPath;
    }

    @Override
    public String getOsPath(String repositoryPath) {
        return PlatformNameFormat.getPlatformPath(repositoryPath);
    }

    protected void bindVaultFsLocator(VaultFsLocator fsLocator) {

        getBuilder().setLocator(fsLocator);
    }

    protected void unbindVaultFsLocator(VaultFsLocator fsLocator) {

        getBuilder().setLocator(null);
    }
    
    private VltSerializationDataBuilder getBuilder() {
    	if (builder==null) {
    		builder = new VltSerializationDataBuilder();
    	}
    	return builder;
    }
    
    @Override
    public SerializationDataBuilder newBuilder(
    		org.apache.sling.ide.transport.Repository repository,
    		File contentSyncRoot) throws SerializationException {
    	VltSerializationDataBuilder b = getBuilder();
    	b.init(repository, contentSyncRoot);
    	return b;
    }

    @Override
    public ResourceProxy readSerializationData(String filePath, InputStream source) throws IOException {

        if (source == null)
            return null;

        String repositoryPath = getRepositoryPath(filePath);

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser parser = factory.newSAXParser();
            ContentXmlHandler handler = new ContentXmlHandler(repositoryPath);
            parser.parse(source, handler);

            return handler.getRoot();
        } catch (SAXException | ParserConfigurationException e) {
            // TODO proper error handling
            throw new IOException(e);
        }
    }
}
