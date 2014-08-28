/*-
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.query.resource.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionJcrTypeResolver implements JcrTypeResolver {

	private static final Logger LOG = LoggerFactory.getLogger(SessionJcrTypeResolver.class);

	private final NodeTypeManager nodeTypeManager;

	public SessionJcrTypeResolver(ResourceResolver resolver) {
		NodeTypeManager m = null;
		try {
			if (resolver != null) {
				m = resolver.adaptTo(Session.class).getWorkspace().getNodeTypeManager();
			}
		} catch (RepositoryException e) {
			LOG.error("Can't get node type manager", e);
			m = null;
		}
		nodeTypeManager = m;
	}

	@Override
	public boolean isJcrType(String name) {
		if (nodeTypeManager == null) {
			return false;
		}
		if (name == null || name.contains("/")) {
			return false;
		}
		try {
			nodeTypeManager.getNodeType(name);
			return true;
		} catch (NoSuchNodeTypeException e) {
			return false;
		} catch (RepositoryException e) {
			LOG.error("Can't check node type " + name, e);
			return false;
		}
	}

	@Override
	public boolean isSubtype(String supertype, String subtype) {
		if (nodeTypeManager == null) {
			return false;
		}
		if (!isJcrType(subtype) || !isJcrType(supertype)) {
			return false;
		}
		try {
			return nodeTypeManager.getNodeType(subtype).isNodeType(supertype);
		} catch (RepositoryException e) {
			LOG.error("Can't compare two node types: " + subtype + " and " + supertype, e);
			return false;
		}
	}

}
