/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.content.jcr;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;

/**
 * The <code>InvalidQueryException</code> is thrown when trying to create a
 * JCR query failed because the query is invalid. It encapsulates the original
 * <code>javax.jcr.query.InvalidQueryException</code> and shows the same message.
 */
public class InvalidQueryException extends ObjectContentManagerException {

    public InvalidQueryException(javax.jcr.query.InvalidQueryException cause) {
        super(cause.getMessage(), cause);
    }

}
