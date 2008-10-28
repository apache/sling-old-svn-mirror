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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.IGNORED;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OsgiController service
 *
 *  @scr.service
 *  @scr.component
 *      immediate="true"
 *      metatype="no"
 *  @scr.property
 *      name="service.description"
 *      value="Sling jcrinstall OsgiController Service"
 *  @scr.property
 *      name="service.vendor"
 *      value="The Apache Software Foundation"
*/
public class OsgiControllerImpl implements OsgiController, Runnable, SynchronousBundleListener {

    private Storage storage;
    private List<OsgiResourceProcessor> processors;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean running;
    private long loopDelay;

    public static final String STORAGE_FILENAME = "controller.storage";

    /** @scr.reference */
    private ConfigurationAdmin configAdmin;

    /** @scr.reference */
    private PackageAdmin packageAdmin;

    /** Storage key: last modified as a Long */
    public static final String KEY_LAST_MODIFIED = "last.modified";

    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;

    protected void activate(ComponentContext context) throws IOException {
        processors = new LinkedList<OsgiResourceProcessor>();
        processors.add(new BundleResourceProcessor(context.getBundleContext(), packageAdmin));
        processors.add(new ConfigResourceProcessor(configAdmin));

        storage = new Storage(context.getBundleContext().getDataFile(STORAGE_FILENAME));
        
        // start queue processing
        running = true;
        final Thread t = new Thread(this, getClass().getSimpleName() + "_" + System.currentTimeMillis());
        t.setDaemon(true);
        t.start();
    }

    protected void deactivate(ComponentContext oldContext) {
        running = false;

        if(storage != null) {
            try {
                storage.saveToFile();
            } catch(IOException ioe) {
                log.warn("IOException in Storage.saveToFile()", ioe);
            }
        }
        
        if (processors != null) {
            for (OsgiResourceProcessor processor : processors) {
                processor.dispose();
            }
        }
        
        storage = null;
        processors = null;
    }
    
    public int installOrUpdate(String uri, long lastModified, InputStream data) throws IOException, JcrInstallException {
        int result = IGNORED;
        final OsgiResourceProcessor p = getProcessor(uri);
        if (p != null) {
            try {
                final Map<String, Object> map = storage.getMap(uri);
                result = p.installOrUpdate(uri, map, data);
                if (result != IGNORED) {
                    map.put(KEY_LAST_MODIFIED, new Long(lastModified));
                }
                storage.saveToFile();
            } catch(IOException ioe) {
                throw ioe;
            } catch(Exception e) {
                throw new JcrInstallException("Exception in installOrUpdate", e);
            }
        }
        return result;
    }

    public void uninstall(String uri) throws JcrInstallException {
        final OsgiResourceProcessor p = getProcessor(uri);
        if(p != null) {
            try {
                p.uninstall(uri, storage.getMap(uri));
                storage.remove(uri);
                storage.saveToFile();
            } catch(Exception e) {
                throw new JcrInstallException("Exception in uninstall", e);
            }
        }
    }

    public Set<String> getInstalledUris() {
        return storage.getKeys();
    }

    /** {@inheritDoc}
     *  @return LAST_MODIFIED_NOT_FOUND if uri not found
     */
    public long getLastModified(String uri) {
        long result = LAST_MODIFIED_NOT_FOUND;

        if(storage.contains(uri)) {
            final Map<String, Object> uriData = storage.getMap(uri);
            final Long lastMod = (Long)uriData.get(KEY_LAST_MODIFIED);
            if(lastMod != null) {
                result = lastMod.longValue();
            }
        }
        return result;
    }

    static String getResourceLocation(String uri) {
        return "jcrinstall://" + uri;
    }

    /** Return the first processor that accepts given uri, null if not found */
    OsgiResourceProcessor getProcessor(String uri) {
        OsgiResourceProcessor result = null;

        if(processors == null) {
            throw new IllegalStateException("Processors are not set");
        }

        for(OsgiResourceProcessor p : processors) {
            if(p.canProcess(uri)) {
                result = p;
                break;
            }
        }

        if(result == null) {
            log.debug("No processor found for resource {}", uri);
        }

        return result;
    }

    /** Schedule our next scan sooner if anything happens to bundles */
    public void bundleChanged(BundleEvent e) {
        //loopDelay = 0;
    }

    /** Process our resource queues at regular intervals, more often if
     *  we received bundle events since the last processing
     */
    public void run() {
        log.info("{} thread {} starts", getClass().getSimpleName(), Thread.currentThread().getName());

        // We could use the scheduler service but that makes things harder to test
        while (running) {
            loopDelay = 1000;
            try {
                for(OsgiResourceProcessor p : processors) {
                    p.processResourceQueue();
                }
            } catch (Exception e) {
                log.warn("Exception in run()", e);
            } finally {
                try {
                    Thread.sleep(loopDelay);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }

        log.info("{} thread {} ends", getClass().getSimpleName(), Thread.currentThread().getName());
    }
}