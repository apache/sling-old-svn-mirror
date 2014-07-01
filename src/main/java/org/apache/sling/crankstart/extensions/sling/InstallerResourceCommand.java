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
package org.apache.sling.crankstart.extensions.sling;

import java.util.LinkedList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.api.CrankstartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that prepares resources for registration with the Sling installer */
@Component
@Service
public class InstallerResourceCommand implements CrankstartCommand {
    
    public static final String I_INSTALLER_RESOURCE = "sling.installer.resource";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_INSTALLER_RESOURCE.equals(commandLine.getVerb());
    }
    
    public String getDescription() {
        return I_INSTALLER_RESOURCE + ": prepares a resource for registration with the Sling installer";
    }

    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final String resourceRef = commandLine.getQualifier();
        if(resourceRef == null || resourceRef.length() == 0) {
            throw new CrankstartException("Missing command qualifier, required to specifiy the resource to register");
        }
        
        @SuppressWarnings("unchecked")
        List<String> resources = (List<String>)crankstartContext.getAttribute(InstallerRegisterCommand.CONTEXT_ATTRIBUTE_NAME);
        if(resources == null) {
            resources = new LinkedList<String>();
            crankstartContext.setAttribute(InstallerRegisterCommand.CONTEXT_ATTRIBUTE_NAME, resources);
        }
        
        resources.add(resourceRef);
        log.info("Installer resource prepared: {}", resourceRef);
    }
}
