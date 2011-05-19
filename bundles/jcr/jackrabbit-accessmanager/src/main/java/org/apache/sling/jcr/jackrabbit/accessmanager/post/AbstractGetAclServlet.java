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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
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

	    	JSONObject acl = internalGetAcl(session, resourcePath);
	        response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");

	        acl.write(response.getWriter());
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
	protected JSONObject internalGetAcl(Session jcrSession, String resourcePath) 
    			throws RepositoryException, JSONException {
		
        if (jcrSession == null) {
            throw new RepositoryException("JCR Session not found");
        }

		Item item = jcrSession.getItem(resourcePath);
		if (item != null) {
			resourcePath = item.getPath();
		} else {
			throw new ResourceNotFoundException("Resource is not a JCR Node");
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

            boolean allow = AccessControlUtil.isAllow(ace);
            if (allow) {
                Set<String> grantedSet = (Set<String>) map.get("granted");
                if (grantedSet == null) {
                    grantedSet = new LinkedHashSet<String>();
                    map.put("granted", grantedSet);
                }
                Privilege[] privileges = ace.getPrivileges();
                for (Privilege privilege : privileges) {
                    grantedSet.add(privilege.getName());
                }
            } else {
                Set<String> deniedSet = (Set<String>) map.get("denied");
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

        List<JSONObject> aclList = new ArrayList<JSONObject>();
        Set<Entry<String, Map<String, Object>>> entrySet = aclMap.entrySet();
        for (Entry<String, Map<String, Object>> entry : entrySet) {
            String principalName = entry.getKey();
            Map<String, Object> value = entry.getValue();

            JSONObject aceObject = new JSONObject();
            aceObject.put("principal", principalName);

            Set<String> grantedSet = (Set<String>) value.get("granted");
            if (grantedSet != null) {
                aceObject.put("granted", grantedSet);
            }

            Set<String> deniedSet = (Set<String>) value.get("denied");
            if (deniedSet != null) {
                aceObject.put("denied", deniedSet);
            }
            aceObject.put("order", value.get("order"));
            aclList.add(aceObject);
        }
        JSONObject jsonAclMap = new JSONObject(aclMap);
        for ( JSONObject jsonObj : aclList) {
        	jsonAclMap.put(jsonObj.getString("principal"), jsonObj);
        }
        
        return jsonAclMap;
    }
    
    protected abstract AccessControlEntry[] getAccessControlEntries(Session session, String absPath) throws RepositoryException;

}
