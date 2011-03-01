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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.core.impl.Activator;
import org.apache.sling.installer.core.impl.EntityResourceList;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.apache.sling.installer.core.impl.RegisteredResourceImpl;
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
        props.put(Constants.SERVICE_VENDOR, Activator.VENDOR);
        props.put(Constants.SERVICE_DESCRIPTION,
            "OSGi Installer Web Console Plugin");
        return bundleContext.registerService("javax.servlet.Servlet", plugin,
            props);
    }

    private OsgiInstallerWebConsolePlugin(
            final OsgiInstallerImpl installer) {
        this.installer = installer;
    }

    /**
     * Internal class to collect the current state.
     */
    private static final class State {
        public final List<EntityResourceList> activeResources = new ArrayList<EntityResourceList>();
        public final List<EntityResourceList> installedResources = new ArrayList<EntityResourceList>();
        public final List<RegisteredResource> untransformedResources = new ArrayList<RegisteredResource>();
    }

    private static final Comparator<EntityResourceList> COMPARATOR = new Comparator<EntityResourceList>() {

        public int compare(EntityResourceList o1, EntityResourceList o2) {
            RegisteredResource r1 = null;
            RegisteredResource r2 = null;
            if ( o1.getResources().size() > 0 ) {
                r1 = o1.getResources().iterator().next();
            }
            if ( o2.getResources().size() > 0 ) {
                r2 = o2.getResources().iterator().next();
            }
            int result;
            if ( r1 == null && r2 == null ) {
                result = 0;
            } else if ( r1 == null ) {
                result = -1;
            } else if ( r2 == null ) {
                result = 1;
            } else {
                result = r1.getType().compareTo(r2.getType());
                if ( result == 0 ) {
                    result = r1.getEntityId().compareTo(r2.getEntityId());
                }
            }
            return result;
        }

    };

    /**
     * Get the current installer state.
     * This method should be called from within a synchronized block for the resources!
     */
    private State getCurrentState() {
        final State state = new State();

        for(final String entityId : this.installer.getPersistentResourceList().getEntityIds()) {
            final EntityResourceList group = this.installer.getPersistentResourceList().getEntityResourceList(entityId);
            if ( group.getActiveResource() != null ) {
                state.activeResources.add(group);
            } else {
                state.installedResources.add(group);
            }
        }

        Collections.sort(state.activeResources, COMPARATOR);
        Collections.sort(state.installedResources, COMPARATOR);

        state.untransformedResources.addAll(this.installer.getPersistentResourceList().getUntransformedResources());

        return state;
    }

    private String getType(final RegisteredResource rsrc) {
        final String type = rsrc.getType();
        if ( type.equals(InstallableResource.TYPE_BUNDLE) ) {
            return "Bundles";
        } else if ( type.equals(InstallableResource.TYPE_CONFIG) ) {
            return "Configurations";
        } else if ( type.equals(InstallableResource.TYPE_FILE) ) {
            return "Files";
        } else if ( type.equals(InstallableResource.TYPE_PROPERTIES) ) {
            return "Properties";
        }
        return type;
    }

    private String getEntityId(final RegisteredResource rsrc) {
        String id = rsrc.getEntityId();
        final int pos = id.indexOf(':');
        if ( pos != -1 ) {
            id = id.substring(pos + 1);
        }
        return id;
    }

    /** Default date format used. */
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS yyyy-MMM-dd");

    /**
     * Format a date
     */
    private synchronized String formatDate(final long time) {
        if ( time == -1 ) {
            return "-";
        }
        final Date d = new Date(time);
        return dateFormat.format(d);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
    throws IOException {

        final PrintWriter pw = res.getWriter();

        pw.print("<p class='statline ui-state-highlight'>Apache Sling OSGi Installer</p>");
        synchronized ( this.installer.getResourcesLock() ) {
            final State state = this.getCurrentState();

            String rt = null;
            for(final EntityResourceList group : state.activeResources) {
                final TaskResource toActivate = group.getActiveResource();
                if ( !toActivate.getType().equals(rt) ) {
                    if ( rt != null ) {
                        pw.println("</tbody></table>");
                    }
                    pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                    pw.printf("<span style='float: left; margin-left: 1em;'>Active Resources - %s</span>", getType(toActivate));
                    pw.println("</div>");
                    pw.println("<table class='nicetable'><tbody>");
                    pw.printf("<tr><th>Entity ID</th><th>Digest</th><th>URL</th><th>State</th></tr>");
                    rt = toActivate.getType();
                }
                pw.printf("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                        getEntityId(toActivate),
                        toActivate.getDigest(),
                        toActivate.getURL(),
                        toActivate.getState());
            }
            if ( rt != null ) {
                pw.println("</tbody></table>");
            }
            rt = null;

            for(final EntityResourceList group : state.installedResources) {
                final Collection<TaskResource> resources = group.getResources();
                if (resources.size() > 0) {
                    final Iterator<TaskResource> iter = resources.iterator();
                    final TaskResource first = iter.next();
                    if ( !first.getType().equals(rt) ) {
                        if ( rt != null ) {
                            pw.println("</tbody></table>");
                        }
                        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                        pw.printf("<span style='float: left; margin-left: 1em;'>Processed Resources - %s</span>", getType(first));
                        pw.println("</div>");
                        pw.println("<table class='nicetable'><tbody>");
                        pw.printf("<tr><th>Entity ID</th><th>Digest</th><th>URL</th><th>State</th></tr>");
                        rt = first.getType();
                    }
                    pw.print("<tr><td>");
                    pw.print(getEntityId(first));
                    pw.print("</td><td>");
                    pw.print(first.getDigest());
                    pw.print("</td><td>");
                    pw.print(first.getURL());
                    pw.print("</td><td>");
                    pw.print(first.getState());
                    if ( first.getState() == ResourceState.INSTALLED ) {
                        final long lastChange = ((RegisteredResourceImpl)first).getLastChange();
                        if ( lastChange > 0 ) {
                            pw.print("<br/>");
                            pw.print(formatDate(lastChange));
                        }
                    }
                    pw.print("</td></tr>");

                    while ( iter.hasNext() ) {
                        final TaskResource resource = iter.next();
                        pw.printf("<tr><td></td><td>%s</td><td>%s</td><td>%s</td></tr>",
                            resource.getDigest(),
                            resource.getURL(),
                            resource.getState());
                    }
                }
            }
            if ( rt != null ) {
                pw.println("</tbody></table>");
            }

            rt = null;
            for(final RegisteredResource registeredResource : state.untransformedResources) {
                if ( !registeredResource.getType().equals(rt) ) {
                    if ( rt != null ) {
                        pw.println("</tbody></table>");
                    }
                    pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                    pw.printf("<span style='float: left; margin-left: 1em;'>Untransformed Resources - %s</span>", getType(registeredResource));
                    pw.println("</div>");
                    pw.println("<table class='nicetable'><tbody>");
                    pw.printf("<tr><th>Digest</th><th>URL</th></tr>");

                    rt = registeredResource.getType();
                }
                pw.printf("<tr><td>%s</td><td>%s</td></tr>",
                    registeredResource.getDigest(),
                    registeredResource.getURL());
            }
            if ( rt != null ) {
                pw.println("</tbody></table>");
            }
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
        synchronized ( this.installer.getResourcesLock() ) {
            final State state = this.getCurrentState();
            pw.println("Active Resources");
            pw.println("----------------");
            String rt = null;
            for(final EntityResourceList group : state.activeResources) {
                final TaskResource toActivate = group.getActiveResource();
                if ( !toActivate.getType().equals(rt) ) {
                    pw.printf("%s:%n", getType(toActivate));
                    rt = toActivate.getType();
                }
                pw.printf("- %s: %s, %s, %s%n",
                        getEntityId(toActivate),
                        toActivate.getDigest(),
                        toActivate.getURL(),
                        toActivate.getState());
            }
            pw.println();

            pw.println("Processed Resources");
            pw.println("-------------------");
            rt = null;
            for(final EntityResourceList group : state.installedResources) {
                final Collection<TaskResource> resources = group.getResources();
                if (resources.size() > 0) {
                    final Iterator<TaskResource> iter = resources.iterator();
                    final TaskResource first = iter.next();
                    if ( !first.getType().equals(rt) ) {
                        pw.printf("%s:%n", getType(first));
                        rt = first.getType();
                    }
                    pw.printf("* %s: %s, %s, %s%n",
                            getEntityId(first),
                            first.getDigest(),
                            first.getURL(),
                            first.getState());
                    while ( iter.hasNext() ) {
                        final TaskResource resource = iter.next();
                        pw.printf("  - %s, %s, %s%n",
                            resource.getDigest(),
                            resource.getURL(),
                            resource.getState());
                    }
                }
            }
            pw.println();

            pw.println("Untransformed Resources");
            pw.println("-----------------------");
            rt = null;
            for(final RegisteredResource registeredResource : state.untransformedResources) {
                if ( !registeredResource.getType().equals(rt) ) {
                    pw.printf("%s:%n", getType(registeredResource));
                    rt = registeredResource.getType();
                }
                pw.printf("- %s, %s%n",
                        registeredResource.getDigest(),
                        registeredResource.getURL());
            }
        }
    }
}
