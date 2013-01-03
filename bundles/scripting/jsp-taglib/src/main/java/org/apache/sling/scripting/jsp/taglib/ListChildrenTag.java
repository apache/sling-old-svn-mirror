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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for listing the children of a Resource. The children will be returned as
 * an Iterator<Resource> in the specified variable.
 */
public class ListChildrenTag extends TagSupport {

	private static final Logger log = LoggerFactory
			.getLogger(ListChildrenTag.class);
	private static final long serialVersionUID = -1945089681840552408L;
	private Resource resource;
	private String var;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() {
		log.trace("doEndTag");

		log.debug("Listing the children of resource: " + resource);

		Iterator<Resource> children = null;
		if (resource != null) {
			children = resource.listChildren();
		}

		log.debug("Saving to variable " + var);
		pageContext.setAttribute(var, children);

		return EVAL_PAGE;
	}

	/**
	 * Gets the resource of which to list the children.
	 * 
	 * @return the Sling Resource
	 */
	public Resource getResource() {
		return resource;
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
	 * Sets the resource of which to list the children.
	 * 
	 * @param resource
	 *            the Sling Resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
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
