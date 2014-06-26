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

/** CrankstartCommand that registers a resource with the Sling installer */
@Component
@Service
public class InstallerResourceCommand implements CrankstartCommand {
    
    public static final String I_INSTALLER_RESOURCE = "sling.installer.resource";
    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_INSTALLER_RESOURCE.equals(commandLine.getVerb());
    }
    
    public String getDescription() {
        return I_INSTALLER_RESOURCE + ": register a resource with the Sling installer";
    }

    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final String resourceRef = commandLine.getQualifier();
        final URL url = new URL(resourceRef);
        final BundleContext ctx = crankstartContext.getOsgiFramework().getBundleContext();
        final String serviceClass = OsgiInstaller.class.getName();
        final ServiceReference ref = ctx.getServiceReference(serviceClass);
        if(ref == null) {
            throw new CrankstartException("Installer service not available, cannot register resource (" + serviceClass + ")");
        }
        final OsgiInstaller installer = (OsgiInstaller)ctx.getService(ref);
        try {
            final InputStream stream = new AutoCloseInputStream(url.openStream());
            final String digest = resourceRef;
            final InstallableResource r = new InstallableResource(resourceRef, stream, null, digest, "file", 100);
            installer.registerResources("crankstart", new InstallableResource[] { r });
            log.info("Resource registered with Sling installer: {}", resourceRef);
        } finally {
            ctx.ungetService(ref);
        }
    }
}
