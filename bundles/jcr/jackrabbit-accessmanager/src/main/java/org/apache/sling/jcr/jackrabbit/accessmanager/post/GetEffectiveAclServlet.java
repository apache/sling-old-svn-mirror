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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.accessmanager.GetEffectiveAcl;

/**
 * <p>
 * Sling GET servlet implementation for dumping the declared ACL of a resource to JSON.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Mapped to the default resourceType. Gets and Acl for a resource. Get of the form
 * &gt;resource&lt;.acl.json Provided the user has access to the ACL, they get a chunk of
 * JSON of the form.
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>GET</li>
 * </ul>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example Response</h4>
 * <code>
 * <pre>
 * {
 * &quot;principalNameA&quot;:
 *      { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *        &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;]
 *       },
 * &quot;principalNameB&quot;:
 *       { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *         &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;] },
 * &quot;principalNameC&quot;:
 *       { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *         &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;] }
 * }
 * </pre>
 * </code>
 */
@Component (immediate=true,
		label="%getEffectiveAcl.get.operation.name",
		description="%getEffectiveAcl.get.operation.description"
)
@Service (value={
		Servlet.class,
		GetEffectiveAcl.class
})
@Properties ({
	@Property (name="sling.servlet.resourceTypes", 
			value="sling/servlet/default"),
	@Property (name="sling.servlet.methods", 
			value="GET"),
	@Property (name="sling.servlet.selectors", 
			value={
				"eacl",
				"tidy.eacl"
			}),
	@Property (name="sling.servlet.extensions", 
			value="json")
})
@SuppressWarnings("serial")
public class GetEffectiveAclServlet extends AbstractGetAclServlet implements GetEffectiveAcl {

    /* (non-Javadoc)
	 * @see org.apache.sling.jcr.jackrabbit.accessmanager.GetEffectiveAcl#getEffectiveAcl(javax.jcr.Session, java.lang.String)
	 */
	public JSONObject getEffectiveAcl(Session jcrSession, String resourcePath)
			throws RepositoryException, JSONException {
		return internalGetAcl(jcrSession, resourcePath);
	}

	@Override
    protected AccessControlEntry[] getAccessControlEntries(Session session, String absPath) throws RepositoryException {
        AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
        AccessControlPolicy[] policies = accessControlManager.getEffectivePolicies(absPath);
        List<AccessControlEntry> allEntries = new ArrayList<AccessControlEntry>(); 
        for (AccessControlPolicy accessControlPolicy : policies) {
            if (accessControlPolicy instanceof AccessControlList) {
                AccessControlEntry[] accessControlEntries = ((AccessControlList)accessControlPolicy).getAccessControlEntries();
                for (AccessControlEntry accessControlEntry : accessControlEntries) {
					allEntries.add(accessControlEntry);
				}
            }
        }
        return allEntries.toArray(new AccessControlEntry[allEntries.size()]);
    }

}
