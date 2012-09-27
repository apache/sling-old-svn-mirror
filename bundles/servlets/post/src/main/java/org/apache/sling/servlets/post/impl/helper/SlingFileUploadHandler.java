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
package org.apache.sling.servlets.post.impl.helper;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;

/**
 * Handles file uploads.
 * <p/>
 *
 * Simple example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./portrait" />
 *   </form>
 * </xmp>
 *
 * this will create a nt:file node below "/home/admin" if the node type of
 * "admin" is (derived from) nt:folder, a nt:resource node otherwise.
 * <p/>
 *
 * Filename example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./*" />
 *   </form>
 * </xmp>
 *
 * same as above, but uses the filename of the uploaded file as name for the
 * new node.
 * <p/>
 *
 * Type hint example:
 * <xmp>
 *   <form action="/home/admin" method="POST" enctype="multipart/form-data">
 *     <input type="file" name="./portrait" />
 *     <input type="hidden" name="./portrait@TypeHint" value="my:file" />
 *   </form>
 * </xmp>
 *
 * this will create a new node with the type my:file below admin. if the hinted
 * type extends from nt:file an intermediate file node is created otherwise
 * directly a resource node.
 */
public class SlingFileUploadHandler {

    // nodetype name string constants
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";
    public static final String NT_UNSTRUCTURED = "nt:unstructured";

    // item name string constants
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_DATA = "jcr:data";

    /**
     * The servlet context.
     */
    private ServletContext servletContext;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Uses the file(s) in the request parameter for creation of new nodes.
     * if the parent node is a nt:folder a new nt:file is created. otherwise
     * just a nt:resource. if the <code>name</code> is '*', the filename of
     * the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    private void setFile(Node parent, RequestProperty prop, List<Modification> changes)
            throws RepositoryException {
    	RequestParameter[] values = prop.getValues();
    	for (RequestParameter requestParameter : values) {
        	RequestParameter value = requestParameter;

        	// ignore if a plain form field or empty
            if (value.isFormField() || value.getSize() <= 0) {
                continue;
            }

            // get node name
            String name = prop.getName();
            if (name.equals("*")) {
                name = value.getFileName();
                // strip of possible path (some browsers include the entire path)
                name = name.substring(name.lastIndexOf('/') + 1);
                name = name.substring(name.lastIndexOf('\\') + 1);
            }
            name = Text.escapeIllegalJcrChars(name);

            // check type hint. if the type is ok and extends from nt:file,
            // create an nt:file with that type. if it's invalid, drop it and let
            // the parent node type decide.
            boolean createNtFile = parent.isNodeType(NT_FOLDER);
            String typeHint = prop.getTypeHint();
            if (typeHint != null) {
                try {
                    NodeTypeManager ntMgr = parent.getSession().getWorkspace().getNodeTypeManager();
                    NodeType nt = ntMgr.getNodeType(typeHint);
                    createNtFile = nt.isNodeType(NT_FILE);
                } catch (RepositoryException e) {
                    // assuming type not valid.
                    typeHint = null;
                }
            }

            // also create an nt:file if the name contains an extension
            // the rationale is that if the file name is "important" we want
            // an nt:file, and an image name with an extension is probably "important"
            if(!createNtFile && name.indexOf('.') > 0) {
                createNtFile = true;
            }

            // set empty type
            if (typeHint == null) {
                typeHint = createNtFile ? NT_FILE : NT_RESOURCE;
            }

            // create nt:file node if needed
            Node resParent;
            if (createNtFile) {
                // create nt:file
                resParent = getOrCreateChildNode(parent, name, typeHint, changes);
                name = JCR_CONTENT;
                typeHint = NT_RESOURCE;
            } else {
            	resParent = parent;
            }

            // create resource node
            Node res = getOrCreateChildNode(resParent, name, typeHint, changes);

            // get content type
            String contentType = value.getContentType();
            if (contentType != null) {
                int idx = contentType.indexOf(';');
                if (idx > 0) {
                    contentType = contentType.substring(0, idx);
                }
            }
            if (contentType == null || contentType.equals("application/octet-stream")) {
                // try to find a better content type
                ServletContext ctx = this.servletContext;
                if (ctx != null) {
                    contentType = ctx.getMimeType(value.getFileName());
                }
                if (contentType == null || contentType.equals("application/octet-stream")) {
                    contentType = "application/octet-stream";
                }
            }

            // set properties
            changes.add(Modification.onModified(
                res.setProperty(JCR_LASTMODIFIED, Calendar.getInstance()).getPath()
            ));
            changes.add(Modification.onModified(
                res.setProperty(JCR_MIMETYPE, contentType).getPath()
            ));
            try {
                changes.add(Modification.onModified(
                    res.setProperty(JCR_DATA, value.getInputStream()).getPath()
                ));
            } catch (IOException e) {
                throw new RepositoryException("Error while retrieving inputstream from parameter value.", e);
            }
		}
    }

    /**
     * Uses the file(s) in the request parameter for creation of new nodes.
     * if the parent node is a nt:folder a new nt:file is created. otherwise
     * just a nt:resource. if the <code>name</code> is '*', the filename of
     * the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    public void setFile(Resource parent, RequestProperty prop, List<Modification> changes)
    throws RepositoryException, PersistenceException {
        final Node node = parent.adaptTo(Node.class);
        if ( node == null ) {
            // TODO
            throw new PersistenceException("Binary properties for resource '" + parent.getPath() + "' currently not supported.");
        }
        this.setFile(node, prop, changes);
    }

    private Node getOrCreateChildNode(Node parent, String name, String typeHint,
            List<Modification> changes) throws RepositoryException {
        Node result;
        if (parent.hasNode(name)) {
            Node existing = parent.getNode(name);
            if (!existing.isNodeType(typeHint)) {
                existing.remove();
                result = createWithChanges(parent, name, typeHint, changes);
            } else {
                result = existing;
            }
        } else {
            result = createWithChanges(parent, name, typeHint, changes);
        }
        return result;
    }

    private Node createWithChanges(Node parent, String name, String typeHint,
            List<Modification> changes) throws RepositoryException {
        Node result = parent.addNode(name, typeHint);
        changes.add(Modification.onCreated(result.getPath()));
        return result;
    }

}