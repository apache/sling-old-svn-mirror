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

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.api.CrankstartException;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that registers prepared resources with the Sling installer */
@Component
@Service
public class InstallerRegisterCommand implements CrankstartCommand {
    
    public static final String CONTEXT_ATTRIBUTE_NAME = InstallerRegisterCommand.class.getName();
    public static final String I_INSTALLER_REGISTER = "sling.installer.register";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_INSTALLER_REGISTER.equals(commandLine.getVerb());
    }
    
    public String getDescription() {
        return I_INSTALLER_REGISTER + ": register prepared resources with the Sling installer";
    }

    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        @SuppressWarnings("unchecked")
        final List<String> resources = (List<String>)crankstartContext.getAttribute(CONTEXT_ATTRIBUTE_NAME);
        
        final List<InstallableResource> toRegister = new LinkedList<InstallableResource>();
        for(String resourceRef : resources) {
            final URL url = new URL(resourceRef);
            final InputStream stream = new AutoCloseInputStream(url.openStream());
            final String digest = resourceRef;
            toRegister.add(new InstallableResource(resourceRef, stream, null, digest, "file", 100));
        }
        
        if(toRegister.isEmpty()) {
            log.warn("No prepared resources found to register, use {} first", InstallerRegisterCommand.I_INSTALLER_REGISTER);
        } else {
            final String prefix = commandLine.getQualifier();
            if(prefix == null || prefix.length() == 0) {
                throw new CrankstartException("Missing command qualifier, required to specify installer resources prefix");
            }
            final BundleContext ctx = crankstartContext.getOsgiFramework().getBundleContext();
            final String serviceClass = OsgiInstaller.class.getName();
            final ServiceReference ref = ctx.getServiceReference(serviceClass);
            if(ref == null) {
                throw new CrankstartException("Installer service not available, cannot register resource (" + serviceClass + ")");
            }
            final OsgiInstaller installer = (OsgiInstaller)ctx.getService(ref);
            try {
                installer.registerResources(prefix, toRegister.toArray(new InstallableResource[] {}));
                log.info("Registered {} resources with installer, using prefix '{}'", toRegister.size(), prefix);
                resources.clear();
            } finally {
                ctx.ungetService(ref);
            }
        }
        
    }
}
