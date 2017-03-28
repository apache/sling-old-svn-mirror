/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.jackrabbit.accessmanager.post;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public abstract class AbstractGetAclServlet extends SlingAllMethodsServlet {

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /* (non-Javadoc)
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

		try {
			Session session = request.getResourceResolver().adaptTo(Session.class);
	    	String resourcePath = request.getResource().getPath();

	    	JsonObject acl = internalGetAcl(session, resourcePath);
	        response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");

	        boolean isTidy = false;
	        final String[] selectors = request.getRequestPathInfo().getSelectors();
	        if (selectors != null && selectors.length > 0) {
	        	for (final String level : selectors) {
		            if("tidy".equals(level)) {
		            	isTidy = true;
		            	break;
		            }
				}
	        }

	        Map<String, Object> options = new HashMap<>();
            options.put(JsonGenerator.PRETTY_PRINTING, isTidy);
	        Json.createGeneratorFactory(options).createGenerator(response.getWriter()).write(acl).flush();
        } catch (AccessDeniedException ade) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ResourceNotFoundException rnfe) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, rnfe.getMessage());
        } catch (Throwable throwable) {
            log.debug("Exception while handling GET "
                + request.getResource().getPath() + " with "
                + getClass().getName(), throwable);
            throw new ServletException(throwable);
        }
    }

    @SuppressWarnings("unchecked")
	protected JsonObject internalGetAcl(Session jcrSession, String resourcePath) throws RepositoryException {

        if (jcrSession == null) {
            throw new RepositoryException("JCR Session not found");
        }

		Item item = jcrSession.getItem(resourcePath);
		if (item != null) {
			resourcePath = item.getPath();
		} else {
			throw new ResourceNotFoundException("Resource is not a JCR Node");
		}

		// Calculate a map of privileges to all the aggregate privileges it is contained in.
		// Use for fast lookup during the mergePrivilegeSets calls below.
        AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(jcrSession);
		Map<Privilege, Set<Privilege>> privilegeToAncestorMap = new HashMap<Privilege, Set<Privilege>>();
        Privilege[] supportedPrivileges = accessControlManager.getSupportedPrivileges(item.getPath());
        for (Privilege privilege : supportedPrivileges) {
			if (privilege.isAggregate()) {
				Privilege[] ap = privilege.getAggregatePrivileges();
				for (Privilege privilege2 : ap) {
					Set<Privilege> set = privilegeToAncestorMap.get(privilege2);
					if (set == null) {
						set = new HashSet<Privilege>();
						privilegeToAncestorMap.put(privilege2, set);
					}
					set.add(privilege);
				}
			}
		}

        AccessControlEntry[] declaredAccessControlEntries = getAccessControlEntries(jcrSession, resourcePath);
        Map<String, Map<String, Object>> aclMap = new LinkedHashMap<String, Map<String,Object>>();
        int sequence = 0;

        for (AccessControlEntry ace : declaredAccessControlEntries) {
            Principal principal = ace.getPrincipal();
            Map<String, Object> map = aclMap.get(principal.getName());
            if (map == null) {
                map = new LinkedHashMap<String, Object>();
                aclMap.put(principal.getName(), map);
                map.put("order", sequence++);
            }
        }
        //evaluate these in reverse order so the most entries with highest specificity are last
        for (int i = declaredAccessControlEntries.length - 1; i >= 0; i--) {
			AccessControlEntry ace = declaredAccessControlEntries[i];

			Principal principal = ace.getPrincipal();
            Map<String, Object> map = aclMap.get(principal.getName());

            Set<Privilege> grantedSet = (Set<Privilege>) map.get("granted");
            if (grantedSet == null) {
                grantedSet = new LinkedHashSet<Privilege>();
                map.put("granted", grantedSet);
            }
            Set<Privilege> deniedSet = (Set<Privilege>) map.get("denied");
            if (deniedSet == null) {
                deniedSet = new LinkedHashSet<Privilege>();
                map.put("denied", deniedSet);
            }

            boolean allow = AccessControlUtil.isAllow(ace);
            if (allow) {
                Privilege[] privileges = ace.getPrivileges();
                for (Privilege privilege : privileges) {
                	mergePrivilegeSets(privilege,
                			privilegeToAncestorMap,
							grantedSet, deniedSet);
                }
            } else {
                Privilege[] privileges = ace.getPrivileges();
                for (Privilege privilege : privileges) {
                	mergePrivilegeSets(privilege,
                			privilegeToAncestorMap,
							deniedSet, grantedSet);
                }
            }
        }

        List<JsonObject> aclList = new ArrayList<>();
        Set<Entry<String, Map<String, Object>>> entrySet = aclMap.entrySet();
        for (Entry<String, Map<String, Object>> entry : entrySet) {
            String principalName = entry.getKey();
            Map<String, Object> value = entry.getValue();

            JsonObjectBuilder aceObject = Json.createObjectBuilder();
            aceObject.add("principal", principalName);

            Set<Privilege> grantedSet = (Set<Privilege>) value.get("granted");
            if (grantedSet != null && !grantedSet.isEmpty()) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Privilege v : grantedSet)
                {
                    arrayBuilder.add(v.getName());
                }
                aceObject.add("granted", arrayBuilder);
            }

            Set<Privilege> deniedSet = (Set<Privilege>) value.get("denied");
            if (deniedSet != null && !deniedSet.isEmpty()) {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Privilege v : deniedSet)
                {
                    arrayBuilder.add(v.getName());
                }
                aceObject.add("denied", arrayBuilder);
            }
            aceObject.add("order", (Integer) value.get("order"));
            aclList.add(aceObject.build());
        }
        JsonObjectBuilder jsonAclMap = Json.createObjectBuilder();
        for (Map.Entry<String, Map<String, Object>> entry : aclMap.entrySet())
        {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Map.Entry<String, Object> inner : entry.getValue().entrySet())
            {
                addTo(builder, inner.getKey(), inner.getValue());
            }
            jsonAclMap.add(entry.getKey(), builder);
        }
        for (JsonObject jsonObj : aclList) {
            jsonAclMap.add(jsonObj.getString("principal"), jsonObj);
        }

        return jsonAclMap.build();
    }
    
    private JsonObjectBuilder addTo(JsonObjectBuilder builder, String key, Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
        {
            builder.add(key, ((Number) value).longValue());
        }
        else if (value instanceof Float || value instanceof Double)
        {
            builder.add(key, ((Number) value).doubleValue());
        }
        else if (value instanceof Privilege)
        {
            JsonObjectBuilder privilegeBuilder = Json.createObjectBuilder();
            privilegeBuilder.add("name", ((Privilege) value).getName());
            builder.add(key, privilegeBuilder);
        }
        else if (value instanceof String)
        {
            builder.add(key, (String) value);
        }
        else
        {
            builder.add(key, value.toString());
        }
        return builder;
    }

	/**
	 * Update the granted and denied privilege sets by merging the result of adding
	 * the supplied privilege.
	 */
	private void mergePrivilegeSets(Privilege privilege,
			Map<Privilege, Set<Privilege>> privilegeToAncestorMap,
			Set<Privilege> firstSet, Set<Privilege> secondSet) {
		//1. remove duplicates and invalid privileges from the list
		if (privilege.isAggregate()) {
			Privilege[] aggregatePrivileges = privilege.getAggregatePrivileges();
			//remove duplicates from the granted set
			List<Privilege> asList = Arrays.asList(aggregatePrivileges);
			firstSet.removeAll(asList);

			//remove from the denied set
			secondSet.removeAll(asList);
		}
		secondSet.remove(privilege);

		//2. check if the privilege is already contained in another granted privilege
		boolean isAlreadyGranted = false;
		Set<Privilege> ancestorSet = privilegeToAncestorMap.get(privilege);
		if (ancestorSet != null) {
			for (Privilege privilege2 : ancestorSet) {
				if (firstSet.contains(privilege2)) {
					isAlreadyGranted = true;
					break;
				}
			}
		}

		//3. add the privilege
		if (!isAlreadyGranted) {
		    firstSet.add(privilege);
		}

		//4. Deal with expanding existing aggregate privileges to remove the invalid
		//  items and add the valid ones.
		Set<Privilege> filterSet = privilegeToAncestorMap.get(privilege);
		if (filterSet != null) {
	    	//re-pack the denied set to compensate
	    	for (Privilege privilege2 : filterSet) {
	    		if (secondSet.contains(privilege2)) {
	    			secondSet.remove(privilege2);
	    			if (privilege2.isAggregate()) {
		    			filterAndMergePrivilegesFromAggregate(privilege2,
		    					firstSet, secondSet, filterSet, privilege);
	    			}
	    		}
			}
		}
	}

	/**
	 * Add all the declared aggregate privileges from the supplied privilege to the secondSet
	 * unless the privilege is already in the firstSet and not contained in the supplied filterSet.
	 */
	private void filterAndMergePrivilegesFromAggregate(Privilege privilege, Set<Privilege> firstSet,
			Set<Privilege> secondSet, Set<Privilege> filterSet, Privilege ignorePrivilege) {
		Privilege[] declaredAggregatePrivileges = privilege.getDeclaredAggregatePrivileges();
		for (Privilege privilege3 : declaredAggregatePrivileges) {
			if (ignorePrivilege.equals(privilege3)) {
				continue; //skip it.
			}
			if (!firstSet.contains(privilege3) && !filterSet.contains(privilege3)) {
				secondSet.add(privilege3);
			}
			if (privilege3.isAggregate()) {
				Privilege[] declaredAggregatePrivileges2 = privilege3.getDeclaredAggregatePrivileges();
				for (Privilege privilege2 : declaredAggregatePrivileges2) {
					if (!ignorePrivilege.equals(privilege2)) {
						if (privilege2.isAggregate()) {
							filterAndMergePrivilegesFromAggregate(privilege2,
									firstSet, secondSet, filterSet, ignorePrivilege);
						}
					}
				}
			}
		}
	}

    protected abstract AccessControlEntry[] getAccessControlEntries(Session session, String absPath) throws RepositoryException;

}
