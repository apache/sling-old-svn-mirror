/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.osgi.obr;

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

        this.name = (String) entry.getKey();

        Map props = (Map) entry.getValue();

        this.version = new VersionRange((String) props.get("version"));
        this.optional = "optional".equals(props.get("resolution:"));
        this.multiple = false;
        this.extend = false;

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
        buf.append(indent).append("<require extend=\"").append(this.extend).append("\" ");
        buf.append("filter=\"").append(this.getFilter()).append("\" ");
        buf.append("multiple=\"").append(this.multiple).append("\" ");
        buf.append("name=\"package\" ");
        buf.append("optional=\"").append(this.isOptional()).append("\">");
        buf.append("Import package ").append(this.getPackageName()).append("</require>");
        out.println(buf);
    }

    /**
     * @return the extend
     */
    boolean isExtend() {
        return this.extend;
    }

    /**
     * @return the multiple
     */
    boolean isMultiple() {
        return this.multiple;
    }

    /**
     * @return the optional
     */
    boolean isOptional() {
        return this.optional;
    }

    /**
     * @return the packageName
     */
    String getPackageName() {
        return this.name;
    }

    /**
     * @return the version
     */
    String getVersionRange() {
        return this.version.toString();
    }

    private String getFilter() {

        // shortcut for version >= 0.0.0
        if (this.version.getHigh() == null && Version.emptyVersion.equals(this.version.getLow())) {
            return "(package=" + this.getPackageName() + ")";
        }

        // "(&(package=org.xml.sax)(version>=0.0.0))"
        StringBuffer buf = new StringBuffer("(&amp;(package=");
        buf.append(this.getPackageName()).append(")");

        String filter = this.version.getFilter();

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
