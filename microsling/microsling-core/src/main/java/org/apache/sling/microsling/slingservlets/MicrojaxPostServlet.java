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
package org.apache.sling.microsling.slingservlets;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet that implements the microjax POST "protocol", see SLING-92 */
public class MicrojaxPostServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1837674988291697074L;
    
    private static final Logger log = LoggerFactory.getLogger(MicrojaxPostServlet.class);
    private final MicrojaxPropertyValueSetter propertyValueSetter = new MicrojaxPropertyValueSetter();
    private int createNodeCounter;

    /** Prefix for parameter names which control this POST
     *  (ujax stands for "microjax", RP_ stands for "request param")
     */
    public static final String RP_PREFIX = "ujax_";
    
    /** Optional request parameter: redirect to the specified URL after POST */ 
    public static final String RP_REDIRECT_TO =  RP_PREFIX + "redirect";
    
    /** Optional request parameter: delete the specified content paths */
    public static final String RP_DELETE_PATH = RP_PREFIX + "delete";
    
    /** Optional request parameter: only request parameters starting with this prefix are
     *  saved as Properties when creating a Node. Active only if at least one parameter 
     *  starts with this prefix, and defaults to {@link DEFAULT_SAVE_PARAM_PREFIX}.
     */
    public static final String RP_SAVE_PARAM_PREFIX = RP_PREFIX + "saveParamPrefix";
    
    /** Default value for {@link RP_SAVE_PARAM_PREFIX} */
    public static final String DEFAULT_SAVE_PARAM_PREFIX = "./";
    
    /** Optional request parameter: if value is 0, created node is ordered so as
     *  to be the first child of its parent.
     */ 
    public static final String RP_ORDER = RP_PREFIX + "order";
    
    /** Code value for RP_ORDER */ 
    public static final String ORDER_ZERO = "0";
    
    /** Optional request parameter: if provided, added at the end of the computed
     *  (or supplied) redirect URL
     */
    public static final String RP_DISPLAY_EXTENSION = RP_PREFIX + "displayExtension";
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Session s = null;
        try {
            
            // select the Resource to process
            Resource currentResource = request.getResource();
            Node currentNode = (Node)currentResource.getRawData();
            
            // need a Node, path and Session
            String currentPath = null;
            if(currentNode != null) {
                currentPath = currentNode.getPath();
                s = currentNode.getSession();
            } else {
                currentPath = SlingRequestPaths.getPathInfo(request);
                // TODO not very convenient way to get a Session...
                final Resource root = request.getResourceResolver().getResource("/");
                final Node rootNode = (Node)root.getRawData();
                if(rootNode == null) {
                    throw new HttpStatusCodeException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Root Node not found");
                }
                s = rootNode.getSession();
            }
            
            final String [] pathsToDelete = request.getParameterValues(RP_DELETE_PATH);
            if(pathsToDelete!=null) {
                // process deletes if any, and if so don't do anything else
                deleteNodes(s, pathsToDelete, currentPath, response);
            } else {
                // if no deletes, create or update nodes
                createOrUpdateNodesFromRequest(request, response, s);
            }
            
        } catch(RepositoryException re) {
            throw new HttpStatusCodeException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,re.toString(),re);
            
        } finally {
            try {
                if (s != null && s.hasPendingChanges()) {
                    s.refresh(false);
                }
            } catch(RepositoryException re) {
                log.warn("RepositoryException in finally block: "+ re.getMessage(),re);
            }
        }
    }
    
    /** Delete specified nodes, and send response */
    protected void deleteNodes(Session s, String [] pathsToDelete, String currentPath, SlingHttpServletResponse response) 
    throws RepositoryException, IOException {
        processDeletes(s, pathsToDelete, currentPath);
        s.save();
        response.setContentType(getServletContext().getMimeType("dummy.txt"));
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        pw.println("Nodes have been deleted(if they existed):");
        for(String path : pathsToDelete) {
            pw.println(path);
        }
        pw.flush();
    }
    
    /** Create or update node(s) according to current request , and send response */ 
    protected void createOrUpdateNodesFromRequest(SlingHttpServletRequest request, SlingHttpServletResponse response, Session s) 
    throws RepositoryException, IOException {
        
        // Where to redirect to when done
        String redirectPath = request.getHeader("Referer");
        
        // find out the actual "save prefix" to use - only parameters starting with
        // this prefix are saved as Properties, when creating nodes, see setPropertiesFromRequest()
        final String savePrefix = getSavePrefix(request);
        
        // use the request path (disregarding resource resolution)
        // but remove any extension or selectors
        String currentPath = SlingRequestPaths.getPathInfo(request);
        Node currentNode = null;
        final int dotPos = currentPath.indexOf('.');
        if(dotPos >= 0) {
            currentPath = currentPath.substring(0,dotPos);
        }
        
        final String starSuffix = "/*";
        if(currentPath.endsWith(starSuffix)) {
            // If the path ends with a *, create a node under its parent, with
            // a generated node name
            currentPath = currentPath.substring(0, currentPath.length() - starSuffix.length());
            currentPath += "/" + (createNodeCounter++) + System.currentTimeMillis();
            
        } else if(s.itemExists(currentPath)) {
            // update to an existing node
            final Item item = s.getItem(currentPath);
            if(item.isNode()) {
                currentNode = (Node)item;
            } else {
                throw new HttpStatusCodeException(HttpServletResponse.SC_CONFLICT,"Item at path " + currentPath + " is not a Node");
            }
            
        } else {
            // request to create a new node at a specific path - use the supplied path as is
        }
        
        Set<Node> createdNodes = new HashSet<Node>();
        if(currentNode == null) {
            currentNode = deepCreateNode(s, currentPath, createdNodes);
        }
        currentPath = currentNode.getPath();
        
        // process the "order" command if any
        final String order = request.getParameter(RP_ORDER);
        if(order!=null) {
            processNodeOrder(currentNode,order);
        }
        
        // walk the request parameters, create and save nodes and properties
        setPropertiesFromRequest(currentNode, request, savePrefix, createdNodes);
        
        // sava data and find out where to redirect
        s.save();
        final String forcedRedirect = request.getParameter(RP_REDIRECT_TO);
        final String redirectExtension = request.getParameter(RP_DISPLAY_EXTENSION);
        if(forcedRedirect != null) {
            redirectPath = forcedRedirect;
        } else if(currentNode != null) {
            redirectPath = currentNode.getPath();
        }
        if(redirectExtension!=null) {
            redirectPath += redirectExtension;
        }
        
        final String redirectUrl = 
            SlingRequestPaths.getContextPath(request)
            + SlingRequestPaths.getServletPath(request)
            + redirectPath; 
        if(log.isDebugEnabled()) {
            log.debug("Redirecting to " + redirectUrl);
        }
        response.sendRedirect(redirectUrl);
    }

    /** Set Node properties from current request 
     *  TODO should handle file uploads as well
     */
    private void setPropertiesFromRequest(Node n, SlingHttpServletRequest request, 
            String savePrefix, Set<Node> createdNodes)
            throws RepositoryException {
        
        for(Map.Entry<String, RequestParameter[]>  e : request.getRequestParameterMap().entrySet()) {
            String name = e.getKey();
            if(savePrefix!=null) {
                if(!name.startsWith(savePrefix)) continue;
                name = name.substring(savePrefix.length());
            }
            setProperty(n,request,name,e.getValue(),createdNodes);
        }
    }
    
    /** Set a single Property on node N 
     * @throws RepositoryException */
    private void setProperty(Node n, SlingHttpServletRequest request, String name, 
            RequestParameter[] values, Set<Node> createdNodes) throws RepositoryException {
        
        // split the relative path identifying the property to be saved
        String proppath = name;

        // @ValueFrom can be used to define mappings between form fields and JCR properties
// TODO        
//        final int vfIndex = name.indexOf("@ValueFrom"); 
//        if (vfIndex >= 0) {
//            // Indirect
//            proppath = name.substring(0, vfIndex);
//        } else if (name.indexOf("@") >= 0) {
//            // skip "Hints"
//            return;
//        }

        final String path = n.getPath();
        String parentpath = "";
        String propname=name;

        if (propname.indexOf("/")>=0) {
            parentpath=proppath.substring(0, name.lastIndexOf("/"));
            propname = proppath.substring(name.lastIndexOf("/") + 1);
        }

        // if the whole thing ended in a slash -> skip
        if (propname.equals("")) {
            return;
        }

        // get or create the parent node
        final Session s = n.getSession();
        Node parent;
        if(name.startsWith("/")) {
            parent = deepCreateNode(s, parentpath, createdNodes);
            
        } else if (!parentpath.equals("")) {
            parent = (Node) s.getItem(path + "/" + parentpath);
        } else {
            parent = (Node) s.getItem(path);
        }
        
        // TODO String typehint = request.getParameter(proppath + "@TypeHint");
        final String typeHint = null;
        final boolean nodeIsNew = createdNodes.contains(parent); 
        propertyValueSetter.setProperty(parent, propname, values, typeHint, nodeIsNew);
}

    /**
     * Deep creates a node, parent-padding with nt:unstructured nodes
     *
     * @param path absolute path to node that needs to be deep-created
     */
    private Node deepCreateNode(Session s, String path, Set<Node> createdNodes)
            throws RepositoryException {
        if(log.isDebugEnabled()) {
            log.debug("Deep-creating Node '" + path + "'");
        }
        
        String[] pathelems = path.substring(1).split("/");
        int i = 0;
        String mypath = "";
        Node parent = s.getRootNode();
        while (i < pathelems.length) {
            String name = pathelems[i];
            mypath += "/" + name;
            if (!s.itemExists(mypath)) {
                createdNodes.add(parent.addNode(name));
            }
            parent = (Node) s.getItem(mypath);
            i++;
        }
        return (parent);
    }

    /** Delete Items at the provided paths 
     *  @param pathsToDelete each path that does not start with / is
     *      prepended with currentPath 
     */ 
    private void processDeletes(Session s, String [] pathsToDelete, String currentPath) 
    throws RepositoryException {
        for(String path : pathsToDelete) {
            if(!path.startsWith("/")) {
                path = currentPath + "/" + path;
            }
            if(s.itemExists(path)) {
                s.getItem(path).remove();
                if(log.isDebugEnabled()) {
                    log.debug("Deleted item " + path);
                }
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Item '" + path + "' not found for deletion, ignored");
                }
            }
        }
    }
    
    /** Return the "save prefix" to use, null if none */
    private String getSavePrefix(SlingHttpServletRequest request) {
        String prefix = request.getParameter(RP_SAVE_PARAM_PREFIX);
        if(prefix==null) {
            prefix = DEFAULT_SAVE_PARAM_PREFIX;
        }
        
        // if no parameters start with this prefix, it is not used
        String result = null;
        for(String name : request.getRequestParameterMap().keySet()) {
            if(name.startsWith(prefix)) {
                result = prefix;
                break;
            }
        }
        
        return result;
    }
    
    /** If orderCode is ORDER_ZERO, move n so that it is the first
     *  child of its parent 
     * @throws RepositoryException */
    private void processNodeOrder(Node n, String orderCode) throws RepositoryException {
        if(ORDER_ZERO.equals(orderCode)) {
            final String path = n.getPath();
            final Node parent=(Node) n.getSession().getItem(path.substring(0,path.lastIndexOf('/')));
            final String myname=path.substring(path.lastIndexOf('/')+1);
            final String beforename=parent.getNodes().nextNode().getName();
            parent.orderBefore(myname, beforename);
            
            if(log.isDebugEnabled()) {
                log.debug("Node " + n.getPath() + " moved to be first child of its parent, due to orderCode=" + orderCode);
            }
            
        } else {
            if(log.isDebugEnabled()) {
                log.debug("orderCode '" + orderCode + "' invalid, ignored");
            }
        }
    }
}
