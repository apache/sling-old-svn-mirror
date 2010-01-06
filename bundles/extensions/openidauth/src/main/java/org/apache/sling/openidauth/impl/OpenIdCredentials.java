/*
 * Copyright 1997-2010 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.openidauth.impl;

import javax.jcr.Credentials;

import com.dyuproject.openid.OpenIdUser;

@SuppressWarnings("serial")
class OpenIdCredentials implements Credentials {

    private final OpenIdUser user;

    public OpenIdCredentials(final OpenIdUser user) {
        this.user = user;
    }

    public OpenIdUser getUser() {
        return user;
    }
}
