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
package org.apache.sling.testing.junit.rules.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class IgnoreTestsConfig {

    public static final String IGNORE_LIST_PROP = Constants.CONFIG_PROP_PREFIX + "ignorelist";
    public static final String RUN_IGNORE_LIST_PROP = Constants.CONFIG_PROP_PREFIX + "ignorelist.run";

    private final int numberOfIgnoreLists = 3;
    private final boolean runIgnoreList;
    private static IgnoreTestsConfig INSTANCE;
    private Map<String, String> ignoreTokens = new HashMap<String, String>();


    /**
     * @return the singleton config object.
     */
    public static IgnoreTestsConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new IgnoreTestsConfig();
        }
        return INSTANCE;
    }

    /**
     * Recreate the singleton config object.
     */
    public static void reCreate() {
        INSTANCE = new IgnoreTestsConfig();
    }

    private IgnoreTestsConfig() {
        for (int i = 0; i <= numberOfIgnoreLists; i++) {
            StringTokenizer st = new StringTokenizer(System.getProperty(IGNORE_LIST_PROP, ""), ",");
            while (st.hasMoreElements()) {
                String token = st.nextToken();
                String[] pair = token.split(":");

                // Split by ":" and get the ignore (partial) java FQDN and a reason
                // Ex: com.adobe.test.*:GRANITE-4242
                //     com.adobe.test.MyTest
                String ignoreToken = (pair.length > 0) ? pair[0] : "";
                String reason = (pair.length > 1) ? pair[1] : "";

                // Add to ignore token map
                ignoreTokens.put(ignoreToken.trim(), reason.trim());
            }
        }
        this.runIgnoreList = System.getProperty(RUN_IGNORE_LIST_PROP) != null;
    }

    public Match match(String fqdn) {
        if (null == fqdn || "".equals(fqdn)) {
            throw new IllegalArgumentException("The ignore class/method String must not be null or empty");
        }
        String className = StringUtils.substringBefore(fqdn, "#");
        Match match = matchToken(fqdn);
        if (!match.isIgnored() && (fqdn.indexOf('#') > 0)) {
            return matchToken(className);
        } else {
            return match;
        }
    }

    private Match matchToken(String matchToken) {
        if (!runIgnoreList ) {
            // run the tests that are not in the ignorelist
            for (String ignoreToken : ignoreTokens.keySet()) {
                if (asteriskMatch(ignoreToken, matchToken)) {
                    return new Match(true, ignoreTokens.get(ignoreToken));
                }
            }
            return new Match(false);
        } else {
            // run only the ignore list, so reverse logic.
            for (String ignoreToken : ignoreTokens.keySet()) {
                if (asteriskMatch(ignoreToken, matchToken)) {
                    return new Match(false, ignoreTokens.get(ignoreToken));
                }
            }
            return new Match(true, "Running tests in ignorelist only");
        }
    }


    private static String createRegexFromGlob(String glob) {
        String out = "^";
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
                case '*': out += ".*"; break;
                case '?': out += '.'; break;
                case '.': out += "\\."; break;
                case '\\': out += "\\\\"; break;
                default: out += c;
            }
        }
        out += '$';
        return out;
    }

    public static boolean asteriskMatch(String pattern, String text) {
        return text.matches(createRegexFromGlob(pattern));
    }


}
