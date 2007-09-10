/*
 * $Url: $
 * $Id: PackageRequirement.java 28983 2007-07-02 14:13:58Z fmeschbe $
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

class PackageRequirement extends Requirement {

    private String name;
    private VersionRange version;
    private boolean optional;
    private boolean multiple;
    private boolean extend;
    
    PackageRequirement(Map.Entry entry) {
        super("package");
    
        name = (String) entry.getKey();
        
        Map props = (Map) entry.getValue();
        
        version = new VersionRange((String) props.get("version"));
        optional = "optional".equals(props.get("resolution:"));
        multiple = false;
        extend = false;
        
        // <require extend="false"
        //    filter="(&(package=org.xml.sax)(version>=0.0.0))"
        //    multiple="false"
        //    name="package"
        //    optional="false">
        //  Import package org.xml.sax
        // </require>
    }
    
    public void serialize(PrintWriter out, String indent) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent).append("<require extend=\"").append(extend).append("\" ");
        buf.append("filter=\"").append(getFilter()).append("\" ");
        buf.append("multiple=\"").append(multiple).append("\" ");
        buf.append("name=\"package\" ");
        buf.append("optional=\"").append(isOptional()).append("\">");
        buf.append("Import package ").append(getPackageName()).append("</require>");
        out.println(buf);
    }
    
    /**
     * @return the extend
     */
    boolean isExtend() {
        return extend;
    }

    /**
     * @return the multiple
     */
    boolean isMultiple() {
        return multiple;
    }

    /**
     * @return the optional
     */
    boolean isOptional() {
        return optional;
    }

    /**
     * @return the packageName
     */
    String getPackageName() {
        return name;
    }

    /**
     * @return the version
     */
    String getVersionRange() {
        return version.toString();
    }

    private String getFilter() {
        
        // shortcut for version >= 0.0.0
        if (version.getHigh() == null && Version.emptyVersion.equals(version.getLow())) {
            return "(package=" + getPackageName() + ")";
        }
        
        // "(&(package=org.xml.sax)(version>=0.0.0))"
        StringBuffer buf = new StringBuffer("(&amp;(package=");
        buf.append(getPackageName()).append(")");

        String filter = version.getFilter();
        
        // ISO-encode the filter
        for (int i=0; i < filter.length(); i++) {
            char c = filter.charAt(i);
            switch (c) {
                case '&':
                    buf.append("&amp;");
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                case '>':
                    buf.append("&gt;");
                    break;
                default:
                    buf.append(c);
            }
        }
        
        buf.append(')');
        
        return buf.toString();
    }
}
