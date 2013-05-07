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

import javax.servlet.jsp.tagext.TagSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for searching for resources using the given query formulated in the given
 * language.
 */
public class FindResourcesTag extends TagSupport {

	/** The Constant log. */
	private static final Logger log = LoggerFactory
			.getLogger(FindResourcesTag.class);
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8717969268407440925L;
	
	/** The query. */
	private String query;
	
	/** The language. */
	private String language;
	
	/** The var. */
	private String var;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		log.debug("Finding resources using query: {} of language {}", query,
				language);

		ResourceResolver resolver = getResourceResolver();
		final Iterator<Resource> resources = resolver.findResources(query,
				language);
		
		log.debug("Saving resources to variable {}", var);
		pageContext.setAttribute(var, resources);

		return EVAL_PAGE;
	}

	/**
	 * Gets the language.
	 *
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Gets the query.
	 *
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Method for retrieving the ResourceResolver from the page context.
	 * 
	 * @return the resource resolver
	 */
	protected ResourceResolver getResourceResolver() {
		final SlingBindings bindings = (SlingBindings) pageContext.getRequest()
				.getAttribute(SlingBindings.class.getName());
		final SlingScriptHelper scriptHelper = bindings.getSling();
		final ResourceResolver resolver = scriptHelper.getRequest()
				.getResourceResolver();
		return resolver;
	}

	/**
	 * Gets the variable name to which to save the list of children.
	 * 
	 * @return the variable name
	 */
	public String getVar() {
		return var;
	}

	/**
	 * Sets the language.
	 *
	 * @param language the new language
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Sets the query.
	 *
	 * @param query the new query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Sets the variable name to which to save the list of children.
	 * 
	 * @param var
	 *            the variable name
	 */
	public void setVar(String var) {
		this.var = var;
	}
}
