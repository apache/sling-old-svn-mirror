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

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for retrieving a property from a ValueMap.
 */
public class GetPropertyTag extends TagSupport {

	private static final Logger log = LoggerFactory
			.getLogger(GetPropertyTag.class);
	private static final long serialVersionUID = -1945089681840552408L;
	private ValueMap properties;
	private String key;
	private Object defaultValue;
	private String returnClass;
	private String var;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		Object value = null;
		if(properties != null){
			if (this.defaultValue != null) {
				value = properties.get(key, defaultValue);
			} else if (returnClass != null && returnClass.trim().length() != 0) {
				ClassLoader classLoader = getClassLoader();
				
				log.debug("Returning value as type: " + returnClass);
				try {
					Class<?> clazz = classLoader.loadClass(returnClass);
					value = properties.get(key, clazz);
				} catch (ClassNotFoundException cnfe) {
					log.warn("Unable to find class: " + returnClass, cnfe);
				}
			} else {
				value = properties.get(key);
			}
		}else{
			if (this.defaultValue != null) {
				value = defaultValue;
			}
		}

		log.debug("Saving " + value + " to variable " + var);
		pageContext.setAttribute(var, value);

		return EVAL_PAGE;
	}

	/**
	 * Method for retrieving the classloader from the OSGi console.
	 * 
	 * @return the classloader
	 */
	protected ClassLoader getClassLoader() {
		final SlingBindings bindings = (SlingBindings) pageContext.getRequest()
				.getAttribute(SlingBindings.class.getName());
		final SlingScriptHelper scriptHelper = bindings.getSling();
		final DynamicClassLoaderManager dynamicClassLoaderManager = scriptHelper
				.getService(DynamicClassLoaderManager.class);
		final ClassLoader classLoader = dynamicClassLoaderManager
				.getDynamicClassLoader();
		return classLoader;
	}

	/**
	 * Gets the default value to return if no value exists for the key. If
	 * specified, this takes precedence over returnClass.
	 * 
	 * @return the default value
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Gets key to retrieve the value from from the ValueMap.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Gets the ValueMap from which to retrieve the value.
	 * 
	 * @return the ValueMap of properties
	 */
	public ValueMap getProperties() {
		return properties;
	}

	/**
	 * Gets the name of class into which to coerce the returned value.
	 * 
	 * @return the class name
	 */
	public String getReturnClass() {
		return returnClass;
	}

	/**
	 * Gets the variable name to which to save the value
	 * 
	 * @return the variable name
	 */
	public String getVar() {
		return var;
	}

	/**
	 * Sets the default value to return if no value exists for the key. If
	 * specified, this takes precedence over returnClass.
	 * 
	 * @param defaultValue
	 *            the default value
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Sets the key to retrieve the value from from the ValueMap.
	 * 
	 * @param key
	 *            the key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Sets the ValueMap from which to retrieve the value.
	 * 
	 * @param properties
	 *            the ValueMap of properties
	 */
	public void setProperties(ValueMap properties) {
		this.properties = properties;
	}

	/**
	 * Sets the name of class into which to coerce the returned value.
	 * 
	 * @param returnClass
	 *            the class name
	 */
	public void setReturnClass(String returnClass) {
		this.returnClass = returnClass;
	}

	/**
	 * Sets the variable name to which to save the value.
	 * 
	 * @param var
	 *            the variable name
	 */
	public void setVar(String var) {
		this.var = var;
	}
}
