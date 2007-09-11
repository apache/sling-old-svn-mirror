/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.content.jcr.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.content.jcr.JcrContentManager;
import org.apache.sling.content.jcr.JcrContentManagerFactory;
import org.apache.sling.content.jcr.internal.loader.Loader;
import org.apache.sling.content.jcr.internal.mapping.PersistenceManagerProviderImpl;
import org.apache.sling.jcr.SlingRepository;
import org.apache.sling.mime.MimeTypeService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;


/**
 * The <code>JcrContentHelper</code> TODO
 *
 * @scr.component immediate="true" label="%content.name"
 *          description="%content.description"
 * @scr.property name="service.description"
 *          value="Sling ContentManagerFactory Implementation"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service interface="org.apache.sling.content.jcr.JcrContentManagerFactory"
 * @author fmeschbe
 */
public class JcrContentHelper implements JcrContentManagerFactory,
        SynchronousBundleListener {

    private ComponentContext componentContext;

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    /**
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private EventAdmin eventAdmin;

    /**
     * @scr.reference cardinality="0..1" policy="dynamic"
     */
    private MimeTypeService mimeTypeService;

    private PersistenceManagerProviderImpl pmProvider;

    private Loader loader;

    // ---------- ContentManagerFactory ----------------------------------------

    /**
     * @throws IllegalStateException If this provider is not operational
     */
    public JcrContentManager getContentManager(Session session) {
        return this.pmProvider.getContentManager(session);
    }

    // ---------- BundleListener -----------------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {
        // TODO: This is synchronous - take care to not block the system !!
        switch (event.getType()) {
            case BundleEvent.INSTALLED:
                // register content and types when the bundle content is
                // available
                this.loader.registerBundle(event.getBundle());
                break;

            case BundleEvent.STARTING: // STARTED:
                // register mappings before the bundle gets activated
                this.pmProvider.registerMapperClient(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove mappings after the bundle has stopped
                this.pmProvider.unregisterMapperClient(event.getBundle());
                break;

            case BundleEvent.UNINSTALLED:
                this.loader.unregisterBundle(event.getBundle());
                break;
        }
    }

    public SlingRepository getRepository() {
        return this.repository;
    }

    // ---------- Event Dispatching on behalf of the loader and PMProvider -----

    public void fireEvent(Bundle sourceBundle, String eventName,
            Map<String, Object> props) {
        // check event admin service, return if not available
        EventAdmin ea = this.eventAdmin;
        if (ea == null) {
            return;
        }

        // get a private copy of the properties
        Dictionary<String, Object> table = new Hashtable<String, Object>(props);

        // service information of this JcrContentHelper service
        ServiceReference sr = this.componentContext.getServiceReference();
        if (sr != null) {
            table.put(EventConstants.SERVICE, sr);
            table.put(EventConstants.SERVICE_ID,
                sr.getProperty(org.osgi.framework.Constants.SERVICE_ID));
            table.put(EventConstants.SERVICE_OBJECTCLASS,
                sr.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
            if (sr.getProperty(org.osgi.framework.Constants.SERVICE_PID) != null) {
                table.put(EventConstants.SERVICE_PID,
                    sr.getProperty(org.osgi.framework.Constants.SERVICE_PID));
            }
        }

        // source bundle information (if available)
        if (sourceBundle != null) {
            table.put(EventConstants.BUNDLE_SYMBOLICNAME,
                sourceBundle.getSymbolicName());
        }

        // timestamp the event
        table.put(EventConstants.TIMESTAMP,
            new Long(System.currentTimeMillis()));

        // create the event
        ea.postEvent(new Event(eventName, table));
    }

    // ---------- Helper -------------------------------------------------------

    public String getMimeType(String name) {
        // local copy to not get NPE despite check for null due to concurrent
        // unbind
        MimeTypeService mts = this.mimeTypeService;
        return (mts != null) ? mts.getMimeType(name) : null;
    }

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        this.componentContext = componentContext;
        this.loader = new Loader(this);
        this.pmProvider = new PersistenceManagerProviderImpl(this);

        componentContext.getBundleContext().addBundleListener(this);

        // TODO: Consider running this in the background !!
        Bundle[] bundles = componentContext.getBundleContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                // load content for bundles which are neither INSTALLED nor
                // UNINSTALLED
                this.loader.registerBundle(bundles[i]);
            }

            if (bundles[i].getState() == Bundle.ACTIVE) {
                // register active bundles with the mapper client
                this.pmProvider.registerMapperClient(bundles[i]);
            }
        }
    }

    protected void deactivate(ComponentContext oldContext) {
        this.componentContext.getBundleContext().removeBundleListener(this);

        this.pmProvider.dispose();
        this.loader.dispose();
        this.componentContext = null;
    }
}
