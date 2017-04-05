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
package org.apache.sling.crankstart.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.provisioning.model.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunModeFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<String> activeRunModes;
    public static final String SLING_RUN_MODES = "sling.run.modes";

    RunModeFilter() {
        final String sysProp = System.getProperty(SLING_RUN_MODES, "");
        activeRunModes = new ArrayList<String>(Arrays.asList(sysProp.split(",")));
        log.info("Active run modes: {}", activeRunModes);
    }
    
    public boolean runModeActive(RunMode m) {
        // A RunMode is active if all its names are active
        final String [] names = m.getNames();
        if(names == null || names.length == 0) {
            return true;
        }
        
        boolean active = true;
        for(String name : names) {
            active &= activeRunModes.contains(name);
        }
        return active;
    }
}