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
import java.util.Map;

import org.apache.sling.scripting.jsp.jasper.JasperException;
import org.apache.sling.scripting.jsp.jasper.compiler.TldLocationsCache;
import org.apache.sling.scripting.jsp.jasper.xmlparser.ParserUtils;
import org.apache.sling.scripting.jsp.jasper.xmlparser.TreeNode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The <code>SlingTldLocationsCache</code> TODO
 */
public class SlingTldLocationsCache
    extends TldLocationsCache implements TaglibCache {

    private static final String TLD_SCHEME = "tld:";

    private final Map<String, TldLocationEntry> tldLocations = new HashMap<String, TldLocationEntry>();

    public SlingTldLocationsCache(final BundleContext context) {
        final Bundle[] bundles = context.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].getState() == Bundle.RESOLVED || bundles[i].getState() == Bundle.ACTIVE ) {
                addBundle(bundles[i]);
            }
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

    public String[] getLocation(final String uri) throws JasperException {
        synchronized (tldLocations) {
            if (tldLocations.containsKey(uri)) {
                return new String[] { TLD_SCHEME + uri, null };
            }
        }

        return null;
    }

    public void addBundle(Bundle bundle) {
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

    public boolean isBundleUsed(Bundle bundle) {
        synchronized (tldLocations) {
            for(final TldLocationEntry tle : tldLocations.values()) {
                if (tle.getBundleId() == bundle.getBundleId()) {
                    return true;
                }
            }
        }
        return false;
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
