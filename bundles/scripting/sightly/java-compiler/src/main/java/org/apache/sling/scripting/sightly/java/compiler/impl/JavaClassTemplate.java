/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.java.compiler.impl;

/**
 * Template for generated Java classes.
 */
public class JavaClassTemplate {


    private String classTemplate;

    private static final String MAIN_BODY = "MainBody";
    private static final String CLASS_NAME = "ClassName";
    private static final String PACKAGE_NAME = "PackageName";
    private static final String TEMPLATE_INIT = "SubTemplateMapInit";
    private static final String NAME = "Name";

    private StringBuilder templateInitBuilder = new StringBuilder();

    public JavaClassTemplate(String template) {
        this.classTemplate = template;
    }

    public void writeMainBody(String content) {
        setPart(MAIN_BODY, content);
    }

    public void writeSubTemplate(String name, String content) {
        templateInitBuilder.append(insertPart(NAME, content, name));
    }

    public void setClassName(String name) {
        setPart(CLASS_NAME, name);
    }

    public void setPackageName(String name) {
        setPart(PACKAGE_NAME, name);
    }

    @Override
    public String toString() {
        return insertPart(TEMPLATE_INIT, classTemplate, templateInitBuilder.toString());
    }

    private void setPart(String partName, String content) {
        classTemplate = insertPart(partName, classTemplate, content);
    }

    private String insertPart(String partName, String original, String content) {
        String id = "##" + partName + "##";
        return original.replace(id, content);
    }
}
