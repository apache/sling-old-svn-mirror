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
package org.apache.sling.ide.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The <tt>IgnoredResources</tt> holds information about what resources are ignored in a local checkout
 */
public class IgnoredResources {

    private List<Pattern> patterns = new ArrayList<>();

    public void registerRegExpIgnoreRule(String root, String pattern) {

        // copied from org.apache.jackrabbit.vault.vlt.meta.Ignored and tweaked
        if (pattern.startsWith("#")) {
            return;
        }
        StringBuilder reg = new StringBuilder("^");
        if (root.equals("/")) {
            root = "";
        }
        reg.append(root).append("/");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                reg.append(".*");
            } else if (c == '?') {
                reg.append(".");
            } else if (c == '.') {
                reg.append("\\.");
            } else {
                reg.append(c);
            }
        }
        reg.append("$");

        patterns.add(Pattern.compile(reg.toString()));
    }

    public boolean isIgnored(String repositoryPath) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(repositoryPath).matches()) {
                return true;
            }
        }
        return false;
    }
}
