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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;

/** Compute the paths under which to look for scripts for a given
 *  request.
 *  See {#link ScriptSearchPathsBuilderTest} for examples.
 */
public class ScriptSearchPathsBuilder {

    /** hardcoded for now, these could be configurable */ 
    public static final String [] DEFAULT_SCRIPT_SEARCH_PATHS = { "/sling/scripts", "/apps" };

    /** Returns the list of paths where scripts can be found, in order, 
     *  for the given request.
     *  The paths are based on the Request's Resource type, and if the 
     *  request contains selectors, they are used to build subpaths of
     *  the main path, which are searched first. 
     */
    public List<String> getScriptSearchPaths(Resource resource, String [] selectors) throws SlingException {
        if(resource==null) {
            throw new SlingException("Resource is null, cannot build script path");
        }
        if(resource.getResourceType() == null) {
            throw new SlingException("resource.getResourceType()==null, cannot build script path");
        }
        
        final String typePath = resource.getResourceType().replaceAll("\\:","/");
        final List<String> result = new LinkedList<String> ();
        
        // use the default script search paths + the path provided by getDynamicDefaultScriptPath
        final List<String> paths = new ArrayList<String>();
        for(String defaultPath : DEFAULT_SCRIPT_SEARCH_PATHS) {
            paths.add(defaultPath + "/" + typePath.trim());
        }
        // the last path in the list does not take the resource type into account (SLING-125)
        paths.add(getDynamicDefaultScriptPath(resource));

        // and add selector-based paths, which take precedence over the default ones
        for(String basePath : paths) {
            
            // if there are selectors A and B, look for a script first under
            // basePath/A/B, then basePath/A, then basePath
            if(selectors!=null) {
                for(int i=selectors.length - 1; i >= 0; i--) {
                    final StringBuffer sb = new StringBuffer();
                    sb.append(basePath);
                    for(int j=0; j <= i; j++) {
                        sb.append("/");
                        sb.append(selectors[j]);
                    }
                    result.add(sb.toString());
                }
            }
            result.add(basePath);
            
        }
        
        return result;
    }
    
    /** Transform the Resource URI into a search path for scripts.
     *  For example, a resource with URI=/content/blog/a/b/c returns
     *  /apps/blog - this allows for simple path-based mapping of
     *  resources to scripts
     */
    protected String getDynamicDefaultScriptPath(Resource r) {
        String result = null;
        if(r!=null && r.getURI() != null) {
            final String [] pathFolders = r.getURI().split("/");
            if(pathFolders.length >= 3) {
                result ="/apps/" + pathFolders[2];
            }
        }
        return result;
    }
}
