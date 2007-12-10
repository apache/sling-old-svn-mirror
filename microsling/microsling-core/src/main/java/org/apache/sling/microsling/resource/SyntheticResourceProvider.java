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
package org.apache.sling.microsling.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides SyntheticResource objects to the
 *  {@link MicroslingResourceResolver}, based on
 *  (hardcoded for now) regular expressions: if the
 *  request path matches one of these expressions,
 *  a SyntheticResource is created.
 */
public class SyntheticResourceProvider {

    private final Logger log = LoggerFactory.getLogger(SyntheticResourceProvider.class);
    
    /** Default regular expressions, SyntheticResources are
     *  created if the given path matches one of these.
     */
    public static String [] DEFAULT_PATH_REGEXP = {
        "/search(/[^\\.]*)?",   // everything under /search, path=up to last dot
        ".*\\*$"          // everything ending with *
    };
    
    private final List<Pattern> pathPattern = new LinkedList<Pattern>();
    
    SyntheticResourceProvider() throws PatternSyntaxException {
        for(String pattern : DEFAULT_PATH_REGEXP) {
            addPathRegexp(pattern);
        }
    }
    
    Resource getSyntheticResource(String pathInfo) {
        Resource result = null;
        
        final ResourcePathIterator it = new ResourcePathIterator(pathInfo);
        while (it.hasNext() && result == null) {
            final String path = it.next();
            for(Pattern p : pathPattern) {
                if(p.matcher(path).matches()) {
                    result = new SyntheticResource(path,null);
                    if(log.isInfoEnabled()) {
                        log.info("Path matches Pattern " + p + ", returning " + result);
                    }
                    break;
                }
            }
        }
        
        return result;
    }
    
    public void addPathRegexp(String str) throws PatternSyntaxException {
        pathPattern.add(Pattern.compile(str));
    }
    
    public void clearPathRegexps() {
        pathPattern.clear();
    }
}
