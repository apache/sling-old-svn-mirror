/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.apt.parser.internal;

import java.io.Writer;

import org.apache.maven.doxia.module.apt.AptSink;

/** AptSink which outputs (X)HTML */
class CustomAptSink extends AptSink {
    
    CustomAptSink(Writer w) {
        super(w);
    }
    
    @Override
    public void sectionTitle1() {
        write("<h1>");
    }

    @Override
    public void sectionTitle1_() {
        write("</h1>");
    }
    
    @Override
    public void sectionTitle2_() {
        write("</h2>");
    }

    @Override
    public void sectionTitle2() {
        write("<h2>");
    }

    @Override
    public void sectionTitle3_() {
        write("</h3>");
    }

    @Override
    public void sectionTitle3() {
        write("<h3>");
    }

    @Override
    public void sectionTitle4_() {
        write("</h4>");
    }

    @Override
    public void sectionTitle4() {
        write("<h4>");
    }

    @Override
    public void list_() {
        write("</ul>");
    }

    @Override
    public void list() {
        write("<ul>");
    }

    @Override
    public void listItem_() {
        write("</li>");
    }

    @Override
    public void listItem() {
        write("<li>");
    }

    
    @Override
    public void head_() {
    }

    @Override
    public void head() {
    }

    @Override
    public void paragraph_() {
        write("</p>");
    }

    @Override
    public void paragraph() {
        write("<p>");
    }

    @Override
    public void link(String name) {
        write("<a href=\"" + name + "\">");
    }

    @Override
    public void link_() {
        write("</a>");
    }
    
    @Override
    public void horizontalRule() {
        write("<hr/>\n");
    }

}