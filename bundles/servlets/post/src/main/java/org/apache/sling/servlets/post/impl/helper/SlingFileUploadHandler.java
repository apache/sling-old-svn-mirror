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
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
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
    private void setFile(final Resource parentResource, final Node parent, final RequestProperty prop, InputStream stream, final List<Modification> changes, String name, final String contentType)
            throws RepositoryException, PersistenceException {
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
        Resource resParent;
        if (createNtFile) {
            // create nt:file
            resParent = getOrCreateChildResource(parentResource, name, typeHint, changes);
            name = JCR_CONTENT;
            typeHint = NT_RESOURCE;
        } else {
            resParent = parentResource;
        }

        // create resource node
        Resource newResource = getOrCreateChildResource(resParent, name, typeHint, changes);
        Node res = newResource.adaptTo(Node.class);

        // set properties
        changes.add(Modification.onModified(
                res.setProperty(JCR_LASTMODIFIED, Calendar.getInstance()).getPath()
                ));
        changes.add(Modification.onModified(
                res.setProperty(JCR_MIMETYPE, contentType).getPath()
                ));
        changes.add(Modification.onModified(
                res.setProperty(JCR_DATA, stream).getPath()
                ));
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
    private void setFile(final Resource parentResource, final RequestProperty prop, final InputStream stream, final List<Modification> changes, String name, final String contentType)
            throws PersistenceException, RepositoryException {
        String typeHint = prop.getTypeHint();
        if ( typeHint == null ) {
            typeHint = NT_FILE;
        }
        // create properties
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("sling:resourceType", typeHint);
        props.put(JCR_LASTMODIFIED, Calendar.getInstance());
        props.put(JCR_MIMETYPE, contentType);
        props.put(JCR_DATA, stream);

        // get or create resource
        Resource result = parentResource.getChild(name);
        if ( result != null ) {
            final ModifiableValueMap vm = result.adaptTo(ModifiableValueMap.class);
            if ( vm == null ) {
                throw new PersistenceException("Resource at " + parentResource.getPath() + '/' + name + " is not modifiable.");
            }
            vm.putAll(props);
        } else {
            result = parentResource.getResourceResolver().create(parentResource, name, props);
        }
        for(final String key : props.keySet()) {
            changes.add(Modification.onModified(result.getPath() + '/' + key));
        }
    }

    private static final String MT_APP_OCTET = "application/octet-stream";

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
    public void setFile(final Resource parent, final RequestProperty prop, final List<Modification> changes)
            throws RepositoryException, PersistenceException {
        for (final RequestParameter value : prop.getValues()) {

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

            // get content type
            String contentType = value.getContentType();
            if (contentType != null) {
                int idx = contentType.indexOf(';');
                if (idx > 0) {
                    contentType = contentType.substring(0, idx);
                }
            }
            if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                // try to find a better content type
                ServletContext ctx = this.servletContext;
                if (ctx != null) {
                    contentType = ctx.getMimeType(value.getFileName());
                }
                if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                    contentType = MT_APP_OCTET;
                }
            }
            final Node node = parent.adaptTo(Node.class);
            try {
                if (node == null) {
                    this.setFile(parent, prop, value.getInputStream(), changes, name, contentType);
                } else {
                    this.setFile(parent, node, prop, value.getInputStream(), changes, name, contentType);
                }
            } catch (final IOException e) {
                throw new PersistenceException("Error while retrieving inputstream from parameter value.", e);
            }
        }

    }

    /**
     * Uses the file(s) from merged chunks for creation of new nodes. if the parent node
     *  is a nt:folder a new nt:file is created. otherwise just a nt:resource. if the <code>name</code>
     *  is '*', the filename of the uploaded file is used.
     *
     * @param parent the parent node
     * @param prop the assembled property info
     * @throws RepositoryException if an error occurs
     */
    public void setFile(Resource parent, RequestProperty prop, InputStream inputstream, List<Modification> changes)
            throws RepositoryException, PersistenceException {
        for (final RequestParameter value : prop.getValues()) {

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

            // get content type
            String contentType = value.getContentType();
            if (contentType != null) {
                int idx = contentType.indexOf(';');
                if (idx > 0) {
                    contentType = contentType.substring(0, idx);
                }
            }
            if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                // try to find a better content type
                ServletContext ctx = this.servletContext;
                if (ctx != null) {
                    contentType = ctx.getMimeType(value.getFileName());
                }
                if (contentType == null || contentType.equals(MT_APP_OCTET)) {
                    contentType = MT_APP_OCTET;
                }
            }
            final Node node = parent.adaptTo(Node.class);
            try {
                if (node == null) {
                    this.setFile(parent, prop, inputstream, changes, name, contentType);
                } else {
                    this.setFile(parent, node, prop, inputstream, changes, name, contentType);
                }
            } catch (final IOException e) {
                throw new PersistenceException("Error while retrieving inputstream from parameter value.", e);
            }
        }
    }

    private Resource getOrCreateChildResource(final Resource parent, final String name,
            final String typeHint,
            final List<Modification> changes)
                    throws PersistenceException, RepositoryException {
        Resource result = parent.getChild(name);
        if ( result != null ) {
            final Node existing = result.adaptTo(Node.class);
            if ( existing != null && !existing.isNodeType(typeHint)) {
                existing.remove();
                result = createWithChanges(parent, name, typeHint, changes);
            }
        } else {
            result = createWithChanges(parent, name, typeHint, changes);
        }
        return result;
    }

    private Resource createWithChanges(final Resource parent, final String name,
            final String typeHint,
            final List<Modification> changes)
                    throws PersistenceException {
        Map<String, Object> properties = null;
        if ( typeHint != null ) {
            properties = new HashMap<String, Object>();
            if ( parent.adaptTo(Node.class) != null ) {
                properties.put("jcr:primaryType", typeHint);
            } else {
                properties.put("sling:resourceType", typeHint);
            }
        }
        final Resource result = parent.getResourceResolver().create(parent, name, properties);
        changes.add(Modification.onCreated(result.getPath()));
        return result;
    }
}