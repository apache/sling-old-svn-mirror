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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for retrieving resources based on either an absolute path or a relative
 * path and a base resource.
 */
public class GetResourceTag extends TagSupport {

	/** The Constant log. */
	private static final Logger log = LoggerFactory
			.getLogger(GetResourceTag.class);

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1945089681840552408L;

	/** The base. */
	private Resource base;

	/** The path. */
	private String path;

	/** The var. */
	private String var;

	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		ResourceResolver resolver = getResourceResolver();
		Resource resource = null;
		if (path.startsWith("/")) {
			log.debug("Retrieving resource at absolute path: {}", path);
			resource = resolver.getResource(path);
		} else {
			if (base != null) {
				log.debug(
						"Retrieving resource at relative path: {} to resource {}",
						path, base.getPath());
				resource = resolver.getResource(base, path);
			} else {
				log.warn(
						"Unable to retrieve resource at relative path {}, no base resource specified",
						path);
			}
		}

		log.debug("Saving {} to variable {}", resource, var);
		pageContext.setAttribute(var, resource);

		return EVAL_PAGE;
	}

	/**
	 * Gets the base resource.
	 * 
	 * @return the base resource
	 */
	public Resource getBase() {
		return base;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	/**
	 * Get the path of the resource to retrieve.
	 * 
	 * @return the path
	 */
	public String getPath() {
		return path;
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
	 * Sets the base resource.
	 * 
	 * @param base
	 *            the new base resource
	 */
	public void setBase(Resource base) {
		this.base = base;
	}

	/**
	 * Set the path of the resource to retrieve.
	 * 
	 * @param path
	 *            the path
	 */
	public void setPath(String path) {
		this.path = path;
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
