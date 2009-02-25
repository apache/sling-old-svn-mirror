/*
 * $URL$
 * $Id$
 *
 * Copyright 1997-2007 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.commons.compiler;

/**
 *
 */
public interface ClassWriter {

    /**
     * 
     * @param className
     * @param data
     * @throws Exception
     */
    void write(String className, byte[] data) throws Exception;
}
