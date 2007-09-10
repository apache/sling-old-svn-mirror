/*
 * $Id: DeliveryPageIteratorWrapper.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import com.day.hermes.contentbus.Page;
import com.day.hermes.contentbus.PageIterator;

import java.util.NoSuchElementException;

/**
 * The <code>DeliveryPageIteratorWrapper</code> wraps a ContentBus {@link PageIterator} and
 * returns a {@link DeliveryPageWrapper} object whenever {@link #nextPage} is called.
 *
 * @version $Revision: 1.3 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author mreutegg
 * @since echidna
 * @audience core
 */
class DeliveryPageIteratorWrapper implements PageIterator {

    /** The original ContentBus <code>PageIterator</code>. */
    private PageIterator delegatee = null;

    /** The response object for the dependency registration */
    private DeliveryHttpServletResponse response = null;

    /**
     * Constructs a new DeliveryPageIteratorWrapper.
     * @param it The original <code>PageIterator</code>.
     * @param res The response object for the dependency registration
     */
    public DeliveryPageIteratorWrapper(PageIterator it, DeliveryHttpServletResponse res) {
        delegatee = it;
        response = res;
    }

    public boolean hasNext() {
        return delegatee.hasNext();
    }

    public Object next() {
        return this.nextPage();
    }

    public Page nextPage() throws NoSuchElementException {
        return new DeliveryPageWrapper(delegatee.nextPage(), response);
    }

    public void remove() {
        delegatee.remove();
    }
}
