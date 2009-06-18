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
package org.apache.sling.jackrabbit.usermanager.post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingRequestPaths;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.post.impl.DateParser;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all the POST servlets for the UserManager operations 
 */
public abstract class AbstractAuthorizablePostServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = -5918670409789895333L;

	/**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * @scr.property values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
     *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ"
     *               values.2="yyyy-MM-dd'T'HH:mm:ss" values.3="yyyy-MM-dd"
     *               values.4="dd.MM.yyyy HH:mm:ss" values.5="dd.MM.yyyy"
     */
    private static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";
	
	private DateParser dateParser;
	
    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        dateParser = new DateParser();
        String[] dateFormats = OsgiUtil.toStringArray(props.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            dateParser.register(dateFormat);
        }
    }

    protected void deactivate(ComponentContext context) {
        dateParser = null;
    }
	
    
	/* (non-Javadoc)
	 * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
	 */
	@Override
	protected void doPost(SlingHttpServletRequest request,
			SlingHttpServletResponse httpResponse) throws ServletException,
			IOException {
        // prepare the response
        HtmlResponse htmlResponse = new HtmlResponse();
        htmlResponse.setReferer(request.getHeader("referer"));

        // calculate the paths
        String path = getItemPath(request);
        htmlResponse.setPath(path);

        // location
        htmlResponse.setLocation(externalizePath(request, path));

        // parent location
        path = ResourceUtil.getParent(path);
        if (path != null) {
        	htmlResponse.setParentLocation(externalizePath(request, path));
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);

        final List<Modification> changes = new ArrayList<Modification>();
        
        try {
            handleOperation(request, htmlResponse, changes);
            
            //TODO: maybe handle SlingAuthorizablePostProcessor handlers here
            
            // set changes on html response
            for(Modification change : changes) {
                switch ( change.getType() ) {
                    case MODIFY : htmlResponse.onModified(change.getSource()); break;
                    case DELETE : htmlResponse.onDeleted(change.getSource()); break;
                    case MOVE :   htmlResponse.onMoved(change.getSource(), change.getDestination()); break;
                    case COPY :   htmlResponse.onCopied(change.getSource(), change.getDestination()); break;
                    case CREATE : htmlResponse.onCreated(change.getSource()); break;
                    case ORDER : htmlResponse.onChange("ordered", change.getSource(), change.getDestination()); break;
                }
            }
            
            if (session.hasPendingChanges()) {
                session.save();
            }
        } catch (ResourceNotFoundException rnfe) {
            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                rnfe.getMessage());
        } catch (Throwable throwable) {
            log.debug("Exception while handling POST "
                + request.getResource().getPath() + " with "
                + getClass().getName(), throwable);
            htmlResponse.setError(throwable);
        } finally {
            try {
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException in finally block: {}",
                    e.getMessage(), e);
            }
        }
        
        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
            String redirect = getRedirectUrl(request, htmlResponse);
            if (redirect != null) {
                httpResponse.sendRedirect(redirect);
                return;
            }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(httpResponse, isSetStatus(request));
	}

	/**
	 * Extending Servlet should implement this operation to do the work
	 * 
	 * @param request the sling http request to process
	 * @param htmlResponse the response 
	 * @param changes 
	 */
	abstract protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes) throws RepositoryException;
	
	
    /**
     * compute redirect URL (SLING-126)
     *
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(HttpServletRequest request, HtmlResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
        if (result != null && ctx.getPath() != null) {

            // redirect to created/modified Resource
            int star = result.indexOf('*');
            if (star >= 0) {
                StringBuffer buf = new StringBuffer();

                // anything before the star
                if (star > 0) {
                    buf.append(result.substring(0, star));
                }

                // append the name of the manipulated node
                buf.append(ResourceUtil.getName(ctx.getPath()));

                // anything after the star
                if (star < result.length() - 1) {
                    buf.append(result.substring(star + 1));
                }

                // use the created path as the redirect result
                result = buf.toString();

            } else if (result.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
                // if the redirect has a trailing slash, append modified node
                // name
                result = result.concat(ResourceUtil.getName(ctx.getPath()));
            }

            if (log.isDebugEnabled()) {
                log.debug("Will redirect to " + result);
            }
        }
        return result;
    }

    protected boolean isSetStatus(SlingHttpServletRequest request) {
        String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
        if (statusParam == null) {
            log.debug(
                "getStatusMode: Parameter {} not set, assuming standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        if (SlingPostConstants.STATUS_VALUE_BROWSER.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for user-friendly status code",
                SlingPostConstants.RP_STATUS);
            return false;
        }

        if (SlingPostConstants.STATUS_VALUE_STANDARD.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        log.debug(
            "getStatusMode: Parameter {} set to unknown value {}, assuming standard status code",
            SlingPostConstants.RP_STATUS);
        return true;
    }
	
    
    
    // ------ The methods below are based on the private methods from the ModifyOperation class -----
    
    /**
     * Collects the properties that form the content to be written back to the
     * repository. 
     * 
     * NOTE: In the returned map, the key is the property name not a path.
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    protected Map<String, RequestProperty> collectContent(
            SlingHttpServletRequest request, HtmlResponse response) {

        boolean requireItemPrefix = requireItemPathPrefix(request);

        // walk the request parameters and collect the properties
        Map<String, RequestProperty> reqProperties = new HashMap<String, RequestProperty>();
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
            String propPath;
            if (paramName.startsWith("./")) {
            	propPath = paramName.substring(2);
            } else {
            	propPath = paramName;
            }
            if (propPath.indexOf('/') != -1) {
            	//only one path segment is valid here, so this paramter can't be used.
            	continue; //skip it.
            }

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
            	//don't support @MoveFrom here
                continue;
            }

            // SLING-455: @CopyFrom means moving content to another location
            // @CopyFrom example:
            // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
            	//don't support @CopyFrom here
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
     * Removes all properties listed as {@link RequestProperty#isDelete()} from
     * the authorizable.
     *
     * @param authorizable The <code>org.apache.jackrabbit.api.security.user.Authorizable</code> 
     * 				that should have properties deleted.
     * @param reqProperties The map of request properties to check for
     *            properties to be removed.
     * @param response The <code>HtmlResponse</code> to be updated with
     *            information on deleted properties.
     * @throws RepositoryException Is thrown if an error occurrs checking or
     *             removing properties.
     */
    protected void processDeletes(Authorizable resource, 
            Map<String, RequestProperty> reqProperties,
            List<Modification> changes) throws RepositoryException {

        for (RequestProperty property : reqProperties.values()) {
            if (property.isDelete()) {
            	if (resource.hasProperty(property.getName())) {
            		resource.removeProperty(property.getName());
                    changes.add(Modification.onDeleted(property.getPath()));
            	}
            }
        }
    }

    
    /**
     * Writes back the content
     *
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    protected void writeContent(Session session, Authorizable authorizable,
            Map<String, RequestProperty> reqProperties, List<Modification> changes)
            throws RepositoryException {

        for (RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                // skip jcr special properties
                if (prop.getName().equals("jcr:primaryType")
                    || prop.getName().equals("jcr:mixinTypes")) {
                    continue;
                }
                if (authorizable.isGroup()) {
                    if (prop.getName().equals("groupId")) {
                    	//skip these
                    	continue;
                	}                	
                } else {
                    if (prop.getName().equals("userId") ||
                    		prop.getName().equals("pwd") ||
                    		prop.getName().equals("pwdConfirm")) {
                    	//skip these
                    	continue;
                    }
                }
                if (prop.isFileUpload()) {
                	//don't handle files for user properties for now.
                	continue;
                    //uploadHandler.setFile(parent, prop, changes);
                } else {
                	setPropertyAsIs(session, authorizable, prop, changes);
                }
            }
        }
    }
    
    /**
     * set property without processing, except for type hints
     *
     * @param parent the parent node
     * @param prop the request property
     * @throws RepositoryException if a repository error occurs.
     */
    private void setPropertyAsIs(Session session, Authorizable parent, RequestProperty prop, List<Modification> changes)
            throws RepositoryException {

    	String parentPath;
    	if (parent.isGroup()) {
    		parentPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + parent.getID();
    	} else {
    		parentPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX + parent.getID();
    	}


        // no explicit typehint
        int type = PropertyType.UNDEFINED;
        if (prop.getTypeHint() != null) {
            try {
                type = PropertyType.valueFromName(prop.getTypeHint());
            } catch (Exception e) {
                // ignore
            }
        }

        String[] values = prop.getStringValues();
		if (values == null) {
            // remove property
	        boolean removedProp = removePropertyIfExists(parent, prop.getName());
	        if (removedProp) {
	            changes.add(Modification.onDeleted(
	            		parentPath + "/" + prop.getName()
	            ));
	        }
        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            if (parent.hasProperty(prop.getName())) {
            	Value val = session.getValueFactory().createValue("");
            	parent.setProperty(prop.getName(), val);
                changes.add(Modification.onModified(
                	parentPath + "/" + prop.getName()
                ));
            }
        } else if (values.length == 1) {
            boolean removedProp = removePropertyIfExists(parent, prop.getName());
            // if the provided value is the empty string, we don't have to do anything.
            if ( values[0].length() == 0 ) {
                if ( removedProp ) {
                    changes.add(Modification.onDeleted(parentPath + "/" + prop.getName()));
                }
            } else {
                // modify property
                if (type == PropertyType.DATE) {
                    // try conversion
                    Calendar c = dateParser.parse(values[0]);
                    if (c != null) {
                        if ( prop.hasMultiValueTypeHint() ) {
                            final Value[] array = new Value[1];
                            array[0] = session.getValueFactory().createValue(c);
                            parent.setProperty(prop.getName(), array);
                            changes.add(Modification.onModified(
                                parentPath + "/" + prop.getName()
                            ));
                        } else {
                        	Value cVal = session.getValueFactory().createValue(c);
                        	parent.setProperty(prop.getName(), cVal);
                            changes.add(Modification.onModified(
                                    parentPath + "/" + prop.getName()
                                ));
                        }
                        return;
                    }
                    // fall back to default behaviour
                }
                if ( type == PropertyType.UNDEFINED ) {
                	Value val = session.getValueFactory().createValue(values[0], PropertyType.STRING);
                	parent.setProperty(prop.getName(), val);
                } else {
                    if ( prop.hasMultiValueTypeHint() ) {
                        final Value[] array = new Value[1];
                        array[0] = session.getValueFactory().createValue(values[0], type);
                        parent.setProperty(prop.getName(), array);
                    } else {
                    	Value val = session.getValueFactory().createValue(values[0], type);
                        parent.setProperty(prop.getName(), val);
                    }
                }
                changes.add(Modification.onModified(parentPath + "/" + prop.getName()));
            }
        } else {
            removePropertyIfExists(parent, prop.getName());
            if (type == PropertyType.DATE) {
                // try conversion
                ValueFactory valFac = session.getValueFactory();
                Value[] c = dateParser.parse(values, valFac);
                if (c != null) {
                	parent.setProperty(prop.getName(), c);
                    changes.add(Modification.onModified(
                    		parentPath + "/" + prop.getName()
                    ));
                    return;
                }
                // fall back to default behaviour
            }

            Value [] vals = new Value[values.length];
            if ( type == PropertyType.UNDEFINED ) {
            	for(int i=0; i < values.length; i++) {
            		vals[i] = session.getValueFactory().createValue(values[i]);
            	}
            } else {
            	for(int i=0; i < values.length; i++) {
            		vals[i] = session.getValueFactory().createValue(values[i], type);
            	}
            }
        	parent.setProperty(prop.getName(), vals);
            changes.add(Modification.onModified(parentPath + "/" + prop.getName()));
        }
    
    }

    /**
     * Removes the property with the given name from the parent resource if it
     * exists.
     *
     * @param parent the parent resource
     * @param name the name of the property to remove
     * @return path of the property that was removed or <code>null</code> if
     *         it was not removed
     * @throws RepositoryException if a repository error occurs.
     */
	private boolean removePropertyIfExists(Authorizable resource, String name) throws RepositoryException {
    	if (resource.getProperty(name) != null) {
    		resource.removeProperty(name);
    		return true;
    	}
    	return false;
	}

	
	// ------ These methods were copied from AbstractSlingPostOperation ------

    /**
     * Returns the path of the resource of the request as the item path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     */
    protected String getItemPath(SlingHttpServletRequest request) {
        return request.getResource().getPath();
    }

    /**
     * Returns an external form of the given path prepending the context path
     * and appending a display extension.
     *
     * @param path the path to externalize
     * @return the url
     */
    protected final String externalizePath(SlingHttpServletRequest request,
            String path) {
        StringBuffer ret = new StringBuffer();
        ret.append(SlingRequestPaths.getContextPath(request));
        ret.append(request.getResourceResolver().map(path));

        // append optional extension
        String ext = request.getParameter(SlingPostConstants.RP_DISPLAY_EXTENSION);
        if (ext != null && ext.length() > 0) {
            if (ext.charAt(0) != '.') {
                ret.append('.');
            }
            ret.append(ext);
        }

        return ret.toString();
    }
	
    /**
     * Returns <code>true</code> if the <code>name</code> starts with either
     * of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>}.
     */
    protected boolean hasItemPathPrefix(String name) {
        return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
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
     */
    protected final boolean requireItemPathPrefix(
            SlingHttpServletRequest request) {

        boolean requirePrefix = false;

        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements() && !requirePrefix) {
            String name = (String) names.nextElement();
            requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
        }

        return requirePrefix;
    }
    
}
