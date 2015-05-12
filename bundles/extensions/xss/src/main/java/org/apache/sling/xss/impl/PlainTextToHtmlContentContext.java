/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or
 * more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the
 * Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 ******************************************************************************/
package org.apache.sling.xss.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provides the capability of securing input provided as plain text for
 * HTML output.
 */
public class PlainTextToHtmlContentContext implements XSSFilterRule {

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * @see XSSFilterRule#check(PolicyHandler, String)
     */
    public boolean check(final PolicyHandler policy, final String str) {
        // there's nothing that can't be escaped, so just return true
        return true;
    }

    /**
     * @see XSSFilterRule#filter(PolicyHandler, java.lang.String)
     */
    public String filter(final PolicyHandler policy, final String str) {
        final String cleaned = escapeXml(str);
        log.debug("Protecting (plain text -> HTML) :\n{}\nto\n{}", str, cleaned);
        return cleaned;
    }

    private static String escapeXml(final String input) {
        if (input == null) {
            return null;
        }

        final StringBuilder b = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '&') {
                b.append("&amp;");
            } else if (c == '<') {
                b.append("&lt;");
            } else if (c == '>') {
                b.append("&gt;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * @see XSSFilterRule#supportsPolicy()
     */
    public boolean supportsPolicy() {
        return false;
    }
}
