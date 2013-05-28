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

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for adapting adaptables to classes.
 */
public class AdaptToTag extends TagSupport {

	private static final Logger log = LoggerFactory
			.getLogger(AdaptToTag.class);
	private static final long serialVersionUID = -1945089681840552408L;
	private Adaptable adaptable;
	private String adaptTo;
	private String var;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		ClassLoader classLoader = getClassLoader();
		log.debug("Adapting adaptable " + adaptable + " to class " + adaptTo);

		if (adaptable != null) {
			Object adapted = null;
			try {
				Class<?> adaptToClass = classLoader.loadClass(adaptTo);
				adapted = adaptable.adaptTo(adaptToClass);
			} catch (ClassNotFoundException e) {
				log.warn("Unable to retrieve class " + adaptTo, e);
			}

			log.debug("Saving " + adapted + " to variable " + var);
			pageContext.setAttribute(var, adapted);
		} else {
			log.warn("Null adaptable specified");
		}

		return EVAL_PAGE;
	}

	/**
	 * Get the adaptable object to be adapted.
	 * 
	 * @return the adaptable
	 */
	public Adaptable getAdaptable() {
		return adaptable;
	}

	/**
	 * Gets the class name to adapt the adaptable to.
	 * 
	 * @return the class to adapt to
	 */
	public String getAdaptTo() {
		return adaptTo;
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
	 * Gets the variable name to save the resulting object to.
	 * 
	 * @return the variable name
	 */
	public String getVar() {
		return var;
	}

	/**
	 * Sets the adaptable object to be adapted.
	 * 
	 * @param adaptable
	 *            the object to adapt
	 */
	public void setAdaptable(Adaptable adaptable) {
		this.adaptable = adaptable;
	}

	/**
	 * Sets the class name to adapt the adaptable to.
	 * 
	 * @param adaptTo
	 *            the class to adapt to
	 */
	public void setAdaptTo(String adaptTo) {
		this.adaptTo = adaptTo;
	}

	/**
	 * Gets the variable name to save the resulting object to.
	 * 
	 * @param var
	 *            the variable name
	 */
	public void setVar(String var) {
		this.var = var;
	}

}
