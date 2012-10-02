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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.osgi.framework.Constants;


@Component
@Service(value=javax.servlet.Servlet.class)
@Properties({
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Apache Sling OSGi Installer Web Console Plugin"),
    @Property(name="felix.webconsole.label", value="osgi-installer"),
    @Property(name="felix.webconsole.title", value="OSGi Installer"),
    @Property(name="felix.webconsole.configprinter.modes", value={"zip", "txt"})
})
@SuppressWarnings("serial")
public class OsgiInstallerWebConsolePlugin extends GenericServlet {

    @Reference
    private InfoProvider installer;

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

    private String getEntityId(final RegisteredResource rsrc, String alias) {
        String id = rsrc.getEntityId();
        final int pos = id.indexOf(':');
        if ( pos != -1 ) {
            id = id.substring(pos + 1);
        }
        return (alias == null ? id : id + '\n' + alias);
    }

    private String getURL(final Resource rsrc) {
        if ( rsrc.getVersion() != null ) {
            return rsrc.getURL() + " (" + rsrc.getVersion() + ")";
        }
        return rsrc.getURL();
    }

    private String getState(final Resource rsrc) {
        String stateInfo = rsrc.getState().toString();
        // INSTALLED state has some variants
        if ( rsrc.getState() == ResourceState.INSTALLED) {
            if (rsrc.getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED) != null ) {
                stateInfo = "EXCLUDED";
            }
            if (rsrc.getAttribute(TaskResource.ATTR_INSTALL_INFO) != null) {
                stateInfo = stateInfo + "(*)";
            }
        }
        return stateInfo;
    }

    private String getInfo(final RegisteredResource rsrc) {
        return rsrc.getDigest() + '/' + String.valueOf(rsrc.getPriority());
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
    public void service(final ServletRequest req, final ServletResponse res)
            throws IOException {
        final PrintWriter pw = res.getWriter();

        final InstallationState state = this.installer.getInstallationState();
        pw.print("<p class='statline ui-state-highlight'>Apache Sling OSGi Installer");
        if ( state.getActiveResources().size() == 0 && state.getInstalledResources().size() == 0 && state.getUntransformedResources().size() == 0 ) {
            pw.print(" - no resources registered.");
        }
        pw.print("</p>");

        String rt = null;
        for(final ResourceGroup group : state.getActiveResources()) {
            final Resource toActivate = group.getResources().get(0);
            if ( !toActivate.getType().equals(rt) ) {
                if ( rt != null ) {
                    pw.println("</tbody></table>");
                }
                pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                pw.printf("<span style='float: left; margin-left: 1em;'>Active Resources - %s</span>", getType(toActivate));
                pw.println("</div>");
                pw.println("<table class='nicetable'><tbody>");
                pw.printf("<tr><th>Entity ID</th><th>Digest/Priority</th><th>URL (Version)</th><th>State</th></tr>");
                rt = toActivate.getType();
            }
            pw.printf("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>",
                    getEntityId(toActivate, group.getAlias()),
                    getInfo(toActivate),
                    getURL(toActivate),
                    toActivate.getState());
        }
        if ( rt != null ) {
            pw.println("</tbody></table>");
        }
        rt = null;

        for(final ResourceGroup group : state.getInstalledResources()) {
            final Collection<Resource> resources = group.getResources();
            if (resources.size() > 0) {
                final Iterator<Resource> iter = resources.iterator();
                final Resource first = iter.next();
                if ( !first.getType().equals(rt) ) {
                    if ( rt != null ) {
                        pw.println("</tbody></table>");
                    }
                    pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                    pw.printf("<span style='float: left; margin-left: 1em;'>Processed Resources - %s</span>", getType(first));
                    pw.println("</div>");
                    pw.println("<table class='nicetable'><tbody>");
                    pw.printf("<tr><th>Entity ID</th><th>Digest/Priority</th><th>URL (Version)</th><th>State</th></tr>");
                    rt = first.getType();
                }
                pw.print("<tr><td>");
                pw.print(getEntityId(first, group.getAlias()));
                pw.print("</td><td>");
                pw.print(getInfo(first));
                pw.print("</td><td>");
                pw.print(getURL(first));
                pw.print("</td><td>");
                pw.print(getState(first));
                if ( first.getState() == ResourceState.INSTALLED ) {
                    final long lastChange = first.getLastChange();
                    if ( lastChange > 0 ) {
                        pw.print("<br/>");
                        pw.print(formatDate(lastChange));
                    }
                }
                pw.print("</td></tr>");
                if ( first.getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED) != null ) {
                    pw.printf("<tr><td></td><td colspan='2'>%s</td><td></td></tr>",
                            first.getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED));
                }
                if ( first.getAttribute(TaskResource.ATTR_INSTALL_INFO) != null ) {
                    pw.printf("<tr><td></td><td colspan='2'>%s</td><td></td></tr>",
                            first.getAttribute(TaskResource.ATTR_INSTALL_INFO));

                }
                while ( iter.hasNext() ) {
                    final Resource resource = iter.next();
                    pw.printf("<tr><td></td><td>%s</td><td>%s</td><td>%s</td></tr>",
                            getInfo(resource),
                            getURL(resource),
                            resource.getState());
                }
            }
        }
        if ( rt != null ) {
            pw.println("</tbody></table>");
        }

        rt = null;
        for(final RegisteredResource registeredResource : state.getUntransformedResources()) {
            if ( !registeredResource.getType().equals(rt) ) {
                if ( rt != null ) {
                    pw.println("</tbody></table>");
                }
                pw.println("<div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px;'>");
                pw.printf("<span style='float: left; margin-left: 1em;'>Untransformed Resources - %s</span>", getType(registeredResource));
                pw.println("</div>");
                pw.println("<table class='nicetable'><tbody>");
                pw.printf("<tr><th>Digest/Priority</th><th>URL</th></tr>");

                rt = registeredResource.getType();
            }
            pw.printf("<tr><td>%s</td><td>%s</td></tr>",
                    getInfo(registeredResource),
                    registeredResource.getURL());
        }
        if ( rt != null ) {
            pw.println("</tbody></table>");
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
        final InstallationState state = this.installer.getInstallationState();
        pw.println("Active Resources");
        pw.println("----------------");
        String rt = null;
        for(final ResourceGroup group : state.getActiveResources()) {
            final Resource toActivate = group.getResources().get(0);
            if ( !toActivate.getType().equals(rt) ) {
                pw.printf("%s:%n", getType(toActivate));
                rt = toActivate.getType();
            }
            pw.printf("- %s: %s, %s, %s%n",
                    getEntityId(toActivate, group.getAlias()),
                    getInfo(toActivate),
                    getURL(toActivate),
                    toActivate.getState());
        }
        pw.println();

        pw.println("Processed Resources");
        pw.println("-------------------");
        rt = null;
        for(final ResourceGroup group : state.getInstalledResources()) {
            final Collection<Resource> resources = group.getResources();
            if (resources.size() > 0) {
                final Iterator<Resource> iter = resources.iterator();
                final Resource first = iter.next();
                if ( !first.getType().equals(rt) ) {
                    pw.printf("%s:%n", getType(first));
                    rt = first.getType();
                }
                pw.printf("* %s: %s, %s, %s%n",
                        getEntityId(first, group.getAlias()),
                        getInfo(first),
                        getURL(first),
                        getState(first));
                if ( first.getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED) != null ) {
                    pw.printf("  : %s",
                            first.getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED));
                }
                if ( first.getAttribute(TaskResource.ATTR_INSTALL_INFO) != null ) {
                    pw.printf("  : %s",
                            first.getAttribute(TaskResource.ATTR_INSTALL_INFO));

                }
                while ( iter.hasNext() ) {
                    final Resource resource = iter.next();
                    pw.printf("  - %s, %s, %s%n",
                            getInfo(resource),
                            getURL(resource),
                            resource.getState());
                }
            }
        }
        pw.println();

        pw.println("Untransformed Resources");
        pw.println("-----------------------");
        rt = null;
        for(final RegisteredResource registeredResource : state.getUntransformedResources()) {
            if ( !registeredResource.getType().equals(rt) ) {
                pw.printf("%s:%n", getType(registeredResource));
                rt = registeredResource.getType();
            }
            pw.printf("- %s, %s%n",
                    getInfo(registeredResource),
                    registeredResource.getURL());
        }
    }
}
