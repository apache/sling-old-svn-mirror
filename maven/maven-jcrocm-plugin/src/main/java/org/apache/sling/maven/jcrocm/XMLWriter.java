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
package org.apache.sling.maven.jcrocm;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.codehaus.plexus.util.StringUtils;

public class XMLWriter extends PrintWriter {

    String indent = "";

    XMLWriter(OutputStream descriptorStream, boolean append) throws IOException {
        super(new OutputStreamWriter(descriptorStream, "UTF-8"));

        if (!append) {
            println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
    }

    private void indent() {
        indent += "  ";
    }

    private void outdent() {
        if (indent.length() >= 2) {
            indent = indent.substring(2);
        }
    }

    void printComment(String message) {
        print  (indent);
        println("<!--");
        print  (indent);
        print  ("    ");
        println(message);
        print  (indent);
        println("-->");
    }
    
    void printElementStart(String name, boolean hasAttributes) {
        print(indent);
        print("<");
        print(name);
        if (!hasAttributes) {
            printElementStartClose(false);
        }
        indent();
    }

    void printElementStartClose(boolean isEmpty) {
        if (isEmpty) {
            print(" /");
            outdent();
        }
        println('>');
    }

    void printElementEnd(String name) {
        outdent();
        print(indent);
        print("</");
        print(name);
        println('>');
    }

    void printAttribute(String name, String value) {
        if (!StringUtils.isEmpty(name) && value != null) {
            println();
            print(indent);
            print(' ');
            print(name);
            print("=\"");
            print(value);
            print('"');
        }
    }

    void printAttribute(String name, boolean value) {
        if (value) {
            printAttribute(name, "true");
        }
    }
}
