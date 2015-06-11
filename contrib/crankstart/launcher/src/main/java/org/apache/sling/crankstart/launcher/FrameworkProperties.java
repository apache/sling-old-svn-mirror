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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.KeyValueMap;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Get OSGi framework properties from a provisioning model */
public class FrameworkProperties {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Model model;
    private Map<String, String> fprops;
    
    public static final String CRANKSTART_SYSPROP_OVERRIDE_PREFIX = "sling.crankstart.";
    
    public FrameworkProperties(Model m) {
        model = m;
    }
    
    public synchronized Map<String, String> getProperties(FeatureFilter filter) {
        if(fprops == null) {
            fprops = new HashMap<String, String>();
            for(Feature f : model.getFeatures()) {
                if(filter != null && filter.ignoreFeature(f)) {
                    continue;
                }
                for(RunMode rm : f.getRunModes()) {
                    final KeyValueMap<String> settings = rm.getSettings();
                    if(settings.size() > 0) {
                        log.info("Using settings from Feature {}, RunMode {} as framework properties", f.getName(), rm.getNames());
                        for(Map.Entry<String, String> e : settings) {
                            log.info("framework property set from provisioning model: {}={}", e.getKey(), e.getValue());
                            fprops.put(e.getKey(), e.getValue());
                        }
                    }
                }
            }
        }
        return fprops;
    }
}