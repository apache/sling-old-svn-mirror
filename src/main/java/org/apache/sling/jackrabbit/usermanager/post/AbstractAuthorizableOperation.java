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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.post.impl.DateParser;
import org.apache.sling.jackrabbit.usermanager.post.impl.RequestProperty;
import org.apache.sling.jackrabbit.usermanager.resource.AuthorizableResourceProvider;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;


/**
 * Base class for operations that do work on authorizable resources
 */
public abstract class AbstractAuthorizableOperation extends AbstractSlingPostOperation {
	
    /**
     * @scr.property values.0="EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
     *               values.1="yyyy-MM-dd'T'HH:mm:ss.SSSZ"
     *               values.2="yyyy-MM-dd'T'HH:mm:ss" values.3="yyyy-MM-dd"
     *               values.4="dd.MM.yyyy HH:mm:ss" values.5="dd.MM.yyyy"
     */
    private static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";
	
	private DateParser dateParser;


	/**
     * To be used for the encryption. E.g. for passwords in
     * {@link javax.jcr.SimpleCredentials#getPassword()}  SimpleCredentials} 
     * @scr.property valueRef="DEFAULT_PASSWORD_DIGEST_ALGORITHM"
     */
    private static final String PROP_PASSWORD_DIGEST_ALGORITHM = "password.digest.algorithm";
    private static final String DEFAULT_PASSWORD_DIGEST_ALGORITHM = "sha1";
    private String passwordDigestAlgoritm = null;

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        dateParser = new DateParser();
        String[] dateFormats = OsgiUtil.toStringArray(props.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            dateParser.register(dateFormat);
        }
        Object propValue = props.get(PROP_PASSWORD_DIGEST_ALGORITHM);
        if (propValue instanceof String) {
        	passwordDigestAlgoritm = (String)propValue;
        } else {
        	passwordDigestAlgoritm = DEFAULT_PASSWORD_DIGEST_ALGORITHM;
        }
    }

    protected void deactivate(ComponentContext context) {
        dateParser = null;
        passwordDigestAlgoritm = null;
    }
	
    protected String digestPassword(String pwd) throws IllegalArgumentException {
        try {
            StringBuffer password = new StringBuffer();
            password.append("{").append(passwordDigestAlgoritm).append("}");
            password.append(Text.digest(passwordDigestAlgoritm, pwd.getBytes("UTF-8")));
            return password.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }
	

    /**
     * Update the group membership based on the ":member" request
     * parameters.  If the ":member" value ends with @Delete it is removed
     * from the group membership, otherwise it is added to the group membership.
     * 
     * @param request
     * @param authorizable
     * @throws RepositoryException
     */
	protected void updateGroupMembership(SlingHttpServletRequest request,
			Authorizable authorizable, List<Modification> changes) throws RepositoryException {
		if (authorizable.isGroup()) {
			Group group = ((Group)authorizable);
    		String groupPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX + group.getID(); 

	    	ResourceResolver resolver = request.getResourceResolver();
	    	Resource baseResource = request.getResource();
	    	boolean changed = false;

	    	//first remove any members posted as ":member@Delete"
	    	String[] membersToDelete = request.getParameterValues(SlingPostConstants.RP_PREFIX + "member" + SlingPostConstants.SUFFIX_DELETE);
	    	if (membersToDelete != null) {
				for (String member : membersToDelete) {
	                Resource res = resolver.getResource(baseResource, member);
	                if (res != null) {
	                	Authorizable memberAuthorizable = res.adaptTo(Authorizable.class);
	                	if (memberAuthorizable != null) {
	                		group.removeMember(memberAuthorizable);
	                		changed = true;
	                	}
	                }
					
				}
	    	}
	    	
	    	//second add any members posted as ":member"
	    	String[] membersToAdd = request.getParameterValues(SlingPostConstants.RP_PREFIX + "member");
	    	if (membersToAdd != null) {
				for (String member : membersToAdd) {
	                Resource res = resolver.getResource(baseResource, member);
	                if (res != null) {
	                	Authorizable memberAuthorizable = res.adaptTo(Authorizable.class);
	                	if (memberAuthorizable != null) {
	                		group.addMember(memberAuthorizable);
	                		changed = true;
	                	}
	                }
				}
	    	}

	    	if (changed) {
        		//add an entry to the changes list to record the membership change
        		changes.add(Modification.onModified(groupPath + "/members"));
	    	}
		}
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

	
    /**
     * Returns an iterator on <code>Resource</code> instances addressed in the
     * {@link SlingPostConstants#RP_APPLY_TO} request parameter. If the request
     * parameter is not set, <code>null</code> is returned. If the parameter
     * is set with valid resources an empty iterator is returned. Any resources
     * addressed in the {@link SlingPostConstants#RP_APPLY_TO} parameter is
     * ignored.
     *
     * @param request The <code>SlingHttpServletRequest</code> object used to
     *            get the {@link SlingPostConstants#RP_APPLY_TO} parameter.
     * @return The iterator of resources listed in the parameter or
     *         <code>null</code> if the parameter is not set in the request.
     */
    protected Iterator<Resource> getApplyToResources(
            SlingHttpServletRequest request) {

        String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
        if (applyTo == null) {
            return null;
        }

        return new ApplyToIterator(request, applyTo);
    }

    private static class ApplyToIterator implements Iterator<Resource> {

        private final ResourceResolver resolver;
        private final Resource baseResource;
        private final String[] paths;

        private int pathIndex;

        private Resource nextResource;

        ApplyToIterator(SlingHttpServletRequest request, String[] paths) {
            this.resolver = request.getResourceResolver();
            this.baseResource = request.getResource();
            this.paths = paths;
            this.pathIndex = 0;

            nextResource = seek();
        }

        public boolean hasNext() {
            return nextResource != null;
        }

        public Resource next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Resource result = nextResource;
            nextResource = seek();

            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Resource seek() {
            while (pathIndex < paths.length) {
                String path = paths[pathIndex];
                pathIndex++;

                Resource res = resolver.getResource(baseResource, path);
                if (res != null) {
                    return res;
                }
            }

            // no more elements in the array
            return null;
        }
    }
	
}
