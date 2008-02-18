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
package org.apache.sling.osgi.console.web.internal.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.osgi.console.web.ConfigurationPrinter;
import org.apache.sling.osgi.console.web.Render;
import org.apache.sling.osgi.console.web.internal.BaseManagementPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationRender extends BaseManagementPlugin implements Render {

    public static final String NAME = "config";

    public static final String LABEL = "Configuration Status";

    private ServiceTracker cfgPrinterTracker;

    private int cfgPrinterTrackerCount;

    private SortedMap<String, ConfigurationPrinter> configurationPrinters = new TreeMap<String, ConfigurationPrinter>();

    public String getName() {
        return NAME;
    }

    public String getLabel() {
        return LABEL;
    }

    public void render(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        PrintWriter pw = response.getWriter();

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        pw.println("<tr class='content'>");
        pw.println("<th class='content container'>Configuration Details</th>");
        pw.println("</tr>");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>");
        pw.println("<pre>");

        pw.println("*** Date: "
            + SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG,
                SimpleDateFormat.LONG, Locale.US).format(new Date()));
        pw.println();

        this.printSystemProperties(pw);
        this.printRawFrameworkProperties(pw);
        this.printAssemblies(pw);
        this.printBundles(pw);
        this.printServices(pw);
        this.printPreferences(pw);
        this.printConfigurations(pw);

        for (ConfigurationPrinter cp : getConfigurationPrinters()) {
            printConfigurationPrinter(pw, cp);
        }

        pw.println("</pre>");
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("</table>");
    }

    private Collection<ConfigurationPrinter> getConfigurationPrinters() {
        if (cfgPrinterTracker == null) {
            cfgPrinterTracker = new ServiceTracker(getBundleContext(),
                ConfigurationPrinter.SERVICE, null);
            cfgPrinterTracker.open();
            cfgPrinterTrackerCount = -1;
        }

        if (cfgPrinterTrackerCount != cfgPrinterTracker.getTrackingCount()) {
            SortedMap<String, ConfigurationPrinter> cp = new TreeMap<String, ConfigurationPrinter>();
            Object[] services = cfgPrinterTracker.getServices();
            if (services != null) {
                for (Object srv : services) {
                    ConfigurationPrinter cfgPrinter = (ConfigurationPrinter) srv;
                    cp.put(cfgPrinter.getTitle(), cfgPrinter);
                }
            }
            configurationPrinters = cp;
            cfgPrinterTrackerCount = cfgPrinterTracker.getTrackingCount();
        }

        return configurationPrinters.values();
    }

    private void printSystemProperties(PrintWriter pw) {
        pw.println("*** System properties:");

        Properties props = System.getProperties();
        SortedSet<Object> keys = new TreeSet<Object>(props.keySet());
        for (Object key : keys) {
            this.infoLine(pw, null, (String) key, props.get(key));
        }

        pw.println();
    }

    private void printRawFrameworkProperties(PrintWriter pw) {
        pw.println("*** Raw Framework properties:");

        File file = new File(getBundleContext().getProperty("sling.home"),
            "sling.properties");
        if (file.exists()) {
            Properties props = new Properties();
            InputStream ins = null;
            try {
                ins = new FileInputStream(file);
                props.load(ins);
            } catch (IOException ioe) {
                // handle or ignore
            } finally {
                IOUtils.closeQuietly(ins);
            }

            SortedSet<Object> keys = new TreeSet<Object>(props.keySet());
            for (Object key : keys) {
                this.infoLine(pw, null, (String) key, props.get(key));
            }

        } else {
            pw.println("  No Framework properties in " + file);
        }

        pw.println();
    }

    private void printAssemblies(PrintWriter pw) {
        pw.println("*** Assemblies:");

        Bundle[] bundles = getBundleContext().getBundles();
        SortedSet<String> keys = new TreeSet<String>();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getHeaders().get("Assembly-Bundles") != null) {
                keys.add(this.getBundleString(bundles[i], false));
            }
        }

        if (keys.isEmpty()) {
            pw.println("  No Assemblies installed");
        } else {
            for (Iterator<String> ki = keys.iterator(); ki.hasNext();) {
                this.infoLine(pw, null, null, ki.next());
            }
        }

        pw.println();
    }

    private void printBundles(PrintWriter pw) {
        pw.println("*** Bundles:");
        // biz.junginger.freemem.FreeMem (1.3.0) "FreeMem Eclipse Memory
        // Monitor" [Resolved]

        Bundle[] bundles = getBundleContext().getBundles();
        SortedSet<String> keys = new TreeSet<String>();
        for (int i = 0; i < bundles.length; i++) {
            keys.add(this.getBundleString(bundles[i], true));
        }

        for (Iterator<String> ki = keys.iterator(); ki.hasNext();) {
            this.infoLine(pw, null, null, ki.next());
        }

        pw.println();
    }

    private void printServices(PrintWriter pw) {
        pw.println("*** Services:");

        // get the list of services sorted by service ID (ascending)
        SortedMap<Object, ServiceReference> srMap = new TreeMap<Object, ServiceReference>();
        try {
            ServiceReference[] srs = getBundleContext().getAllServiceReferences(
                null, null);
            for (int i = 0; i < srs.length; i++) {
                srMap.put(srs[i].getProperty(Constants.SERVICE_ID), srs[i]);
            }
        } catch (InvalidSyntaxException ise) {
            // should handle, for now just print nothing, actually this is not
            // expected
        }

        for (Iterator<ServiceReference> si = srMap.values().iterator(); si.hasNext();) {
            ServiceReference sr = si.next();

            this.infoLine(pw, null,
                String.valueOf(sr.getProperty(Constants.SERVICE_ID)),
                sr.getProperty(Constants.OBJECTCLASS));
            this.infoLine(pw, "  ", "Bundle", this.getBundleString(
                sr.getBundle(), false));

            Bundle[] users = sr.getUsingBundles();
            if (users != null && users.length > 0) {
                List<String> userString = new ArrayList<String>();
                for (int i = 0; i < users.length; i++) {
                    userString.add(this.getBundleString(users[i], false));
                }
                this.infoLine(pw, "  ", "Using Bundles", userString);
            }

            String[] keys = sr.getPropertyKeys();
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                if (!Constants.SERVICE_ID.equals(keys[i])
                    && !Constants.OBJECTCLASS.equals(keys[i])) {
                    this.infoLine(pw, "  ", keys[i], sr.getProperty(keys[i]));
                }
            }

            pw.println();
        }
    }

    private void printPreferences(PrintWriter pw) {
        pw.println("*** System Preferences:");

        ServiceReference sr = getBundleContext().getServiceReference(
            PreferencesService.class.getName());
        if (sr == null) {
            pw.println("  Preferences Service not registered");
            pw.println();
            return;
        }

        PreferencesService ps = (PreferencesService) getBundleContext().getService(
            sr);
        try {
            this.printPreferences(pw, ps.getSystemPreferences());

            String[] users = ps.getUsers();
            for (int i = 0; users != null && i < users.length; i++) {
                pw.println("*** User Preferences " + users[i] + ":");
                this.printPreferences(pw, ps.getUserPreferences(users[i]));
            }
        } catch (BackingStoreException bse) {
            // todo or not :-)
        } finally {
            getBundleContext().ungetService(sr);
        }
    }

    private void printPreferences(PrintWriter pw, Preferences prefs)
            throws BackingStoreException {

        String[] children = prefs.childrenNames();
        for (int i = 0; i < children.length; i++) {
            this.printPreferences(pw, prefs.node(children[i]));
        }

        String[] keys = prefs.keys();
        for (int i = 0; i < keys.length; i++) {
            this.infoLine(pw, null, prefs.absolutePath() + "/" + keys[i],
                prefs.get(keys[i], null));
        }

        pw.println();
    }

    private void printConfigurations(PrintWriter pw) {
        pw.println("*** Configurations:");

        ServiceReference sr = getBundleContext().getServiceReference(
            ConfigurationAdmin.class.getName());
        if (sr == null) {
            pw.println("  Configuration Admin Service not registered");
        } else {

            ConfigurationAdmin ca = (ConfigurationAdmin) getBundleContext().getService(
                sr);
            try {
                Configuration[] configs = ca.listConfigurations(null);
                if (configs != null && configs.length > 0) {
                    SortedMap<Object, Configuration> sm = new TreeMap<Object, Configuration>();
                    for (int i = 0; i < configs.length; i++) {
                        sm.put(configs[i].getPid(), configs[i]);
                    }

                    for (Iterator<Configuration> mi = sm.values().iterator(); mi.hasNext();) {
                        this.printConfiguration(pw, mi.next());
                    }
                } else {
                    pw.println("  No Configurations available");
                }
            } catch (Exception e) {
                // todo or not :-)
            } finally {
                getBundleContext().ungetService(sr);
            }
        }

        pw.println();
    }

    private void printConfigurationPrinter(PrintWriter pw,
            ConfigurationPrinter cp) {
        pw.println("*** " + cp.getTitle() + ":");
        cp.printConfiguration(pw);
        pw.println();
    }

    private void printConfiguration(PrintWriter pw, Configuration config) {
        this.infoLine(pw, "", "PID", config.getPid());

        if (config.getFactoryPid() != null) {
            this.infoLine(pw, "  ", "Factory PID", config.getFactoryPid());
        }

        String loc = (config.getBundleLocation() != null)
                ? config.getBundleLocation()
                : "Unbound";
        this.infoLine(pw, "  ", "BundleLocation", loc);

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = config.getProperties();
        if (props != null) {
            SortedSet<String> keys = new TreeSet<String>();
            for (Enumeration<String> ke = props.keys(); ke.hasMoreElements();) {
                keys.add(ke.nextElement());
            }

            for (Iterator<String> ki = keys.iterator(); ki.hasNext();) {
                String key = ki.next();
                this.infoLine(pw, "  ", key, props.get(key));
            }
        }

        pw.println();
    }

    private void infoLine(PrintWriter pw, String indent, String label,
            Object value) {
        if (indent != null) {
            pw.print(indent);
        }

        if (label != null) {
            pw.print(label);
            pw.print('=');
        }

        this.printObject(pw, value);

        pw.println();
    }

    private void printObject(PrintWriter pw, Object value) {
        if (value == null) {
            pw.print("null");
        } else if (value.getClass().isArray()) {
            this.printArray(pw, (Object[]) value);
        } else {
            pw.print(value);
        }
    }

    private void printArray(PrintWriter pw, Object[] values) {
        pw.print('[');
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                this.printObject(pw, values[i]);
            }
        }
        pw.print(']');
    }

    private String getBundleString(Bundle bundle, boolean withState) {
        StringBuffer buf = new StringBuffer();

        if (bundle.getSymbolicName() != null) {
            buf.append(bundle.getSymbolicName());
        } else if (bundle.getLocation() != null) {
            buf.append(bundle.getLocation());
        } else {
            buf.append(bundle.getBundleId());
        }

        @SuppressWarnings("unchecked")
        Dictionary<String, String> headers = bundle.getHeaders();
        if (headers.get(Constants.BUNDLE_VERSION) != null) {
            buf.append(" (").append(headers.get(Constants.BUNDLE_VERSION)).append(
                ')');
        }

        if (headers.get(Constants.BUNDLE_NAME) != null) {
            buf.append(" \"").append(headers.get(Constants.BUNDLE_NAME)).append(
                '"');
        }

        if (withState) {
            buf.append(" [");
            switch (bundle.getState()) {
                case Bundle.INSTALLED:
                    buf.append("Installed");
                    break;
                case Bundle.RESOLVED:
                    buf.append("Resolved");
                    break;
                case Bundle.STARTING:
                    buf.append("Starting");
                    break;
                case Bundle.ACTIVE:
                    buf.append("Active");
                    break;
                case Bundle.STOPPING:
                    buf.append("Stopping");
                    break;
                case Bundle.UNINSTALLED:
                    buf.append("Uninstalled");
                    break;
            }
            buf.append(']');
        }

        return buf.toString();
    }

}
