package org.apache.sling.commons.html.impl;

import org.ccil.cowan.tagsoup.ElementType;
import org.ccil.cowan.tagsoup.HTMLSchema;

/**
 * Implements support for the anchor tag to contain content. This implementation
 * does not fully support HTML5 as such, it does address the most common issue.
 */
public class HTMLExtendedSchema extends HTMLSchema {

	public HTMLExtendedSchema() {
		super();
		ElementType anchor = getElementType("a");
		anchor.setMemberOf(M_INLINE | M_BLOCKINLINE | M_BLOCK);
	}

}
