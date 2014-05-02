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
package org.apache.sling.scripting.jsp;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.apache.sling.scripting.jsp.jasper.xmlparser.ParserUtils;
import org.apache.sling.scripting.jsp.jasper.xmlparser.TreeNode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;

/**
 * The <code>SlingTldLocationsCache</code> TODO
 */
public class SlingTldLocationsCache
    extends TldLocationsCache implements BundleListener {

    private static final String TLD_SCHEME = "tld:";

    private final Map<String, TldLocationEntry> tldLocations = new HashMap<String, TldLocationEntry>();

    private ServiceRegistration serviceRegistration;

    private final BundleContext bundleContext;

    public SlingTldLocationsCache(final BundleContext context) {
        this.bundleContext = context;
        context.addBundleListener(this);
        final Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getState() == Bundle.RESOLVED || bundles[i].getState() == Bundle.ACTIVE ) {
                addBundle(bundles[i]);
            }
        }

        Dictionary<String, Object> tldConfigPrinterProperties = new Hashtable<String, Object>();
        tldConfigPrinterProperties.put("felix.webconsole.label", "jsptaglibs");
        tldConfigPrinterProperties.put("felix.webconsole.title", "JSP Taglibs");
        tldConfigPrinterProperties.put("felix.webconsole.configprinter.modes", "always");
        this.serviceRegistration = context.registerService(Object.class.getName(),
            this, tldConfigPrinterProperties);

    }

    public void deactivate(final BundleContext context) {
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
        context.removeBundleListener(this);
    }

    // ---------- Tld Location URL support -------------------------------------

    public void bundleChanged(final BundleEvent event) {
        if ( event.getType() == BundleEvent.RESOLVED ) {
            this.addBundle(event.getBundle());
        } else if ( event.getType() == BundleEvent.UNRESOLVED ) {
            this.removeBundle(event.getBundle());
        }
    }

    URL getTldLocationURL(String tldLocation) {
        if (tldLocation.startsWith(TLD_SCHEME)) {
            tldLocation = tldLocation.substring(TLD_SCHEME.length());

            TldLocationEntry tle;
            synchronized (tldLocations) {
                tle = tldLocations.get(tldLocation);
            }

            if (tle != null) {
                return tle.getTldURL();
            }
        }

        return null;
    }

    // ---------- TldLocationsCache support ------------------------------------

    @Override
    public String[] getLocation(final String uri) throws JasperException {
        synchronized (tldLocations) {
            if (tldLocations.containsKey(uri)) {
                return new String[] { TLD_SCHEME + uri, null };
            }
        }

        return null;
    }

    private void addBundle(final Bundle bundle) {
        // currently only META-INF/*.tld is supported, this should
        // be extended for registration in a Bundle Manifest Header

        final Enumeration<?> entries = bundle.findEntries("META-INF", "*.tld", false);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                final URL taglib = (URL) entries.nextElement();
                final String uri = getUriFromTld(taglib);

                synchronized (tldLocations) {
                    if (uri != null && !tldLocations.containsKey(uri)) {
                        tldLocations.put(uri, new TldLocationEntry(bundle,
                            taglib.getPath()));
                    }
                }
            }
        }
    }

    private void removeBundle(final Bundle bundle) {
        synchronized (tldLocations) {
            final Iterator<Map.Entry<String, TldLocationEntry>> i = tldLocations.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, TldLocationEntry> entry = i.next();
                if (entry.getValue().getBundleId() == bundle.getBundleId()) {
                    i.remove();
                }
            }
        }
    }

    /*
     * Returns the value of the uri element of the given TLD, or null if the
     * given TLD does not contain any such element.
     */
    private String getUriFromTld(final URL resource) {
        InputStream stream = null;
        try {
            stream = resource.openStream();

            // Parse the tag library descriptor at the specified resource path
            TreeNode tld = new ParserUtils().parseXMLDocument(
                resource.toString(), stream);
            TreeNode uri = tld.findChild("uri");
            if (uri != null) {
                String body = uri.getBody();
                if (body != null) {
                    return body;
                }
            }
        } catch (Exception e) {
            // TODO: handle
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    // do nothing
                }
            }
        }

        return null;
    }

    public void printConfiguration(final PrintWriter pw) {
        pw.println("Currently available JSP Taglibs:");
        final SortedMap<String, String> taglibs = new TreeMap<String, String>();

        for (final Map.Entry<String, TldLocationEntry> entry : tldLocations.entrySet()) {
            final long bundleId = entry.getValue().getBundleId();
            final Bundle bundle = bundleContext.getBundle(bundleId);
            if (bundle != null) {
                taglibs.put(entry.getKey(), String.format("%s (%s)", bundle.getSymbolicName(), bundleId));
            } else {
                // really shouldn't happen
                taglibs.put(entry.getKey(), String.format("INVALID BUNDLE ID: %s", bundleId));
            }
        }

        for (final Map.Entry<String, String> entry : taglibs.entrySet()) {
            pw.printf("  %s - %s\n", entry.getKey(), entry.getValue());
        }
    }


    private static final class TldLocationEntry {
        private final long bundleId;

        private final URL tldURL;

        private TldLocationEntry(final Bundle bundle, final String tldPath) {
            this.bundleId = bundle.getBundleId();
            this.tldURL = bundle.getEntry(tldPath);
        }

        long getBundleId() {
            return this.bundleId;
        }

        URL getTldURL() {
            return this.tldURL;
        }
    }
}
