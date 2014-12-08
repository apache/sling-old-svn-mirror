/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss;


import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * A service providing validators and encoders for XSS protection during the composition of HTML
 * pages.
 * <p/>
 * Note: in general, validators are safer than encoders.  Encoding only ensures that content within
 * the encoded context cannot break out of said context.  It requires that there be a context (for
 * instance, a string context in Javascript), and that damage cannot be done from within the context
 * (for instance, a javascript: URL within a href attribute.
 * <p/>
 * When in doubt, use a validator.
 */
@ProviderType
public interface XSSAPI {

    // =============================================================================================
    // VALIDATORS
    //

    /**
     * Validate a string which should contain an integer, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.
     *
     * @param integer      the source integer
     * @param defaultValue a default value if the source can't be used
     * @return a sanitized integer
     */
    public Integer getValidInteger(String integer, int defaultValue);

    /**
     * Validate a string which should contain a long, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.
     *
     * @param source       the source long
     * @param defaultValue a default value if the source can't be used
     * @return a sanitized integer
     */
    public Long getValidLong(String source, long defaultValue);

    /**
     * Validate a string which should contain a dimension, returning a default value if the source is
     * empty, can't be parsed, or contains XSS risks.  Allows integer dimensions and the keyword "auto".
     *
     * @param dimension    the source dimension
     * @param defaultValue a default value if the source can't be used
     * @return a sanitized dimension
     */
    public String getValidDimension(String dimension, String defaultValue);

    /**
     * Sanitizes a URL for writing as an HTML href or src attribute value.
     *
     * @param url the source URL
     * @return a sanitized URL (possibly empty)
     */
    public String getValidHref(String url);

    /**
     * Validate a Javascript token.  The value must be either a single identifier, a literal number,
     * or a literal string.
     *
     * @param token        the source token
     * @param defaultValue a default value to use if the source doesn't meet validity constraints.
     * @return a string containing a single identifier, a literal number, or a literal string token
     */
    public String getValidJSToken(String token, String defaultValue);

    /**
     * Validate a style/CSS token. Valid CSS tokens are specified at http://www.w3.org/TR/css3-syntax/
     *
     * @param token        the source token
     * @param defaultValue a default value to use if the source doesn't meet validity constraints.
     *
     * @return a string containing sanitized style token
     */
    public String getValidStyleToken(String token, String defaultValue);

    /**
     * Validate a CSS color value. Color values as specified at http://www.w3.org/TR/css3-color/#colorunits
     * are safe and definitively allowed. Vulnerable constructs will be disallowed. Currently known
     * vulnerable constructs include url(...), expression(...), and anything with a semicolon.
     *
     * @param color        the color value to be used.
     * @param defaultColor a default value to use if the input color value doesn't meet validity constraints.
     * @return a string a css color value.
     */
    public String getValidCSSColor(String color, String defaultColor);

    // =============================================================================================
    // ENCODERS
    //

    /**
     * Encodes a source string for HTML element content.
     * DO NOT USE FOR WRITING ATTRIBUTE VALUES!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForHTML(String source);

    /**
     * Encodes a source string for writing to an HTML attribute value.
     * DO NOT USE FOR ACTIONABLE ATTRIBUTES (href, src, event handlers); YOU MUST USE A VALIDATOR FOR THOSE!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForHTMLAttr(String source);

    /**
     * Encodes a source string for XML element content.
     * DO NOT USE FOR WRITING ATTRIBUTE VALUES!
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForXML(String source);

    /**
     * Encodes a source string for writing to an XML attribute value.
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForXMLAttr(String source);

    /**
     * Encodes a source string for writing to JavaScript string content.
     * DO NOT USE FOR WRITING TO ARBITRARY JAVASCRIPT; YOU MUST USE A VALIDATOR FOR THAT.
     * (Encoding only ensures that the source material cannot break out of its context.)
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForJSString(String source);

    /**
     * Encodes a souce string for writing to CSS string content.
     * DO NOT USE FOR WRITING OUT ARBITRARY CSS TOKENS; YOU MUST USE A VALIDATOR FOR THAT!
     * (Encoding only ensures the source string cannot break out of its context.)
     *
     * @param source the input to encode
     * @return an encoded version of the source
     */
    public String encodeForCSSString(String source);


    // =============================================================================================
    // FILTERS
    //

    /**
     * Filters potentially user-contributed HTML to meet the AntiSamy policy rules currently in
     * effect for HTML output (see the XSSFilter service for details).
     *
     * @param source a string containing the source HTML
     * @return a string containing the sanitized HTML
     */
    public String filterHTML(String source);


    // =============================================================================================
    // JCR-based URL MAPPING
    //

    /**
     * Returns an XSSAPI instance capable of mapping resource URLs.
     * EITHER THIS OR THE RESOURCERESOLVER VERSION MUST BE USED WHEN VALIDATING HREFs!
     *
     * @param request the request from which to obtain the {@link org.apache.sling.xss.XSSAPI}
     * @return an XSSAPI service capable of validating hrefs.
     */
    public XSSAPI getRequestSpecificAPI(SlingHttpServletRequest request);

    /**
     * Returns an XSSAPI instance capable of mapping resource URLs.
     * EITHER THIS OR THE REQUEST VERSION MUST BE USED WHEN VALIDATING HREFs!
     *
     * @param resourceResolver the resolver from which to obtain the {@link org.apache.sling.xss.XSSAPI}
     * @return an XSSAPI service capable of validating hrefs.
     */
    public XSSAPI getResourceResolverSpecificAPI(ResourceResolver resourceResolver);

}
