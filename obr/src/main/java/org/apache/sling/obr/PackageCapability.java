/*
 * $Url: $
 * $Id: PackageCapability.java 28983 2007-07-02 14:13:58Z fmeschbe $
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
import java.util.Map;

import org.osgi.framework.Version;

class PackageCapability extends Capability {

    private String packageName;
    private Version version;
    
    public PackageCapability(Map.Entry entry) {
        super("package");
        
        packageName = (String) entry.getKey();
        
        String v = (String) ((Map) entry.getValue()).get("version");
        version = Version.parseVersion(v);
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void serialize(PrintWriter out, String indent) {
        // <capability name="package">
        // <p n="package" v="org.apache.felix.upnp.extra.controller"/>
        // <p n="version" t="version" v="1.0.0"/>
        // </capability>
        out.print(indent);
        out.println("<capability name=\"package\">");
        printP(out, indent, "package", null, packageName);
        printP(out, indent, "version", "version", version.toString());
        out.print(indent);
        out.println("</capability>");
    }
}
