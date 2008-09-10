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

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.INSTALLED;
import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.UPDATED;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** OsgiResourceProcessor for Bundles */
public class BundleResourceProcessor implements OsgiResourceProcessor {

    public static final String BUNDLE_EXTENSION = ".jar";
    
    /** {@link Storage} key for the bundle ID */
    public static final String KEY_BUNDLE_ID = "bundle.id";
    
    private final BundleContext ctx;
    private final Map<Long, Bundle> pendingBundles;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    BundleResourceProcessor(BundleContext ctx) {
        this.ctx = ctx;
        pendingBundles = new HashMap<Long, Bundle>();
    }

    public int installOrUpdate(String uri, Map<String, Object> attributes, InputStream data) throws Exception {
        // Update if we already have a bundle id, else install
        Bundle b = null;
        boolean updated = false;
        final Long longId = (Long)attributes.get(KEY_BUNDLE_ID);
        
        if(longId != null) {
            b = ctx.getBundle(longId.longValue());
            if(b == null) {
                log.debug("Bundle having id {} not found, uri {} will be installed instead of updating", longId, uri);
            } else {
                b.update(data);
                updated = true;
            }
        }
        
        if(!updated) {
            b = ctx.installBundle(OsgiControllerImpl.getResourceLocation(uri), data);
            attributes.put(KEY_BUNDLE_ID, new Long(b.getBundleId()));
        }
        
        pendingBundles.put(new Long(b.getBundleId()), b);
        
        return updated ? UPDATED : INSTALLED; 
    }

    public void uninstall(String uri, Map<String, Object> attributes) throws BundleException {
        final Long longId = (Long)attributes.get(KEY_BUNDLE_ID);
        if(longId == null) {
            log.debug("No {} in metadata, bundle cannot be uninstalled");
        } else {
            final Bundle b = ctx.getBundle(longId.longValue());
            if(b == null) {
                log.debug("Bundle having id {} not found, cannot uninstall");
            } else {
                pendingBundles.remove(new Long(b.getBundleId()));
                b.uninstall();
            }
        }
    }

    public boolean canProcess(String uri) {
        return uri.endsWith(BUNDLE_EXTENSION);
    }

    public void processResourceQueue() throws BundleException {
        
        if(pendingBundles.isEmpty()) {
            return;
        }
        
        final Iterator<Long> iter = pendingBundles.keySet().iterator(); 
        while(iter.hasNext()) {
            final Long id = iter.next();
            final Bundle bundle = ctx.getBundle(id.longValue());
            final int state = bundle.getState();
            
            if(bundle == null) {
                log.debug("Bundle id {} not found in processResourceQueue(), removed from pending bundles queue");
                iter.remove();
                
            } else if ((state & Bundle.ACTIVE) > 0) {
                log.info("Bundle {} is already active, removed from pending bundles queue", bundle.getLocation());
                iter.remove();
            
            } else if ((state & Bundle.STARTING) > 0) {
                log.info("Bundle {} is starting.", bundle.getLocation());
                
            } else if ((state & Bundle.STOPPING) > 0) {
                log.info("Bundle {} is stopping.", bundle.getLocation());
                
            } else if ((state & Bundle.UNINSTALLED) > 0) {
                log.info("Bundle {} is uninstalled, removed from pending bundles queue", bundle.getLocation());
                iter.remove();
                
            } else if ((state & Bundle.RESOLVED) > 0) {
                log.info("Bundle {} is resolved, trying to start it.", bundle.getLocation());
                bundle.start();
                
            } else if ((state & Bundle.INSTALLED) > 0) {
                log.info("Bundle {} is installed but not resolved.", bundle.getLocation());
            }
        }
    }
}
