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
package org.apache.sling.launchpad.webapp.integrationtest.installer;

import static org.junit.Assert.fail;

import java.util.List;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Installer test, converted to teleported tests from
 *  the previous installer-duplicate.jsp test script
 */
public class ServerSideInstallerTest {
    private InfoProvider ip;
    private InstallationState is;
    
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");

    @Before
    public void setup() throws LoginException {
        ip = teleporter.getService(InfoProvider.class);
        is = ip.getInstallationState();
    }
    
    @Test
    public void noUntransformedResources() {
        final List<?> utr = is.getUntransformedResources();
        if(utr.size() > 0) {
            fail("Untransformed resources found: " + utr); 
        }
    }
    
    @Test
    public void noActiveResources() {
        final List<?> ar = is.getActiveResources();
        if(ar.size() > 0) {
            fail("Active resources found: " + ar); 
        }
    }
    
    /** Optionally ignore specific resources, usually
     *  created by other tests. 
     */
    private boolean ignore(String entityId) {
        return entityId.contains("InstallManyBundlesTest");
    }
    
    @Test
    public void noDuplicates() {
        String output = "";
        final List<ResourceGroup> resources = is.getInstalledResources();
        for(final ResourceGroup group : resources) {
            if ( group.getResources().size() > 1 ) {            
                boolean first = true;
                for(final Resource rsrc : group.getResources()) {
                    if(ignore(rsrc.getEntityId())) {
                        continue;
                    }
                    if ( first ) {
                        output += "Duplicate resources for '" + rsrc.getEntityId() + "' : ";
                        first = false;
                    } else {
                        output += ", ";
                    }
                    output += rsrc.getURL();
                }
                if(!output.isEmpty()) {
                    output += "\n";
                }
            }
        }
        if(output.length() > 0) {
            fail(output);
        }
        
    }
}