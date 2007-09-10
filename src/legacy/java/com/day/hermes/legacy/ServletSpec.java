/*
 * $Id: ServletSpec.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import javax.servlet.ServletContext;

/**
 * Currently CQ supports servlet specifications <code>2.2</code> and <code>2.3</code>.
 *
 * @version $Revision: 1.6 $
 * @author dpfister
 * @since antbear
 */
public class ServletSpec {

    /** Version 2.2 */
    public static final ServletSpec V2_2 = new ServletSpec(2,2);

    /** Version 2.3 */
    public static final ServletSpec V2_3 = new ServletSpec(2,3);

    /** Version 2.4 */
    public static final ServletSpec V2_4 = new ServletSpec(2,4);

    /** Major version */
    public final int major;

    /** Minor version */
    public final int minor;

    /**
     * Create a new <code>ServletSpec</code>. Since all instances of
     * this class are created statically, we hide the constructor.
     */
    ServletSpec(int major, int minor) {
	this.major = major;
	this.minor = minor;
    }

    /**
     * Parse a servlet spec, given its string representation.
     * @param s string representation
     * @return servlet spec or <code>null</code> if the specification
     *         is unknown/unsupported
     */
    public static ServletSpec parseVersion(String s) {
	if (s.equals("2.2")) {
	    return V2_2;
	} else if (s.equals("2.3")) {
	    return V2_3;
	} else if (s.equals("2.4")) {
	    return V2_4;
	} else {
	    return null;
	}
    }

    /**
     * Return a servlet spec, given the servlet context identifying the
     * major/minor version of the servlet spec implementation.
     * @param context   servlet context
     * @return Servlet specification or <code>null</code> if the specification
     *         is unknown/unsupported
     */
    public static ServletSpec getVersion(ServletContext context) {
	int major = context.getMajorVersion();
	int minor = context.getMinorVersion();

	if (major == 2 && minor == 2) {
	    return V2_2;
        } else if (major == 2 && minor == 3) {
            return V2_3;
        } else if (major == 2 && minor == 4) {
            return V2_4;
	} else {
	    return null;
	}
    }

    /**
     * @see Object#toString
     */
    public String toString() {
	return String.valueOf(major) + "." + String.valueOf(minor);
    }
}
