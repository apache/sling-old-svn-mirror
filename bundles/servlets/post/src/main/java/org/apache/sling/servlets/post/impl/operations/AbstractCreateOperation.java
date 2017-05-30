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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.impl.helper.Chunk;
import org.apache.sling.servlets.post.impl.helper.DefaultNodeNameGenerator;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;

abstract class AbstractCreateOperation extends AbstractPostOperation {
    private final Random randomCollisionIndex = new Random();

    /**
     * The default node name generator
     */
    private NodeNameGenerator defaultNodeNameGenerator;

    /**
     * utility class for generating node names
     */
    private NodeNameGenerator[] extraNodeNameGenerators;

    /**
     * regular expression for parameters to ignore
     */
    private Pattern ignoredParameterNamePattern;

    protected AbstractCreateOperation() {
		this.defaultNodeNameGenerator = new DefaultNodeNameGenerator();
		this.ignoredParameterNamePattern = null;
    }

    public void setDefaultNodeNameGenerator(
            NodeNameGenerator defaultNodeNameGenerator) {
        this.defaultNodeNameGenerator = defaultNodeNameGenerator;
    }

    public void setExtraNodeNameGenerators(
            NodeNameGenerator[] extraNodeNameGenerators) {
        this.extraNodeNameGenerators = extraNodeNameGenerators;
    }

    public void setIgnoredParameterNamePattern(
            final Pattern ignoredParameterNamePattern) {
        this.ignoredParameterNamePattern = ignoredParameterNamePattern;
    }

    /**
     * Returns true if any of the request parameters starts with
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>}.
     * In this case only parameters starting with either of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>} are
     * considered as providing content to be stored. Otherwise all parameters
     * not starting with the command prefix <code>:</code> are considered as
     * parameters to be stored.
     *
     * @param request The http request
     * @return If a prefix is required.
     */
    private final boolean requireItemPathPrefix(
            SlingHttpServletRequest request) {

        boolean requirePrefix = false;

        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements() && !requirePrefix) {
            String name = (String) names.nextElement();
            requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
        }

        return requirePrefix;
    }



    /**
     * Returns <code>true</code> if the <code>name</code> starts with either
     * of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>}.
     *
     * @param name The name
     * @return {@code true} if the name has a prefix
     */
    private boolean hasItemPathPrefix(String name) {
        return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
    }

    /**
     * Create resource(s) according to current request
     *
     * @throws PersistenceException if a resource error occurs
     */
    protected void processCreate(final ResourceResolver resolver,
            final Map<String, RequestProperty> reqProperties,
            final PostResponse response,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {

        final String path = response.getPath();
        final Resource resource = resolver.getResource(path);

        if ( resource == null || ResourceUtil.isSyntheticResource(resource) ) {
            deepGetOrCreateResource(resolver, path, reqProperties, changes, versioningConfiguration);
            response.setCreateRequest(true);

        } else {
            updateNodeType(resolver, path, reqProperties, changes, versioningConfiguration);
            updateMixins(resolver, path, reqProperties, changes, versioningConfiguration);
        }
    }

    protected void updateNodeType(final ResourceResolver resolver,
                    final String path,
                    final Map<String, RequestProperty> reqProperties,
                    final List<Modification> changes,
                    final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        final String nodeType = getPrimaryType(reqProperties, path);
        if (nodeType != null) {
            final Resource rsrc = resolver.getResource(path);
            final ModifiableValueMap mvm = rsrc.adaptTo(ModifiableValueMap.class);
            if ( mvm != null ) {
                final Object node = this.jcrSsupport.getNode(rsrc);
                final boolean wasVersionable = (node == null ? false : this.jcrSsupport.isVersionable(rsrc));

                if ( node != null ) {
                    this.jcrSsupport.checkoutIfNecessary(rsrc, changes, versioningConfiguration);
                    this.jcrSsupport.setPrimaryNodeType(node, nodeType);
                } else {
                    mvm.put(JcrConstants.JCR_PRIMARYTYPE, nodeType);
                }

                if ( node != null ) {
                    // this is a bit of a cheat; there isn't a formal checkout, but assigning
                    // the mix:versionable mixin does an implicit checkout
                    if (!wasVersionable &&
                            versioningConfiguration.isCheckinOnNewVersionableNode() &&
                            this.jcrSsupport.isVersionable(rsrc)) {
                        changes.add(Modification.onCheckout(path));
                    }
                }
            }
        }
    }

    protected void updateMixins(final ResourceResolver resolver,
                    final String path,
                    final Map<String, RequestProperty> reqProperties,
                    final List<Modification> changes,
                    final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        final String[] mixins = getMixinTypes(reqProperties, path);
        if (mixins != null) {

            final Resource rsrc = resolver.getResource(path);
            final ModifiableValueMap mvm = rsrc.adaptTo(ModifiableValueMap.class);
            if ( mvm != null ) {
                this.jcrSsupport.checkoutIfNecessary(rsrc, changes, versioningConfiguration);
                mvm.put(JcrConstants.JCR_MIXINTYPES, mixins);

                for(final String mixin : mixins) {
                    // this is a bit of a cheat; there isn't a formal checkout, but assigning
                    // the mix:versionable mixin does an implicit checkout
                    if (mixin.equals(JcrConstants.MIX_VERSIONABLE) &&
                            versioningConfiguration.isCheckinOnNewVersionableNode()) {
                        changes.add(Modification.onCheckout(path));
                    }
                }
            }
        }
    }

    /**
     * Collects the properties that form the content to be written back to the
     * resource tree.
     */
    protected Map<String, RequestProperty> collectContent(
            final SlingHttpServletRequest request,
            final PostResponse response) {

        final boolean requireItemPrefix = requireItemPathPrefix(request);

        // walk the request parameters and collect the properties
        final LinkedHashMap<String, RequestProperty> reqProperties = new LinkedHashMap<>();
        for (final Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            if (ignoreParameter(paramName)) {
                continue;
            }

            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }

            // ensure the paramName is an absolute property name
            final String propPath = toPropertyPath(paramName, response);

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                final RequestProperty prop = getOrCreateRequestProperty(
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
                final RequestProperty prop = getOrCreateRequestProperty(
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
                final RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.VALUE_FROM_SUFFIX);

                // @ValueFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    final String refName = e.getValue()[0].getString();
                    final RequestParameter[] refValues = request.getRequestParameters(refName);
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
                final RequestProperty prop = getOrCreateRequestProperty(
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
                final RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_MOVE_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    final String sourcePath = e.getValue()[0].getString();
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
                final RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_COPY_FROM);

                // @MoveFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    final String sourcePath = e.getValue()[0].getString();
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
                final RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_IGNORE_BLANKS);

                if (e.getValue().length == 1) {
                    prop.setIgnoreBlanks(true);
                }

                continue;
            }

            if (propPath.endsWith(SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING)) {
                final RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.SUFFIX_USE_DEFAULT_WHEN_MISSING);

                if (e.getValue().length == 1) {
                    prop.setUseDefaultWhenMissing(true);
                }

                continue;
            }
            // @Patch
            // Example:
            // <input name="tags@TypeHint" value="String[]" type="hidden" />
            // <input name="tags@Patch"    value="true" type="hidden" />
            // <input name="tags"          value="+apple" type="hidden" />
            // <input name="tags"          value="-orange" type="hidden" />
            if (propPath.endsWith(SlingPostConstants.SUFFIX_PATCH)) {
                final RequestProperty prop = getOrCreateRequestProperty(
                        reqProperties, propPath,
                        SlingPostConstants.SUFFIX_PATCH);

                prop.setPatch(true);

                continue;
            }
            if (propPath.endsWith(SlingPostConstants.SUFFIX_OFFSET)) {
                final RequestProperty prop = getOrCreateRequestProperty(
                        reqProperties, propPath,
                        SlingPostConstants.SUFFIX_OFFSET);
                if (e.getValue().length == 1) {
                    Chunk chunk = prop.getChunk();
                    if(chunk == null){
                        chunk = new Chunk();
                    }
                    chunk.setOffsetValue(Long.parseLong(e.getValue()[0].toString()));
                    prop.setChunk(chunk);
                }
                continue;
            }

            if (propPath.endsWith(SlingPostConstants.SUFFIX_COMPLETED)) {
                final RequestProperty prop = getOrCreateRequestProperty(
                        reqProperties, propPath,
                        SlingPostConstants.SUFFIX_COMPLETED);
                if (e.getValue().length == 1) {
                    Chunk chunk = prop.getChunk();
                    if(chunk == null){
                        chunk = new Chunk();
                    }
                    chunk.setCompleted(Boolean.parseBoolean((e.getValue()[0].toString())));
                    prop.setChunk(chunk);
                }
                continue;
            }

            if (propPath.endsWith(SlingPostConstants.SUFFIX_LENGTH)) {
                final RequestProperty prop = getOrCreateRequestProperty(
                        reqProperties, propPath,
                        SlingPostConstants.SUFFIX_LENGTH);
                if (e.getValue().length == 1) {
                    Chunk chunk = prop.getChunk();
                    if(chunk == null){
                        chunk = new Chunk();
                    }
                    chunk.setLength(Long.parseLong(e.getValue()[0].toString()));
                    prop.setChunk(chunk);
                }
                continue;
            }

            // plain property, create from values
            final RequestProperty prop = getOrCreateRequestProperty(reqProperties,
                propPath, null);
            prop.setValues(e.getValue());
        }

        return reqProperties;
    }

    /**
     * Returns <code>true</code> if the parameter of the given name should be
     * ignored.
     */
    private boolean ignoreParameter(final String paramName) {
        // do not store parameters with names starting with sling:post
        if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
            return true;
        }

        // SLING-298: skip form encoding parameter
        if (paramName.equals("_charset_")) {
            return true;
        }

        // SLING-2120: ignore parameter match ignoredParameterNamePattern
        if (this.ignoredParameterNamePattern != null
            && this.ignoredParameterNamePattern.matcher(paramName).matches()) {
            return true;
        }

        return false;
    }

    /**
     * Returns the <code>paramName</code> as an absolute (unnormalized) property
     * path by prepending the response path (<code>response.getPath</code>) to
     * the parameter name if not already absolute.
     */
    private String toPropertyPath(String paramName, PostResponse response) {
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
     * Deep gets or creates a resource, parent-padding with default resources. If
     * the path is empty, the given parent resource is returned.
     *
     * @param path path to resources that needs to be deep-created
     * @return Resource at path
     * @throws PersistenceException if an error occurs
     * @throws IllegalArgumentException if the path is relative and parent is
     *             <code>null</code>
     */
    protected Resource deepGetOrCreateResource(final ResourceResolver resolver,
                    final String path,
                    final Map<String, RequestProperty> reqProperties,
                    final List<Modification> changes,
                    final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        if (log.isDebugEnabled()) {
            log.debug("Deep-creating resource '{}'", path);
        }
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("path must be an absolute path.");
        }
        // get the starting resource
        String startingResourcePath = path;
        Resource startingResource = null;
        while (startingResource == null) {
            if (startingResourcePath.equals("/")) {
                startingResource = resolver.getResource("/");
                if (startingResource == null){
                	throw new PersistenceException("Access denied for root resource, resource can't be created: " + path);
                }
            } else {
                final Resource r = resolver.getResource(startingResourcePath);
                if ( r != null && !ResourceUtil.isSyntheticResource(r)) {
                    startingResource = resolver.getResource(startingResourcePath);
                    updateNodeType(resolver, startingResourcePath, reqProperties, changes, versioningConfiguration);
                    updateMixins(resolver, startingResourcePath, reqProperties, changes, versioningConfiguration);
                } else {
                    int pos = startingResourcePath.lastIndexOf('/');
                    if (pos > 0) {
                        startingResourcePath = startingResourcePath.substring(0, pos);
                    } else {
                        startingResourcePath = "/";
                    }
                }
            }
        }
        // is the searched resource already existing?
        if (startingResourcePath.length() == path.length()) {
            return startingResource;
        }
        // create nodes
        int from = (startingResourcePath.length() == 1
                ? 1
                : startingResourcePath.length() + 1);
        Resource resource = startingResource;
        while (from > 0) {
            final int to = path.indexOf('/', from);
            final String name = to < 0 ? path.substring(from) : path.substring(
                from, to);
            // although the resource should not exist (according to the first test
            // above)
            // we do a sanety check.
            final Resource child = resource.getChild(name);
            if (child != null && !ResourceUtil.isSyntheticResource(child)) {
                resource = child;
                updateNodeType(resolver, resource.getPath(), reqProperties, changes, versioningConfiguration);
                updateMixins(resolver, resource.getPath(), reqProperties, changes, versioningConfiguration);
            } else {
                final String tmpPath = to < 0 ? path : path.substring(0, to);
                // check for node type
                final String nodeType = getPrimaryType(reqProperties, tmpPath);

                this.jcrSsupport.checkoutIfNecessary(resource, changes, versioningConfiguration);

                try {
                    final Map<String, Object> props = new HashMap<>();
                    if (nodeType != null) {
                        props.put("jcr:primaryType", nodeType);
                    }
                    // check for mixin types
                    final String[] mixinTypes = getMixinTypes(reqProperties,
                        tmpPath);
                    if (mixinTypes != null) {
                        props.put("jcr:mixinTypes", mixinTypes);
                    }

                    resource = resolver.create(resource, name, props);
                } catch (final PersistenceException e) {
                    log.error("Unable to create resource named " + name + " in " + resource.getPath());
                    throw e;
                }
                changes.add(Modification.onCreated(resource.getPath()));
            }
            from = to + 1;
        }
        return resource;
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
    	throws PersistenceException {

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
	            if (request.getResourceResolver().getResource(basePath) != null) {
	    		    throw new PersistenceException(
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

    /** Generate a unique path in case the node name generator didn't */
    private String ensureUniquePath(SlingHttpServletRequest request, String basePath) throws PersistenceException {
		// if resulting path exists, add a suffix until it's not the case
		// anymore
        final ResourceResolver resolver = request.getResourceResolver();

		// if resulting path exists, add a random suffix until it's not the case
		// anymore
		final int MAX_TRIES = 1000;
		if (resolver.getResource(basePath) != null ) {
		    for(int i=0; i < MAX_TRIES; i++) {
		        final int uniqueIndex = Math.abs(randomCollisionIndex.nextInt());
		        String newPath = basePath + "_" + uniqueIndex;
		        if (resolver.getResource(newPath) == null) {
		            basePath = basePath + "_" + uniqueIndex;
		            basePath = newPath;
		            break;
		        }
		    }

	        // Give up after MAX_TRIES
	        if (resolver.getResource(basePath) != null ) {
	            throw new PersistenceException(
	                "Collision in generated node names under " + basePath + ", generated path " + basePath + " already exists");
	        }
		}

		return basePath;
    }
}
