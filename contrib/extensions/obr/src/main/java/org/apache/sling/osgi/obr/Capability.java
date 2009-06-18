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

abstract class Capability implements Serializable {

    private final String name;

    protected Capability(String name) {
        this.name = name;
    }

    String getName() {
        return this.name;
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
