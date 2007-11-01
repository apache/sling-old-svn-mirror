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
package org.apache.sling.microsling.scripting.helpers;

import org.apache.sling.microsling.helpers.constants.HttpConstants;

/** Builds the names of script files based on the current input:
 *  <ul>
 *    <li>HTTP request method name</li>
 *    <li>Request selectors, if any</li>
 *    <li>Desired response Content-Type</li>
 *    <li>Desired script extension</li>
 *  </ul>
 *  
 *  See ScriptFilenameBuilderTest for examples.
 *  
 *  Note that names can include partial paths, for example we return
 *  "print/a4/html.js" for a GET request for an html document with
 *  selectors "print.a4".
 */
public class ScriptFilenameBuilder {

    /** @return a name like "html.js" or "print/a4/html.vlt" or "POST.js" */ 
    public String buildScriptFilename(String methodName,String selectors,String contentType,String scriptExtension) {
        final StringBuffer sb = new StringBuffer();
        
        // path before filename:
        // add selectors in front of the filename if any, replacing dots in them by slashes
        // so that print.a4 becomes print/a4/
        if(selectors != null && selectors.length() > 0) {
            sb.append(selectors.toLowerCase().replace('.','/'));
            sb.append('/');
        }
        
        // filename:
        if(methodName==null || methodName.length() == 0) {
            sb.append("NO_METHOD");
            
        } else if(HttpConstants.METHOD_GET.equalsIgnoreCase(methodName)) {
            // for the GET method, use the simplified content-type, lowercased,
            // as the filename.
            // TODO: how to handle HEAD?
            if(contentType == null || contentType.length() == 0) {
                sb.append("NO_CONTENT_TYPE");
            } else {
                // keep only what follows slash in the content-type
                final String [] splitContentType = contentType.split("/");
                sb.append(splitContentType[splitContentType.length - 1].toLowerCase());
            }
            
        } else {
            // for other methods use the method name
            sb.append(methodName.toUpperCase());
        }
        
        // extension: use desired script extension 
        sb.append(".");
        if(scriptExtension == null || scriptExtension.length()==0) {
            sb.append("NO_EXT");
        } else {
            sb.append(scriptExtension.toLowerCase());
        }
        return sb.toString();
    }
    
}
