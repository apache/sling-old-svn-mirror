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
package org.apache.sling.acldef.provisioning;

import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.Section;

/** Convert ACL definitions found as text in additional sections of 
 *  a Sling provisioning model to model Configurations.
 *  
 *  These configs will be used by the oak-jcr bundle to execute 
 *  the corresponding access control statements, after waiting for
 *  the required services and content to be available. 
 */
public class AclConfigsProvider {
    /** The default name of our ACL definitions sections */
    public static final String DEFAULT_ACLDEF_SECTION_NAME = "accesscontrol";
    
    /** Name Prefix for property for the created Configurations: text of the ACL definition */
    public static final String PROP_ACLDEFTEXT_PREFIX = "acldef.text.";
    
    private final String configFactoryPid;
    private final String sectionName;

    /** Create a provider with a specific config PID and model section name */
    public AclConfigsProvider(String configFactoryPid, String aclDefSectionName) {
        this.configFactoryPid = configFactoryPid;
        if(configFactoryPid == null || configFactoryPid.trim().length() == 0) {
            throw new IllegalArgumentException("config factory PID is empty");
        }
        sectionName = aclDefSectionName == null ? DEFAULT_ACLDEF_SECTION_NAME : aclDefSectionName;
    }
    
    /** Create a provider with the default model section name */
    public AclConfigsProvider(String configFactoryPid) {
        this(configFactoryPid, null);
    }
    
    /** Create a Configuration with one property for each additional accesscontrol
     *  section found in the supplied model.
     *  The properties are named using the {@link #PROP_ACLDEFTEXT_PREFIX} followed
     *  by an integer index starting at 1. 
     *  Each property contains the full text of the corresponding accesscontrol section,
     *  prefixed by a comment indicating which model and feature provided that section.
     *  
     * @param m Model from which to extract accesscontrol sections
     * @return a Configuration object as described above, null if the Model doesn't contain
     *      any additional sections with our section name.
     */
    public Configuration getAclDefConfigs(Model m) {
        Configuration result = null;
        
        int counter = 1;
        for(Feature f : m.getFeatures()) {
            for(Section s : f.getAdditionalSections(sectionName)) {
                if(result == null) {
                    result = createConfiguration();
                }
                result.getProperties().put(PROP_ACLDEFTEXT_PREFIX + counter++, getCommentedContent(m, f, s));
            }
        }
        return result;
    }
    
    private static String getCommentedContent(Model m, Feature f, Section s) {
        final StringBuilder sb = new StringBuilder()
            .append("# Configuration created from feature ")
            .append(f.getName())
            .append(" of model ")
            .append(m.getLocation())
            .append("\n")
            .append(s.getContents().trim())
        ;
        return sb.toString();
    }
    
    /** Create a Configuration with the appropriate PIDs */
    protected Configuration createConfiguration() {
        return new Configuration(null, configFactoryPid);
    }
}