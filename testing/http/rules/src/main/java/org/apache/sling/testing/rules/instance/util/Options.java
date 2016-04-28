/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.rules.instance.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum Options {

    JAR("instance.jar.file"),
    JVM_ARGUMENTS("instance.vm.args"),
    QUICKSTART_OPTIONS("instance.options"),
    INSTALLATIONS("instance.installations"),
    START_TIMEOUT("instance.timeout");

    private String name;

    Options(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSpecified() {
        return System.getProperty(name) != null;
    }

    public boolean isNotSpecified() {
        return !isSpecified();
    }

    public String asString() {
        return System.getProperty(name);
    }

    public String asPath() {
        String value = asString();

        if (value == null) {
            return null;
        }

        return new File(value).getAbsolutePath();
    }

    public File asFile() {
        String path = asPath();

        if (path == null) {
            return null;
        }

        return new File(path);
    }

    public List<String> asList() {
        List<String> result = new ArrayList<String>();

        String values = asString();

        if (values == null) {
            return result;
        }

        String[] array = values.trim().split("\\s");

        for (String element : array) {
            String trimmed = element.trim();

            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    public Integer asInteger(int defaultValue) {
        String string  = asString();

        if (string == null) {
            return defaultValue;
        }

        return new Integer(asString());
    }

}
