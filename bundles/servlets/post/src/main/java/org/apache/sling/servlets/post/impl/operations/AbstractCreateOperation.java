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

package org.apache.sling.servlets.post.impl.operations;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;

abstract class AbstractCreateOperation extends AbstractSlingPostOperation {
    /**
     * The default node name generator
     */
    private final NodeNameGenerator defaultNodeNameGenerator;

    /**
     * utility class for generating node names
     */
    private NodeNameGenerator[] extraNodeNameGenerators;

    public AbstractCreateOperation(NodeNameGenerator defaultNodeNameGenerator) {
		this.defaultNodeNameGenerator = defaultNodeNameGenerator;
	}

	public void setExtraNodeNameGenerators(NodeNameGenerator[] extraNodeNameGenerators) {
        this.extraNodeNameGenerators = extraNodeNameGenerators;
    }

    /**
     * Create node(s) according to current request
     *
     * @throws RepositoryException if a repository error occurs
     */
    protected void processCreate(Session session,
            Map<String, RequestProperty> reqProperties, HtmlResponse response, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws RepositoryException {

        String path = response.getPath();

        if (!session.itemExists(path)) {

            deepGetOrCreateNode(session, path, reqProperties, changes, versioningConfiguration);
            response.setCreateRequest(true);

        } else {

            updateMixins(session, path, reqProperties, changes, versioningConfiguration);
        }
    }

    protected void updateMixins(Session session, String path, Map<String, RequestProperty> reqProperties,
            List<Modification> changes, VersioningConfiguration versioningConfiguration) throws PathNotFoundException,
            RepositoryException, NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException {
        String[] mixins = getMixinTypes(reqProperties, path);
        if (mixins != null) {

            Item item = session.getItem(path);
            if (item.isNode()) {

                Set<String> newMixins = new HashSet<String>();
                newMixins.addAll(Arrays.asList(mixins));

                // clear existing mixins first
                Node node = (Node) item;
                checkoutIfNecessary(node, changes, versioningConfiguration);

                for (NodeType mixin : node.getMixinNodeTypes()) {
                    String mixinName = mixin.getName();
                    if (!newMixins.remove(mixinName)) {
                        node.removeMixin(mixinName);
                    }
                }

                // add new mixins
                for (String mixin : newMixins) {
                    node.addMixin(mixin);
                    // this is a bit of a cheat; there isn't a formal checkout, but assigning
                    // the mix:versionable mixin does an implicit checkout
                    if (mixin.equals("mix:versionable") &&
                            versioningConfiguration.isCheckinOnNewVersionableNode()) {
                        changes.add(Modification.onCheckout(path));
                    }
                }
            }
        }
    }
    
    /**
     * Collects the properties that form the content to be written back to the
     * repository.
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    protected Map<String, RequestProperty> collectContent(
            SlingHttpServletRequest request, HtmlResponse response) {

        boolean requireItemPrefix = requireItemPathPrefix(request);

        // walk the request parameters and collect the properties
        LinkedHashMap<String, RequestProperty> reqProperties = new LinkedHashMap<String, RequestProperty>();
        for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with sling:post
            if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
                continue;
            }
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }

            // ensure the paramName is an absolute property name
            String propPath = toPropertyPath(paramName, response);

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.TYPE_HINT_SUFFIX);

                final RequestParameter[] rp = e.getValue();
                if (rp.length > 0) {
                    prop.setTypeHintValue(rp[0].getString());
                }

                continue;
            }

            // @DefaultValue
            if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.DEFAULT_VALUE_SUFFIX);

                prop.setDefaultValues(e.getValue());

                continue;
            }

            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            // @ValueFrom example:
            // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
            // causes the JCR Text property to be set to the value of the
            // fulltext form field.
            if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.VALUE_FROM_SUFFIX);

                // @ValueFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String refName = e.getValue()[0].getString();
                    RequestParameter[] refValues = request.getRequestParameters(refName);
                    if (refValues != null) {
                        prop.setValues(refValues);
                    }
                }

                continue;
            }

            // SLING-458: Allow Removal of properties prior to update
            // @Delete example:
            // <input name="./Text@Delete" type="hidden" />
            // causes the JCR Text property to be deleted before update
            if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath, SlingPostConstants.SUFFIX_DELETE);

                prop.setDelete(true);

                continue;
            }

            // SLING-455: @MoveFrom means moving content to another location
            // @MoveFrom example:
            // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by moving the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_MOVE_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String sourcePath = e.getValue()[0].getString();
                    prop.setRepositorySource(sourcePath, true);
                }

                continue;
            }

            // SLING-455: @CopyFrom means moving content to another location
            // @CopyFrom example:
            // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_COPY_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String sourcePath = e.getValue()[0].getString();
                    prop.setRepositorySource(sourcePath, false);
                }

                continue;
            }

            // SLING-1412: @IgnoreBlanks
            // @Ignore example:
            // <input name="./Text" type="hidden" value="test" />
            // <input name="./Text" type="hidden" value="" />
            // <input name="./Text@String[]" type="hidden" value="true" />
            // <input name="./Text@IgnoreBlanks" type="hidden" value="true" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_IGNORE_BLANKS)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_IGNORE_BLANKS);

                if (e.getValue().length == 1) {
                    prop.setIgnoreBlanks(true);
                }

                continue;
            }

            if (propPath.endsWith(SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING);

                if (e.getValue().length == 1) {
                    prop.setUseDefaultWhenMissing(true);
                }

                continue;
            }

            // plain property, create from values
            RequestProperty prop = getOrCreateRequestProperty(reqProperties,
                propPath, null);
            prop.setValues(e.getValue());
        }

        return reqProperties;
    }

    /**
     * Returns the <code>paramName</code> as an absolute (unnormalized)
     * property path by prepending the response path (<code>response.getPath</code>)
     * to the parameter name if not already absolute.
     */
    private String toPropertyPath(String paramName, HtmlResponse response) {
        if (!paramName.startsWith("/")) {
            paramName = ResourceUtil.normalize(response.getPath() + '/' + paramName);
        }

        return paramName;
    }

    /**
     * Returns the request property for the given property path. If such a
     * request property does not exist yet it is created and stored in the
     * <code>props</code>.
     *
     * @param props The map of already seen request properties.
     * @param paramName The absolute path of the property including the
     *            <code>suffix</code> to be looked up.
     * @param suffix The (optional) suffix to remove from the
     *            <code>paramName</code> before looking it up.
     * @return The {@link RequestProperty} for the <code>paramName</code>.
     */
    private RequestProperty getOrCreateRequestProperty(
            Map<String, RequestProperty> props, String paramName, String suffix) {
        if (suffix != null && paramName.endsWith(suffix)) {
            paramName = paramName.substring(0, paramName.length()
                - suffix.length());
        }

        RequestProperty prop = props.get(paramName);
        if (prop == null) {
            prop = new RequestProperty(paramName);
            props.put(paramName, prop);
        }

        return prop;
    }


    /**
     * Deep gets or creates a node, parent-padding with default nodes nodes. If
     * the path is empty, the given parent node is returned.
     *
     * @param path path to node that needs to be deep-created
     * @param checkedOutNodes
     * @return node at path
     * @throws RepositoryException if an error occurs
     * @throws IllegalArgumentException if the path is relative and parent is
     *             <code>null</code>
     */
    protected Node deepGetOrCreateNode(Session session, String path,
            Map<String, RequestProperty> reqProperties, List<Modification> changes,
            VersioningConfiguration versioningConfiguration)
            throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("Deep-creating Node '{}'", path);
        }
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must be an absolute path.");
        }
        // get the starting node
        String startingNodePath = path;
        Node startingNode = null;
        while (startingNode == null) {
            if (startingNodePath.equals("/")) {
                startingNode = session.getRootNode();
            } else if (session.itemExists(startingNodePath)) {
                startingNode = (Node) session.getItem(startingNodePath);
                updateMixins(session, startingNodePath, reqProperties, changes, versioningConfiguration);
            } else {
                int pos = startingNodePath.lastIndexOf('/');
                if (pos > 0) {
                    startingNodePath = startingNodePath.substring(0, pos);
                } else {
                    startingNodePath = "/";
                }
            }
        }
        // is the searched node already existing?
        if (startingNodePath.length() == path.length()) {
            return startingNode;
        }
        // create nodes
        int from = (startingNodePath.length() == 1
                ? 1
                : startingNodePath.length() + 1);
        Node node = startingNode;
        while (from > 0) {
            final int to = path.indexOf('/', from);
            final String name = to < 0 ? path.substring(from) : path.substring(
                from, to);
            // although the node should not exist (according to the first test
            // above)
            // we do a sanety check.
            if (node.hasNode(name)) {
                node = node.getNode(name);
                updateMixins(session, node.getPath(), reqProperties, changes, versioningConfiguration);
            } else {
                final String tmpPath = to < 0 ? path : path.substring(0, to);
                // check for node type
                final String nodeType = getPrimaryType(reqProperties, tmpPath);
                checkoutIfNecessary(node, changes, versioningConfiguration);

                try {
                    if (nodeType != null) {
                        node = node.addNode(name, nodeType);
                    } else {
                        node = node.addNode(name);

                    }
                } catch (VersionException e) {
                    log.error("Unable to create node named " + name + " in "+node.getPath());
                    throw e;
                }
                // check for mixin types
                final String[] mixinTypes = getMixinTypes(reqProperties,
                    tmpPath);
                if (mixinTypes != null) {
                    for (String mix : mixinTypes) {
                        node.addMixin(mix);
                    }
                }
                changes.add(Modification.onCreated(node.getPath()));
            }
            from = to + 1;
        }
        return node;
    }

    /**
     * Checks the collected content for a jcr:primaryType property at the
     * specified path.
     *
     * @param path path to check
     * @return the primary type or <code>null</code>
     */
    private String getPrimaryType(Map<String, RequestProperty> reqProperties,
            String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:primaryType");
        return prop == null ? null : prop.getStringValues()[0];
    }

    /**
     * Checks the collected content for a jcr:mixinTypes property at the
     * specified path.
     *
     * @param path path to check
     * @return the mixin types or <code>null</code>
     */
    private String[] getMixinTypes(Map<String, RequestProperty> reqProperties,
            String path) {
        RequestProperty prop = reqProperties.get(path + "/jcr:mixinTypes");
        return (prop == null) || !prop.hasValues() ? null : prop.getStringValues();
    }


    protected String generateName(SlingHttpServletRequest request, String basePath)
    	throws RepositoryException {

		// SLING-1091: If a :name parameter is supplied, the (first) value of this parameter is used unmodified as the name 
		//    for the new node. If the name is illegally formed with respect to JCR name requirements, an exception will be 
		//    thrown when trying to create the node. The assumption with the :name parameter is, that the caller knows what 
		//    he (or she) is supplying and should get the exact result if possible.        
		RequestParameterMap parameters = request.getRequestParameterMap();
		RequestParameter specialParam = parameters.getValue(SlingPostConstants.RP_NODE_NAME);
		if ( specialParam != null ) {
		    if ( specialParam.getString() != null && specialParam.getString().length() > 0 ) {
		        // If the path ends with a *, create a node under its parent, with
		        // a generated node name
		        basePath = basePath += "/" + specialParam.getString();
		
		        // if the resulting path already exists then report an error
		        Session session = request.getResourceResolver().adaptTo(Session.class);
	            String jcrPath = removeAndValidateWorkspace(basePath, session);
	            if (session.itemExists(jcrPath)) {
	    		    throw new RepositoryException(
	    			        "Collision in node names for path=" + basePath);
	            }
		
		        return basePath;
		    }
		}

		// no :name value was supplied, so generate a name
		boolean requirePrefix = requireItemPathPrefix(request);
		
		String generatedName = null;
		if (extraNodeNameGenerators != null) {
		    for (NodeNameGenerator generator : extraNodeNameGenerators) {
		        generatedName = generator.getNodeName(request, basePath, requirePrefix, defaultNodeNameGenerator);
		        if (generatedName != null) {
		            break;
		        }
		    }
		}
		if (generatedName == null) {
		    generatedName = defaultNodeNameGenerator.getNodeName(request, basePath, requirePrefix, defaultNodeNameGenerator);
		}
		
		// If the path ends with a *, create a node under its parent, with
		// a generated node name
		basePath += "/" + generatedName;
		
		basePath = ensureUniquePath(request, basePath);
		
		return basePath;
    }

    private String ensureUniquePath(SlingHttpServletRequest request, String basePath) throws RepositoryException {
		// if resulting path exists, add a suffix until it's not the case
		// anymore
		Session session = request.getResourceResolver().adaptTo(Session.class);
		
		String jcrPath = removeAndValidateWorkspace(basePath, session);
		
		// if resulting path exists, add a suffix until it's not the case
		// anymore
		if (session.itemExists(jcrPath)) {
		    for (int idx = 0; idx < 1000; idx++) {
		        String newPath = jcrPath + "_" + idx;
		        if (!session.itemExists(newPath)) {
		            basePath = basePath + "_" + idx;
		            jcrPath = newPath;
		            break;
		        }
		    }
		}
		
		// if it still exists there are more than 1000 nodes ?
		if (session.itemExists(jcrPath)) {
		    throw new RepositoryException(
		        "Collision in generated node names for path=" + basePath);
		}
		
		return basePath;
    }
    
}
