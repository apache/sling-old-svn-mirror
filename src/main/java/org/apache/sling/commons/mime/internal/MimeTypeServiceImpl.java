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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * The <code>MimeTypeServiceImpl</code> is the official implementation of the
 * {@link MimeTypeService} interface.
 */
@Component(metatype = true, label = "%mime.service.name", description = "%mime.service.description")
@Service(MimeTypeService.class)
@Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling MIME Type Service")
@Reference(name = "MimeTypeProvider", referenceInterface = MimeTypeProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class MimeTypeServiceImpl implements MimeTypeService, BundleListener {

    public static final String CORE_MIME_TYPES = "/META-INF/core_mime.types";

    public static final String MIME_TYPES = "/META-INF/mime.types";

    @Property(unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_MIME_TYPES = "mime.types";

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY, policy=ReferencePolicy.DYNAMIC)
    private LogService logService;

    private Map<String, String> mimeTab = new HashMap<String, String>();

    private Map<String, String> extensionMap = new HashMap<String, String>();

    private MimeTypeProvider[] typeProviders;

    private List<MimeTypeProvider> typeProviderList = new ArrayList<MimeTypeProvider>();

    private ServiceRegistration webConsolePluginService;

    // --------- MimeTypeService interface

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

        String defaultExtension = extensionMap.get(mimeType);

        for (String extension : extensions) {
            if (extension != null && extension.length() > 0) {
                extension = extension.toLowerCase();

                String oldMimeType = mimeTab.get(extension);
                if (oldMimeType == null) {

                    log(LogService.LOG_DEBUG, "registerMimeType: Add mapping "
                        + extension + "=" + mimeType, null);

                    this.mimeTab.put(extension, mimeType);

                    if (defaultExtension == null) {
                        defaultExtension = extension;
                    }

                } else {

                    log(LogService.LOG_INFO,
                        "registerMimeType: Ignoring mapping " + extension + "="
                            + mimeType + ": Mapping " + extension + "="
                            + oldMimeType + " already exists", null);

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

            registerMimeType(line);
        }
    }

    // ---------- SCR implementation -------------------------------------------

    protected void activate(ComponentContext context) {
        context.getBundleContext().addBundleListener(this);

        // register core and default sling mime types
        Bundle bundle = context.getBundleContext().getBundle();
        registerMimeType(bundle.getEntry(CORE_MIME_TYPES));
        registerMimeType(bundle.getEntry(MIME_TYPES));

        // register maps of existing bundles
        Bundle[] bundles = context.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.RESOLVED | Bundle.STARTING
                | Bundle.ACTIVE | Bundle.STOPPING)) != 0
                && bundles[i].getBundleId() != bundle.getBundleId()) {
                this.registerMimeType(bundles[i].getEntry(MIME_TYPES));
            }
        }

        // register configuration properties
        String[] configTypes = OsgiUtil.toStringArray(context.getProperties().get(
            PROP_MIME_TYPES));
        if (configTypes != null) {
            for (String configType : configTypes) {
                registerMimeType(configType);
            }
        }

        try {
            MimeTypeWebConsolePlugin plugin = new MimeTypeWebConsolePlugin(this);

            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("felix.webconsole.label", MimeTypeWebConsolePlugin.LABEL);
            props.put("felix.webconsole.title", MimeTypeWebConsolePlugin.TITLE);
            props.put("felix.webconsole.css", MimeTypeWebConsolePlugin.CSS_REFS);

            webConsolePluginService = context.getBundleContext().registerService(
                "javax.servlet.Servlet", plugin, props);
        } catch (Throwable t) {
            // don't care, we thus don't have the console plugin
        }
    }

    protected void deactivate(ComponentContext context) {
        context.getBundleContext().removeBundleListener(this);

        if (webConsolePluginService != null) {
            webConsolePluginService.unregister();
            webConsolePluginService = null;
        }
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

    // ---------- BundleListener ----------------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            this.registerMimeType(event.getBundle().getEntry(MIME_TYPES));
        }
    }

    // ---------- plugin support -----------------------------------------------

    Map<String, String> getMimeMap() {
        return mimeTab;
    }

    Map<String, String> getExtensionMap() {
        return extensionMap;
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

    private void registerMimeType(URL mimetypes) {
        if (mimetypes != null) {
            InputStream ins = null;
            try {
                ins = mimetypes.openStream();
                this.registerMimeType(ins);
            } catch (IOException ioe) {
                // log but don't actually care
                this.log(LogService.LOG_WARNING, "An error occurred reading "
                    + mimetypes, ioe);
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

    /**
     * Splits the <code>line</code> on whitespace an registers the MIME type
     * mappings provided the line contains more than one whitespace separated
     * fields.
     *
     * @throws NullPointerException if <code>line</code> is <code>null</code>.
     */
    private void registerMimeType(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length > 1) {
            String[] extensions = new String[parts.length - 1];
            System.arraycopy(parts, 1, extensions, 0, extensions.length);
            this.registerMimeType(parts[0], extensions);
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

}
