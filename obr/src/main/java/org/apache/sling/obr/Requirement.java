/*
 * $Url: $
 * $Id: Requirement.java 28983 2007-07-02 14:13:58Z fmeschbe $
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
package org.apache.sling.obr;

abstract class Requirement implements Serializable {

    private final String name;
    
    protected Requirement(String name) {
        this.name = name;
    }
    
    String getName() {
        return name;
    }
}
