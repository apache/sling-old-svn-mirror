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

class PackageCapability extends Capability {

    private String packageName;
    private Version version;

    public PackageCapability(Map.Entry entry) {
        super("package");

        this.packageName = (String) entry.getKey();

        String v = (String) ((Map) entry.getValue()).get("version");
        this.version = Version.parseVersion(v);
    }

    public String getPackageName() {
        return this.packageName;
    }

    public Version getVersion() {
        return this.version;
    }

    public void serialize(PrintWriter out, String indent) {
        // <capability name="package">
        // <p n="package" v="org.apache.felix.upnp.extra.controller"/>
        // <p n="version" t="version" v="1.0.0"/>
        // </capability>
        out.print(indent);
        out.println("<capability name=\"package\">");
        this.printP(out, indent, "package", null, this.packageName);
        this.printP(out, indent, "version", "version", this.version.toString());
        out.print(indent);
        out.println("</capability>");
    }
}
