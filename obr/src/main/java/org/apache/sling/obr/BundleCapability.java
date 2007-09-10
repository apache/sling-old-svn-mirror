/*
 * $Url: $
 * $Id: BundleCapability.java 28983 2007-07-02 14:13:58Z fmeschbe $
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
import java.util.jar.Attributes;

import org.osgi.framework.Constants;

class BundleCapability extends Capability {

    private String manifestVersion = "1";
    private String presentationName;
    private String symbolicName;
    private String version;
    
    BundleCapability(Attributes attrs) {
        super("bundle");
        
        manifestVersion = attrs.getValue(Constants.BUNDLE_MANIFESTVERSION);
        presentationName = attrs.getValue(Constants.BUNDLE_NAME);
        symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
        version = attrs.getValue(Constants.BUNDLE_VERSION);
    }
    
    public void serialize(PrintWriter out, String indent) {
        // <capability name="bundle">
        // <p n="manifestversion" v="1"/>
        // <p n="presentationname" v="ShellGUIPlugin"/>
        // <p n="symbolicname" v="org.apache.felix.shell.gui.plugin"/>
        // <p n="version" t="version" v="0.8.0.SNAPSHOT"/>
        // </capability>
        out.print(indent);
        out.println("<capability name=\"bundle\">");
        printP(out, indent, "manifestversion", null, manifestVersion);
        printP(out, indent, "presentationname", null, presentationName);
        printP(out, indent, "symbolicname", null, symbolicName);
        printP(out, indent, "version", "version", version);
        out.print(indent);
        out.println("</capability>");
    }

    /**
     * @return the manifestVersion
     */
    String getManifestVersion() {
        return manifestVersion;
    }

    /**
     * @return the presentationName
     */
    String getPresentationName() {
        return presentationName;
    }

    /**
     * @return the symbolicName
     */
    String getSymbolicName() {
        return symbolicName;
    }

    /**
     * @return the version
     */
    String getVersion() {
        return version;
    }
    
}
