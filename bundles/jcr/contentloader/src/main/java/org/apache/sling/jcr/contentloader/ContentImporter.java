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
package org.apache.sling.jcr.contentloader;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import aQute.bnd.annotation.ProviderType;


/**
 * The <code>ContentImporter</code> service
 * <p>
 * This interface is not intended to be implemented by bundles. It is
 * implemented by this bundle and may be used by client bundles.
 * </p>
 */
@ProviderType
public interface ContentImporter {

    /**
     * Import content into the repository by parsing the provided content stream.
     *
     * @param parent         the root node for the imported content
     * @param filename       the name of the imported content. Becomes the node name (without extension). The file extension determines the content type.
     * @param contentStream  the content stream to be imported
     * @param importOptions  (optional) additional options to control the import
     * @param importListener (optional) listener to receive callbacks for each change in the import
     * @throws RepositoryException
     * @throws IOException
     */
    void importContent(Node parent, String filename, InputStream contentStream, ImportOptions importOptions, ContentImportListener importListener) throws RepositoryException, IOException;

    /**
     * Import content into the repository by parsing the provided content stream.
     *
     * @param parent         the root node for the imported content
     * @param name           the name of the imported content. Becomes the node name. If null, imports in PARENT_NODE import mode.
     * @param contentType    the content type of the content stream
     * @param contentStream  the content stream to be imported
     * @param importOptions  (optional) additional options to control the import
     * @param importListener (optional) listener to receive callbacks for each change in the import
     * @throws RepositoryException
     * @throws IOException
     */
    void importContent(Node parent, String name, String contentType, InputStream contentStream, ImportOptions importOptions, ContentImportListener importListener) throws RepositoryException, IOException;

}
