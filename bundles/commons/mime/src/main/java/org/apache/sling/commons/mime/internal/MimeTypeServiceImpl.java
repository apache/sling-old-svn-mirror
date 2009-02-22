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
package org.apache.sling.commons.mime.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.mime.MimeTypeService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * The <code>MimeTypeServiceImpl</code> TODO
 *
 * @scr.component immediate="false" metatype="no"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling Servlet"
 * @scr.reference name="MimeTypeProvider"
 *                interface="org.apache.sling.commons.mime.MimeTypeProvider"
 *                cardinality="0..n" policy="dynamic"
 * @scr.service interface="org.apache.sling.commons.mime.MimeTypeService"
 */
public class MimeTypeServiceImpl implements MimeTypeService, BundleListener {

    public static final String MIME_TYPES = "/META-INF/mime.types";

    /** @scr.reference cardinality="0..1" policy="dynamic" */
    private LogService logService;

    private Map<String, String> mimeTab = new HashMap<String, String>();

    private Map<String, String> extensionMap = new HashMap<String, String>();

    private MimeTypeProvider[] typeProviders;

    private List<MimeTypeProvider> typeProviderList = new ArrayList<MimeTypeProvider>();

    /**
     * @see org.apache.sling.commons.mime.MimeTypeService#getMimeType(java.lang.String)
     */
    public String getMimeType(String name) {
        if (name == null) {
            return null;
        }

        String ext = name.substring(name.lastIndexOf('.') + 1);
        ext = ext.toLowerCase();

        String type = this.mimeTab.get(ext);
        if (type == null) {
            MimeTypeProvider[] mtp = this.getMimeTypeProviders();
            for (int i = 0; type == null && i < mtp.length; i++) {
                type = mtp[i].getMimeType(ext);
            }
        }

        return type;
    }

    /**
     * @see org.apache.sling.commons.mime.MimeTypeService#getExtension(java.lang.String)
     */
    public String getExtension(String mimeType) {
        if (mimeType == null) {
            return null;
        }

        // compare using lowercase only
        mimeType = mimeType.toLowerCase();

        String ext = this.extensionMap.get(mimeType);
        if (ext == null) {
            MimeTypeProvider[] mtp = this.getMimeTypeProviders();
            for (int i = 0; ext == null && i < mtp.length; i++) {
                ext = mtp[i].getExtension(mimeType);
            }
        }
        return ext;
    }

    public void registerMimeType(String mimeType, String... extensions) {
        if (mimeType == null || mimeType.length() == 0 || extensions == null
            || extensions.length == 0) {
            return;
        }

        mimeType = mimeType.toLowerCase();

        String defaultExtension = null;
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] != null && extensions[i].length() > 0) {
                extensions[i] = extensions[i].toLowerCase();

                this.mimeTab.put(extensions[i], mimeType);

                if (defaultExtension == null) {
                    defaultExtension = extensions[i];
                }
            }
        }

        if (defaultExtension != null) {
            this.extensionMap.put(mimeType, defaultExtension);
        }
    }

    public void registerMimeType(InputStream mimeTabStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            mimeTabStream, "ISO-8859-1"));

        String line;
        while ((line = br.readLine()) != null) {

            // ignore comment lines
            if (line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                String[] extensions = new String[parts.length - 1];
                System.arraycopy(parts, 1, extensions, 0, extensions.length);
                this.registerMimeType(parts[0], extensions);
            }
        }
    }

    // ---------- internal -----------------------------------------------------

    private MimeTypeProvider[] getMimeTypeProviders() {
        MimeTypeProvider[] list = this.typeProviders;

        if (list == null) {
            synchronized (this.typeProviderList) {
                this.typeProviders = this.typeProviderList.toArray(new MimeTypeProvider[this.typeProviderList.size()]);
                list = this.typeProviders;
            }
        }

        return list;
    }

    private void handleBundle(Bundle bundle) {
        URL mimes = bundle.getEntry(MIME_TYPES);
        if (mimes != null) {
            InputStream ins = null;
            try {
                ins = mimes.openStream();
                this.registerMimeType(ins);
            } catch (IOException ioe) {
                // log but don't actually care
                this.log(LogService.LOG_WARNING, "An error occurred reading "
                    + mimes, ioe);
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }
    }

    private void log(int level, String message, Throwable t) {
        LogService log = this.logService;
        if (log != null) {
            log.log(level, message, t);
        } else {
            PrintStream out = (level == LogService.LOG_ERROR)
                    ? System.err
                    : System.out;
            out.println(message);
            if (t != null) {
                t.printStackTrace(out);
            }
        }
    }

    // ---------- SCR implementation -------------------------------------------

    protected void activate(ComponentContext context) {
        context.getBundleContext().addBundleListener(this);

        // register maps of existing bundles
        Bundle[] bundles = context.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.RESOLVED | Bundle.STARTING
                | Bundle.ACTIVE | Bundle.STOPPING)) != 0) {
                this.handleBundle(bundles[i]);
            }
        }
    }

    protected void deactivate(ComponentContext context) {
        context.getBundleContext().removeBundleListener(this);
    }

    protected void bindMimeTypeProvider(MimeTypeProvider mimeTypeProvider) {
        synchronized (this.typeProviderList) {
            this.typeProviderList.add(mimeTypeProvider);
            this.typeProviders = null;
        }
    }

    protected void unbindMimeTypeProvider(MimeTypeProvider mimeTypeProvider) {
        synchronized (this.typeProviderList) {
            this.typeProviderList.remove(mimeTypeProvider);
            this.typeProviders = null;
        }
    }

    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            this.handleBundle(event.getBundle());
        }
    }
}
