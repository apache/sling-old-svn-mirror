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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.apache.sling.scripting.jsp.jasper.xmlparser.ParserUtils;
import org.apache.sling.scripting.jsp.jasper.xmlparser.TreeNode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * The <code>SlingTldLocationsCache</code> TODO
 */
public class SlingTldLocationsCache extends TldLocationsCache implements
        BundleListener {

    private static final String TLD_SCHEME = "tld:";

    private Map<String, TldLocationEntry> tldLocations = new HashMap<String, TldLocationEntry>();

    public SlingTldLocationsCache(ServletContext servletContext,
            BundleContext context) {
        super(servletContext);

        context.addBundleListener(this);

        Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getState() == Bundle.ACTIVE) {
                addBundle(bundles[i]);
            }
        }
    }

    void shutdown(BundleContext context) {
        context.removeBundleListener(this);

        synchronized (tldLocations) {
            tldLocations.clear();
        }
    }

    // ---------- Tld Location URL support -------------------------------------

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

    public String[] getLocation(String uri) throws JasperException {
        synchronized (tldLocations) {
            if (tldLocations.containsKey(uri)) {
                return new String[] { TLD_SCHEME + uri, null };
            }
        }

        // TODO: Should we fall back to the original implementation at
        // all ??
        return super.getLocation(uri);
    }

    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED) {
            // find and register new TLD
            addBundle(event.getBundle());

        } else if (event.getType() == BundleEvent.STOPPED) {
            // unregister TLD
            removeBundle(event.getBundle());
        }
    }

    // ---------- internal -----------------------------------------------------

    private void addBundle(Bundle bundle) {
        // currently only META-INF/*.tld is supported, this should
        // be extended for registration in a Bundle Manifest Header

        Enumeration<?> entries = bundle.findEntries("META-INF", "*.tld", false);
        if (entries != null) {
            while (entries.hasMoreElements()) {
                URL taglib = (URL) entries.nextElement();
                String uri = getUriFromTld(taglib);

                synchronized (tldLocations) {
                    if (uri != null && !tldLocations.containsKey(uri)) {
                        tldLocations.put(uri, new TldLocationEntry(bundle,
                            taglib.getPath()));
                    }
                }
            }
        }
    }

    private void removeBundle(Bundle bundle) {
        synchronized (tldLocations) {
            for (Iterator<TldLocationEntry> li = tldLocations.values().iterator(); li.hasNext();) {
                TldLocationEntry tle = li.next();
                if (tle != null
                    && tle.getBundle().getBundleId() == bundle.getBundleId()) {
                    li.remove();
                }
            }
        }
    }

    /*
     * Returns the value of the uri element of the given TLD, or null if the
     * given TLD does not contain any such element.
     */
    private String getUriFromTld(URL resource) {
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

    private static class TldLocationEntry {
        private Bundle bundle;

        private String tldPath;

        private TldLocationEntry(Bundle bundle, String tldPath) {
            this.bundle = bundle;
            this.tldPath = tldPath;
        }

        Bundle getBundle() {
            return bundle;
        }

        String getTldPath() {
            return tldPath;
        }

        URL getTldURL() {
            return bundle.getEntry(tldPath);
        }
    }
}
