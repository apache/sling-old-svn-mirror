/*
 * $Id: URLTranslationInfo.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.contentbus.HierarchyNode;

/**
 * The URLTranslationInfo interface provides information of a translation from
 * a internal href to an external URL. This is usually returned by the
 * {@link URLMapper#externalizeHref} call.
 *
 * @version $Revision: 1.5 $
 * @author tripod
 * @since antbear
 * @audience dev
 */
public interface URLTranslationInfo {

    /**
     * Returns the node that is referenced by the internal href or
     * <code>null</code> of the node could not be determined.
     */
    public HierarchyNode getNode();

    /**
     * Returns the translated href.
     */
    public String getExternalHref();

    /**
     * Returns the original href.
     */
    public String getInternalHref();
}
