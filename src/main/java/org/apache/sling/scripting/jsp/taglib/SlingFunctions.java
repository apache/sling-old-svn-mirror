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
package org.apache.sling.scripting.jsp.taglib;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing the TagLib Functions for Sling.
 */
public class SlingFunctions {

	/**
	 * The SLF4J Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(SlingFunctions.class);

	/**
	 * Method allow for the retrieval of resources.
	 * 
	 * @param resolver
	 *            the current resource resolver
	 * @param path
	 *            the path of the resource to retrieve
	 * @return
	 */
	public static final Resource getResource(ResourceResolver resolver, String path) {
		return resolver.getResource(path);
	}

	/**
	 * Method for allowing the invocation of the Sling Resource listChildren
	 * method.
	 * 
	 * @param resource
	 *            the resource of which to list the children
	 * @return the children of the resource
	 * @see org.apache.sling.api.resource.Resource#listChildren()
	 */
	public static final Iterator<Resource> listChildResources(Resource resource) {
		log.trace("listChildren");
		if (resource != null) {
			return resource.listChildren();
		} else {
			return null;
		}
	}
}
