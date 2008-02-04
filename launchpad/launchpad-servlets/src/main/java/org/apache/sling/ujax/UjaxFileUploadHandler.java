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
package org.apache.sling.ujax;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.mime.MimeTypeService;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

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
 * 
 * @version $Rev$, $Date$
 */
public class UjaxFileUploadHandler {

    /**
     * The CVS/SVN id
     */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    // nodetype name string constants
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";

    // item name string constants
    public static final String JCR_CONTENT = "jcr:content";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_DATA = "jcr:data";

    /**
     * the post processor
     */
    private final UjaxPostProcessor ctx;

    /**
     * Constructs file upload handler
     * @param ctx the post processor
     */
    public UjaxFileUploadHandler(UjaxPostProcessor ctx) {
        this.ctx = ctx;
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
    void setFile(Node parent, RequestProperty prop)
            throws RepositoryException {
        RequestParameter value = prop.getValues()[0];
        assert !value.isFormField();

        // ignore if empty
        if (value.getSize() <= 0) {
            return;
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

        // set empty type
        if (typeHint == null) {
            typeHint = createNtFile ? NT_FILE : NT_RESOURCE;
        }

        // remove node
        if (parent.hasNode(name)) {
            parent.getNode(name).remove();
        }

        // create nt:file node if needed
        if (createNtFile) {
            // create nt:file
            parent = parent.addNode(name, typeHint);
            ctx.getChangeLog().onCreated(parent.getPath());
            name = JCR_CONTENT;
            typeHint = NT_RESOURCE;
        }
        
        // create resource node
        Node res = parent.addNode(name, typeHint);
        ctx.getChangeLog().onCreated(res.getPath());

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
            MimeTypeService svc = ctx.getRequest().getServiceLocator().getService(MimeTypeService.class);
            if (svc != null) {
                contentType = svc.getMimeType(value.getFileName());
            }
            if (contentType == null || contentType.equals("application/octet-stream")) {
                contentType = "application/octet-stream";
            }
        }

        // set properties
        ctx.getChangeLog().onModified(
            res.setProperty(JCR_LASTMODIFIED, Calendar.getInstance()).getPath()
        );
        ctx.getChangeLog().onModified(
            res.setProperty(JCR_MIMETYPE, contentType).getPath()
        );
        try {
            ctx.getChangeLog().onModified(
                res.setProperty(JCR_DATA, value.getInputStream()).getPath()
            );
        } catch (IOException e) {
            throw new RepositoryException("Error while retrieving inputstream from parameter value.", e);
        }
    }
}