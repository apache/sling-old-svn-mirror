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

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
import static org.apache.sling.jcr.contentloader.ContentTypeUtil.EXT_JCR_XML;

public class JcrXmlImporter {

    private final Logger logger = LoggerFactory.getLogger(JcrXmlImporter.class);

    public JcrXmlImporter() {
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
    protected Node importJcrXml(Node parent, String name, InputStream contentStream, boolean replace) throws IOException {
        try {
            final String nodeName = (name.endsWith(EXT_JCR_XML)) ? name.substring(0, name.length() - EXT_JCR_XML.length()) : name;

            // ensure the name is not empty
            if (nodeName.length() == 0) {
                throw new IOException("Node name must not be empty (or extension only)");
            }

            // check for existence/replacement
            if (parent.hasNode(nodeName)) {
                Node existingNode = parent.getNode(nodeName);
                if (replace) {
                    logger.debug("importJcrXml: Removing existing node at {}", nodeName);
                    existingNode.remove();
                } else {
                    logger.debug("importJcrXml: Node {} for XML already exists, nothing to to", nodeName);
                    return existingNode;
                }
            }

            final int uuidBehavior;
            if (replace) {
                uuidBehavior = IMPORT_UUID_COLLISION_REPLACE_EXISTING;
            } else {
                uuidBehavior = IMPORT_UUID_CREATE_NEW;
            }

            Session session = parent.getSession();
            session.importXML(parent.getPath(), contentStream, uuidBehavior);

            // additionally check whether the expected child node exists
            return (parent.hasNode(nodeName)) ? parent.getNode(nodeName) : null;
        } catch (InvalidSerializedDataException isde) {
            // the xml might not be System or Document View export, fall back to old-style XML reading
            logger.info("importJcrXml: XML does not seem to be system or document view; cause: {}", isde.toString());
            return null;
        } catch (RepositoryException re) {
            // any other repository related issue...
            logger.info("importJcrXml: Repository issue loading XML; cause: {}", re.toString());
            return null;
        }
    }

}
