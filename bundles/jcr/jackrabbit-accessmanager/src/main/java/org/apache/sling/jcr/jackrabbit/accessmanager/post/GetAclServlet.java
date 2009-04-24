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
package org.apache.sling.jcr.jackrabbit.accessmanager.post;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlList;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling GET servlet implementation for dumping the declared ACL of a resource
 * to JSON.
 * 
 * @scr.component immediate="true" 
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/servlet/default"
 * @scr.property name="sling.servlet.methods" value="GET" 
 * @scr.property name="sling.servlet.selectors" value="acl" 
 * @scr.property name="sling.servlet.extensions " value="json" 
 */
public class GetAclServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = 3391376559396223184L;

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
    		if (session == null) {
    			throw new RepositoryException("JCR Session not found");
    		}
    		
        	String resourcePath = null;
        	Resource resource = request.getResource();
        	if (resource == null) {
    			throw new ResourceNotFoundException("Resource not found.");
        	} else {
        		Item item = resource.adaptTo(Item.class);
        		if (item != null) {
        			resourcePath = item.getPath();
        		} else {
        			throw new ResourceNotFoundException("Resource is not a JCR Node");
        		}
        	}

        	AccessControlEntry[] declaredAccessControlEntries = getDeclaredAccessControlEntries(session, resourcePath);
        	Map<String, Map<String, Set<String>>> aclMap = new LinkedHashMap<String, Map<String,Set<String>>>();
        	for (AccessControlEntry ace : declaredAccessControlEntries) {
    			Principal principal = ace.getPrincipal();
    			Map<String, Set<String>> map = aclMap.get(principal.getName());
    			if (map == null) {
    				map = new LinkedHashMap<String, Set<String>>();
    				aclMap.put(principal.getName(), map);
    			}

    			boolean allow = AccessControlUtil.isAllow(ace);
    			if (allow) {
    				Set<String> grantedSet = map.get("granted");
    				if (grantedSet == null) {
    					grantedSet = new LinkedHashSet<String>();
    					map.put("granted", grantedSet);
    				}
    				Privilege[] privileges = ace.getPrivileges();
    				for (Privilege privilege : privileges) {
    					grantedSet.add(privilege.getName());
    				}
    			} else {
    				Set<String> deniedSet = map.get("denied");
    				if (deniedSet == null) {
    					deniedSet = new LinkedHashSet<String>();
    					map.put("denied", deniedSet);
    				}
    				Privilege[] privileges = ace.getPrivileges();
    				for (Privilege privilege : privileges) {
    					deniedSet.add(privilege.getName());
    				}
    			}
    		}
        	

        	response.setContentType("application/json");
        	response.setCharacterEncoding("UTF-8");

        	JSONObject jsonObj = new JSONObject();
        	Set<Entry<String, Map<String, Set<String>>>> entrySet = aclMap.entrySet();
        	for (Entry<String, Map<String, Set<String>>> entry : entrySet) {
        		String principalName = entry.getKey();
        		Map<String, Set<String>> value = entry.getValue();
        		
        		JSONObject aceObject = new JSONObject();
        		Set<String> grantedSet = value.get("granted");
        		if (grantedSet != null) {
            		aceObject.put("granted", grantedSet);
        		}
        		
        		Set<String> deniedSet = value.get("denied");
        		if (deniedSet != null) {
        			aceObject.put("denied", deniedSet);
        		}

        		jsonObj.put(principalName, aceObject);
			}
        	

            // do the dump
        	jsonObj.write(response.getWriter());
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

	private AccessControlEntry[] getDeclaredAccessControlEntries(Session session, String absPath) throws RepositoryException {
		AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
		AccessControlPolicy[] policies = accessControlManager.getPolicies(absPath);
		for (AccessControlPolicy accessControlPolicy : policies) {
			if (accessControlPolicy instanceof AccessControlList) {
				AccessControlEntry[] accessControlEntries = ((AccessControlList)accessControlPolicy).getAccessControlEntries();
				return accessControlEntries;
			}
		}
		return new AccessControlEntry[0];
	}
	
}
