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
package org.apache.sling.installer.core.impl.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@SuppressWarnings("serial")
public class OsgiInstallerWebConsolePlugin extends GenericServlet {

    private final OsgiInstallerImpl installer;

    public static ServiceRegistration register(final BundleContext bundleContext,
            final OsgiInstallerImpl installer) {
        final OsgiInstallerWebConsolePlugin plugin = new OsgiInstallerWebConsolePlugin(
                installer);
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", "osgi-installer");
        props.put("felix.webconsole.title", "OSGi Installer");
        props.put("felix.webconsole.configprinter.modes", new String[] {"zip", "txt"});
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_DESCRIPTION,
            "OSGi Installer Web Console Plugin");
        return bundleContext.registerService("javax.servlet.Servlet", plugin,
            props);
    }

    private OsgiInstallerWebConsolePlugin(
            final OsgiInstallerImpl installer) {
        this.installer = installer;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
    throws IOException {

        final PrintWriter pw = res.getWriter();

        pw.println("<h1>Entities</h1>");
        pw.println("<ul>");
        synchronized ( this.installer.getResourcesLock() ) {
            Collection<String> entities = this.installer.getPersistentResourceList().getEntityIds();
            for (String entityId : entities) {
                EntityResourceList erl = this.installer.getPersistentResourceList().getEntityResourceList(entityId);
                TaskResource registeredResource = erl.getActiveResource();
                if (registeredResource != null) {
                    pw.printf("<li>%s: %s, %s, %s%n",
                        registeredResource.getEntityId(),
                        registeredResource.getDigest(),
                        registeredResource.getScheme(),
                        registeredResource.getState());
                }
                Collection<TaskResource> resources = erl.getResources();
                if (resources.size() > 0) {
                    pw.println("<ul>");
                    for (TaskResource resource : resources) {
                        pw.printf("<li>%s: %s, %s, %s</li>%n",
                            resource.getEntityId(), resource.getDigest(),
                            resource.getScheme(), resource.getState());
                    }
                    pw.println("</ul>");
                }
                pw.println("</li>");
            }
            pw.println("</ul>");

            printUnknownResources(pw, this.installer.getPersistentResourceList().getUntransformedResources());
        }
    }

    /**
     * Method for the configuration printer.
     */
    public void printConfiguration(final PrintWriter pw, final String mode) {
        if ( !"zip".equals(mode) && !"txt".equals(mode) ) {
            return;
        }
        pw.println("Apache Sling OSGi Installer");
        pw.println("===========================");
        pw.println("Entities:");
        synchronized ( this.installer.getResourcesLock() ) {
            Collection<String> entities = this.installer.getPersistentResourceList().getEntityIds();
            for (String entityId : entities) {
                EntityResourceList erl = this.installer.getPersistentResourceList().getEntityResourceList(entityId);
                TaskResource registeredResource = erl.getActiveResource();
                if (registeredResource != null) {
                    pw.printf("- %s: %s, %s, %s%n",
                        registeredResource.getEntityId(),
                        registeredResource.getDigest(),
                        registeredResource.getScheme(),
                        registeredResource.getState());
                }
                Collection<TaskResource> resources = erl.getResources();
                if (resources.size() > 0) {
                    for (TaskResource resource : resources) {
                        pw.printf("- %s: %s, %s, %s%n",
                            resource.getEntityId(), resource.getDigest(),
                            resource.getScheme(), resource.getState());
                    }
                }
            }

            pw.println();
            pw.println("Untransformed Resources:");
            for (RegisteredResource registeredResource : this.installer.getPersistentResourceList().getUntransformedResources()) {
                pw.printf("- %s: %s, %s%n",
                    registeredResource.getEntityId(),
                    registeredResource.getDigest(), registeredResource.getScheme());
            }
        }
    }

    private void printUnknownResources(final PrintWriter pw,
            final List<RegisteredResource> unknown) {
        pw.println("<h1>Untransformed Resources</h1>");
        pw.println("<ul>");
        for (RegisteredResource registeredResource : unknown) {
            pw.printf("<li>%s: %s, %s</li>%n",
                registeredResource.getEntityId(),
                registeredResource.getDigest(), registeredResource.getScheme());
        }
        pw.println("</ul>");
    }
}
