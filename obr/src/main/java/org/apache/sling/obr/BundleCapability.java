/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
