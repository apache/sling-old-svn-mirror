/*
 * $Url: $
 * $Id: Capability.java 28983 2007-07-02 14:13:58Z fmeschbe $
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

import java.io.PrintWriter;

abstract class Capability implements Serializable {

    private final String name;
    
    protected Capability(String name) {
        this.name = name;
    }
    
    String getName() {
        return name;
    }
    
    protected void printP(PrintWriter out, String indent, String name, String type, String value) {
        if (name == null || value == null) {
            return;
        }
        
        out.print(indent);
        out.print("  <p n=\"");
        out.print(name);
        out.print("\" ");
        if (type != null) {
            out.print("t=\"=");
            out.print(type);
            out.print("\" ");
        }
        out.print("v=\"");
        out.print(value);
        out.println("\"/>");
    }
}
