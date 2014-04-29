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
package org.apache.sling.jcr.contentloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

/**
 * The <code>DefaultContentImporter</code> is the default implementation of the ContentImporter service providing the following functionality:
 * <ul>
 * <li>Import content into the content repository.
 * </ul>
 */
@Component(
    metatype = true,
    immediate = false,
    label = "%content.import.service.name",
    description = "%content.import.service.description"
)
@Properties({
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = "service.description", value = "Apache Sling JCR Content Import Service")
})
@Service(ContentImporter.class)
public class DefaultContentImporter extends BaseImportLoader implements JcrContentHelper, ContentImporter {

    private final Logger log = LoggerFactory.getLogger(DefaultContentImporter.class);

    /**
     * The MimeTypeService used by the initial content initialContentLoader to resolve MIME types for files to be installed.
     */
    @Reference
    private MimeTypeService mimeTypeService;

    /* (non-Javadoc)
     * @see org.apache.sling.jcr.contentloader.ContentImporter#importContent(javax.jcr.Node, java.lang.String, java.io.InputStream, org.apache.sling.jcr.contentloader.ImportOptions, org.apache.sling.jcr.contentloader.ContentImportListener)
	 */
    public void importContent(Node parent, String name, InputStream contentStream, ImportOptions importOptions, ContentImportListener importListener) throws RepositoryException, IOException {

        // special treatment for system view imports
        if (name.endsWith(EXT_JCR_XML)) {
            boolean replace = (importOptions == null) ? false : importOptions.isOverwrite();
            Node node = importSystemView(parent, name, contentStream, replace);
            if (node != null) {
                if (importListener != null) {
                    importListener.onCreate(node.getPath());
                }
                return;
            }
        }

        DefaultContentCreator contentCreator = new DefaultContentCreator(this);
        List<String> createdPaths = new ArrayList<String>();
        contentCreator.init(importOptions, this.defaultImportProviders, createdPaths, importListener);

        contentCreator.prepareParsing(parent, toPlainName(contentCreator, name));

        final ImportProvider ip = contentCreator.getImportProvider(name);
        ContentReader reader = ip.getReader();
        reader.parse(contentStream, contentCreator);

        // save changes
        Session session = parent.getSession();
        session.save();

        // finally checkin versionable nodes
        for (final Node versionable : contentCreator.getVersionables()) {
            versionable.checkin();
            if (importListener != null) {
                importListener.onCheckin(versionable.getPath());
            }
        }
    }

    private String toPlainName(DefaultContentCreator contentCreator, String name) {
        final String providerExt = contentCreator.getImportProviderExtension(name);
        if (providerExt != null) {
            if (name.length() == providerExt.length()) {
                return null; // no name is provided
            }
            return name.substring(0, name.length() - providerExt.length());
        }
        return name;
    }

    /**
     * Import the XML file as JCR system or document view import. If the XML
     * file is not a valid system or document view export/import file,
     * <code>false</code> is returned.
     *
     * @param parent        The parent node below which to import
     * @param name          the name of the import resource
     * @param contentStream The XML content to import
     * @param replace       Whether or not to replace the subtree at name if the
     *                      node exists.
     * @return <code>true</code> if the import succeeds, <code>false</code> if the import fails due to XML format errors.
     * @throws IOException If an IO error occurrs reading the XML file.
     */
    private Node importSystemView(Node parent, String name, InputStream contentStream, boolean replace) throws IOException {
        InputStream ins = null;
        try {
            // check whether we have the content already, nothing to do then
            final String nodeName = (name.endsWith(EXT_JCR_XML)) ? name.substring(0, name.length() - EXT_JCR_XML.length()) : name;

            // ensure the name is not empty
            if (nodeName.length() == 0) {
                throw new IOException("Node name must not be empty (or extension only)");
            }

            // check for existence/replacement
            if (parent.hasNode(nodeName)) {
                Node existingNode = parent.getNode(nodeName);
                if (replace) {
                    log.debug("importSystemView: Removing existing node at {}", nodeName);
                    existingNode.remove();
                } else {
                    log.debug("importSystemView: Node {} for XML already exists, nothing to to", nodeName);
                    return existingNode;
                }
            }

            final int uuidBehavior;
            if (replace) {
                uuidBehavior = IMPORT_UUID_COLLISION_REPLACE_EXISTING;
            } else {
                uuidBehavior = IMPORT_UUID_CREATE_NEW;
            }

            ins = contentStream;
            Session session = parent.getSession();
            session.importXML(parent.getPath(), ins, uuidBehavior);

            // additionally check whether the expected child node exists
            return (parent.hasNode(nodeName)) ? parent.getNode(nodeName) : null;
        } catch (InvalidSerializedDataException isde) {
            // the xml might not be System or Document View export, fall back to old-style XML reading
            log.info("importSystemView: XML does not seem to be system view export, trying old style; cause: {}", isde.toString());
            return null;
        } catch (RepositoryException re) {
            // any other repository related issue...
            log.info("importSystemView: Repository issue loading XML, trying old style; cause: {}", re.toString());
            return null;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    // ---------- JcrContentHelper implementation ---------------------------------------------

    /* (non-Javadoc)
     * @see org.apache.sling.jcr.contentloader.internal.JcrContentHelper#getMimeType(java.lang.String)
     */
    public String getMimeType(String name) {
        // local copy to not get NPE despite check for null due to concurrent unbind
        MimeTypeService mts = mimeTypeService;
        return (mts != null) ? mts.getMimeType(name) : null;
    }

}
