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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.runmode.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A RegexpFilter that is RunMode-aware: accepts names with a 
 *  prefix that matches the supplied regexp, and suffixes that 
 *  match the list of run mode strings provided by a RunMode service.
 */
class RunModeRegexpFilter extends RegexpFilter {
    private final RunMode runMode;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    RunModeRegexpFilter(String regexp, RunMode runMode) {
        super(regexp);
        this.runMode = runMode;
    }
    
    @Override
    boolean accept(final String path) {
        // If path contains dots, remove suffixes starting with
        // dots until path matches regexp, and accept if all suffixes
        // are included in our list of runmodes
        final char DOT = '.';
        boolean result = false;
        
        if(path.indexOf(DOT) > 0) {
            int pos = 0;
            String prefix = path;
            final List<String> modes = new LinkedList<String>();
            while( (pos = prefix.lastIndexOf(DOT)) >= 0) {
                modes.add(prefix.substring(pos + 1));
                prefix = prefix.substring(0, pos);
                if(super.accept(prefix)) {
                    result = true;
                    break;
                }
            }
            
            // If path prefix matches, check that all our runmodes match
            if(result) {
                for(String m : modes) {
                    final String [] toTest = { m };
                    if(!runMode.isActive(toTest)) {
                        result = false;
                        break;
                    }
                }
            }
            
            if(log.isDebugEnabled()) {
                log.debug("accept(" + path + ")=" + result + " (prefix=" + prefix + ", modes=" + modes + ")");
            }
            
        } else {
            result = super.accept(path);
        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ", RunMode=" + runMode;
    }

}
