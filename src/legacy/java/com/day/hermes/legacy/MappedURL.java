/**
 * $Id: MappedURL.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.legacy;

/**
 * Generalized decomposition of the request’s url.getFile() string :
 * <quote>
 *   <code>handle.some.selector.strings.ext/some/suffix/path.withExtension</code>
 * </quote>
 * <p>
 * This sample file string results in the following results :
 * <dl>
 * <dt>originalURL<dd>same as the complete string above
 * <dt>handle<dd>mapped version of handle
 * <dt>selectors<dd>{ “<code>some</code>”, “<code>selector</code>”,
 * 	“<code>strings</code>” }
 * <dt>selectorString<dd><code>some.selector.strings</code>
 * <dt>extension<dd><code>ext</code>
 * <dt>suffix<dd>/some/suffix/path.withExtension
 * </dl>
 * <p>
 * Deprecated names :
 * <dl>
 * <dt>query<dd>{ “<code>some</code>”, “<code>selector</code>”,
 * 	“<code>strings</code>” }
 * <dt>combinedQuery<dd><code>some.selector.strings</code>
 * </dl>
 *
 * @version $Revision: 1.12 $
 * @author fmeschbe
 * @since coati
 * @audience wad
 */
public interface MappedURL {

    /** The URI String from which this <code>MappedURL</code> was built */
    public String getOriginalURL();

    /** The handle from the original URL after applying mappings. */
    public String getHandle();

    /**
     * The string between the handle and the extension/suffx.
     */
    public String getSelectorString();

    /**
     * The strings between the handle and the extension/suffix, splitted at
     * dots.
     */
    public String[] getSelectors();

    /**
     * The string between the handle and the extension/suffx.
     *
     * @deprecated as of coati, use {@link #getSelectorString()} instead.
     */
    public String getCombinedQuery();

    /**
     * The strings between the handle and the extension/suffix, splitted at
     * dots.
     *
     * @deprecated as of coati, use {@link #getSelectors()} instead.
     */
    public String[] getQuery();

    /**
     * The i-th selector string or <code>null</code> if i&lt;0 or
     * i&gt;getSelectors().length.
     *
     * @param i The index of the selector to return.
     *
     * @return The value of the selector if 0 &lt;= i &lt; getSelectors().length
     *      or <code>null</code> otherwise.
     */
    public String getSelector(int i);

    /**
     * The extension of URI String, this is not the extension of the optional
     * suffix
     */
    public String getExtension();

    /**
     * The suffix, the path element after the handle but before the input fields
     */
    public String getSuffix();

}
