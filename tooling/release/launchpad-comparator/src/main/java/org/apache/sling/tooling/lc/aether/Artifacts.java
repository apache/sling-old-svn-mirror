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
package org.apache.sling.tooling.lc.aether;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Artifacts {

    private static final Pattern VERSION_NUMBER = Pattern.compile("^(\\d+)(-SNAPSHOT)?");
    
    public static final String launchpadCoordinates(String version) {
        
        Matcher versionMatcher = VERSION_NUMBER.matcher(version);
        
        if ( !versionMatcher.matches()) {
            throw new IllegalArgumentException("Invalid version " + version);
        }
        
        int versionNumber = Integer.parseInt(versionMatcher.group(1));

        
        // versions 6 and 7 used an XML bundle list
        if ( versionNumber < 8 ) {
            return "org.apache.sling:org.apache.sling.launchpad:xml:bundlelist:" + version;
        }
        
        // versions 8 and newer use the provisioning model
        return "org.apache.sling:org.apache.sling.launchpad:txt:slingfeature:" + version;
    }
}
