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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.jsp.taglib.helpers.XSSSupport;
import org.apache.sling.scripting.jsp.taglib.helpers.XSSSupport.ENCODING_MODE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tag for writing properly XSS encoded text to the response using the OWASP
 * ESAPI for supporting a number of encoding modes.
 */
public class EncodeTag extends BodyTagSupport {

	private static final long serialVersionUID = 5673936481350419997L;

	private static final Logger log = LoggerFactory.getLogger(EncodeTag.class);
	private String value;
	private String defaultValue;
	private ENCODING_MODE mode;
	private boolean readBody = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
	 */
	@Override
	public int doEndTag() throws JspException {
		log.trace("doEndTag");

		if (readBody) {
			if (bodyContent != null && bodyContent.getString() != null) {
				String encoded = XSSSupport.encode(bodyContent.getString(),
						mode);
				write(encoded);
			}
		}
		return EVAL_PAGE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.jsp.tagext.BodyTagSupport#doStartTag()
	 */
	@Override
	public int doStartTag() throws JspException {
		int res = SKIP_BODY;
		String unencoded = value;
		if (unencoded == null) {
			unencoded = defaultValue;
		}

		if (unencoded != null) {
			String encoded = XSSSupport.encode(unencoded, mode);
			write(encoded);
		} else {
			readBody = true;
			res = EVAL_BODY_BUFFERED;
		}
		return res;
	}

	/**
	 * @return the default value
	 */
	public String getDefault() {
		return defaultValue;
	}

	/**
	 * @return the mode
	 */
	public String getMode() {
		return mode.toString();
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param defaultValue
	 *            the default value to set
	 */
	public void setDefault(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @param mode
	 *            the mode to set
	 */
	public void setMode(String mode) {
		this.mode = XSSSupport.getEncodingMode(mode);
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Writes the encoded text to the response.
	 * 
	 * @param encoded
	 *            the encoded text to write to the page
	 * @throws JspException
	 */
	private void write(String encoded) throws JspException {
		if (!StringUtils.isEmpty(encoded)) {
			try {
				pageContext.getOut().write(encoded);
			} catch (IOException e) {
				log.error("Exception writing escaped content to page", e);
				throw new JspException(
						"Exception writing escaped content to page", e);
			}
		}
	}
}
