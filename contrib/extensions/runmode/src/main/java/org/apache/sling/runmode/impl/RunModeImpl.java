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
package org.apache.sling.runmode.impl;

import org.apache.sling.runmode.RunMode;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RunMode service that sets the current run modes from a
 *  BundleContext property when activated.
 *  
 *  BundleContext properties can be set from system properties,
 *  that's one way of defining the current set of run modes.
 *        
 *  @scr.component
 *      metatype="no"
 *      label="Sling RunMode service"
 *      description="RunMode service configured from a BundleContext property"
 *      immediate="true"
 *
 *  @scr.service
 */
public class RunModeImpl implements RunMode {

    private String [] runModes;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public RunModeImpl() {
    }
    
    RunModeImpl(String str) {
        runModes = parseRunModes(str);
    }
    
    protected void activate(ComponentContext context) {
        runModes = parseRunModes(context.getBundleContext().getProperty(RUN_MODES_SYSTEM_PROPERTY));
        log.info("{}, set from BundleContext property '{}'", toString(), RUN_MODES_SYSTEM_PROPERTY);
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(": ");
        for(int i=0; i < runModes.length; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(runModes[i]);
        }
        return sb.toString();
    }

    public String[] getCurrentRunModes() {
        final String [] result = new String[runModes.length];
        System.arraycopy(runModes, 0, result, 0, runModes.length);
        return result;
    }

    public boolean isActive(String[] runModesToCheck) {
        boolean result = false;
        
        mainLoop:
        for(String m : runModesToCheck) {
            m = m.trim();
            if(m.equals(RUN_MODE_WILDCARD)) {
                result = true;
                break mainLoop;
            }
            for(int i = 0; i < runModes.length; i++) {
                if(m.equals(runModes[i])) {
                    result = true;
                    break mainLoop;
                }
            }
        }
        
        return result;
    }
    
    /** Parse str as a comma-separated list of run modes */
    private String [] parseRunModes(String str) {
        if(str == null || str.trim().length() == 0) {
            return new String[0];
        }
        
        final String [] result = str.split(",");
        for(int i=0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }
    
}
