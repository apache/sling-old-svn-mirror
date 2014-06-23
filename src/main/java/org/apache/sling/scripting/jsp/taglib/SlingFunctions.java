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

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.scripting.jsp.taglib.helpers.XSSSupport;
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
	 * Adapt the adaptable to the adapter class.
	 * 
	 * @param adaptable
	 *            the adaptable instance
	 * @param adapter
	 *            the class to which to adapt the adaptable
	 * @return the adapted class instance
	 */
	public static Object adaptTo(Adaptable adaptable, String adapter)
			throws ClassNotFoundException {
		log.trace("adaptTo");
		Object adapted = null;

		if (adaptable != null) {
			log.debug("Adapting {} to class {}", adaptable, adapter);
			try {
				Class<?> adapterClass = loadClass(adapter);
				adapted = adaptable.adaptTo(adapterClass);
			} catch (ClassNotFoundException e) {
				log.error("Could not load class " + adapter, e);
			}
		} else {
			log.warn("Null adaptable specified");
		}
		return adapted;
	}

	/**
	 * Loads the Class for the name from the current thread's classload.
	 * 
	 * @param className
	 *            The name of the class to load
	 * @return the class
	 * @throws ClassNotFoundException
	 *             a class with the specified name could not be found
	 */
	public static String escape(String value, String mode) {
		return XSSSupport.encode(value, XSSSupport.getEncodingMode(mode));
	}

	/**
	 * Searches for resources using the given query formulated in the given
	 * language.
	 * 
	 * @param resourceResolver
	 * @param query
	 *            The query string to use to find the resources.
	 * @param language
	 *            The language in which the query is formulated.
	 * @return An Iterator of Resource objects matching the query.
	 */
	public static Iterator<Resource> findResources(
			ResourceResolver resourceResolver, String query, String language) {
		log.trace("findResources");

		Iterator<Resource> resources = null;
		if (resourceResolver != null) {
			log.debug("Finding resources with query {} of type {}", query,
					language);
			resources = resourceResolver.findResources(query, language);
		} else {
			log.warn("Null resolver specified");
		}
		return resources;
	}

	/**
	 * Gets the resource at the relative path to the provided resource.
	 * 
	 * @param base
	 *            the resource relative to which to find the path
	 * @param path
	 *            the relative path at which to find the resource
	 * @return the resource
	 */
	public static Resource getRelativeResource(Resource base, String path) {
		log.trace("getRelativeResource");

		Resource relative = null;
		if (base != null) {
			log.debug("Getting relative resource of {} at path {}",
					base.getPath(), path);
			relative = base.getResourceResolver().getResource(base, path);
		} else {
			log.warn("Null base resource specified");
		}

		return relative;
	}

	/**
	 * Method allow for the retrieval of resources.
	 * 
	 * @param resolver
	 *            the current resource resolver
	 * @param path
	 *            the path of the resource to retrieve
	 * @return the resource at the path or null
	 */
	public static final Resource getResource(ResourceResolver resolver,
			String path) {
		log.trace("getResource");

		log.debug("Getting resource at path {}", path);
		if (resolver == null) {
			throw new IllegalArgumentException("Null resource resolver");
		}
		return resolver.getResource(path);
	}

	/**
	 * Gets the value of the specified key from the ValueMap and either coerses
	 * the value into the specified type or uses the specified type as a default
	 * depending on the parameter passed in.
	 * 
	 * @param properties
	 *            the ValueMap from which to retrieve the value
	 * @param key
	 *            the key for the value to retrieve
	 * @param defaultOrType
	 *            either the default value or the class to which to coerce the
	 *            value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static final <E> E getValue(ValueMap properties, String key,
			Object defaultOrType) {
		if (defaultOrType instanceof Class<?>) {
			return properties.get(key, (Class<E>) defaultOrType);
		} else {
			return properties.get(key, (E) defaultOrType);
		}
	}

	/**
	 * Method for checking whether or not a resource has child resources.
	 * 
	 * @param resource
	 *            the resource to check for child resources
	 * @return true if the resource has child resources, false otherwise
	 * @since 2.2.2
	 */
	public static final boolean hasChildren(Resource resource) {
		return resource != null ? resource.listChildren().hasNext() : false;
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
	public static final Iterator<Resource> listChildren(Resource resource) {
		log.trace("listChildren");

		Iterator<Resource> children = null;
		if (resource != null) {
			log.debug("Listing children at path {}", resource.getPath());
			children = resource.listChildren();
		} else {
			log.warn("Null resource specified");
		}
		return children;
	}

	/**
	 * Loads the Class for the name from the current thread's classload.
	 * 
	 * @param className
	 *            The name of the class to load
	 * @return the class
	 * @throws ClassNotFoundException
	 *             a class with the specified name could not be found
	 */
	private static Class<?> loadClass(String className)
			throws ClassNotFoundException {
		return Thread.currentThread().getContextClassLoader()
				.loadClass(className);
	}

}