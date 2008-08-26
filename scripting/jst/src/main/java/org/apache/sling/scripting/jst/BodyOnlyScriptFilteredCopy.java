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
package org.apache.sling.scripting.jst;

import java.util.regex.Pattern;


/** Extends ScriptFilteredCopy by copying only code that was
 *  generated from inside the <body> tag of the template
 */
class BodyOnlyScriptFilteredCopy extends ScriptFilteredCopy {

    private int state = 0;
    private final Pattern bodyStart;
    private final Pattern bodyEnd;
    
    BodyOnlyScriptFilteredCopy() {
        bodyStart = Pattern.compile("^out.write\\(.*<body.*");
        bodyEnd = Pattern.compile("^out.write\\(.*</body.*");
    }
    
    protected boolean copyLine(String line) {
        boolean result = false;
        
        if(state == 0) {
            // <body> not seen yet, go to state 1 if seen
            if(bodyStart.matcher(line).matches()) {
                state = 1;
            }
            
        } else if(state == 1) {
            // if </body>, done copying -> state 2
            if(bodyEnd.matcher(line).matches()) {
                state = 2;
            } else {
                result = true;
            }
        }
        
        return result;
    }

}
