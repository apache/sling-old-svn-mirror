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
package org.apache.sling.crankstart.launcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.sling.provisioning.model.Model;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Setup the OSGi framework based on a provisioning model */
@SuppressWarnings("serial")
public class FrameworkSetup extends HashMap<String, Object> implements Callable<Object> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @SuppressWarnings("unchecked")
    private <T> T require(String key, Class<T> desiredType) throws IllegalStateException {
        final Object o = get(key);
        if(o == null) {
            throw new IllegalStateException("Missing required object:" + key);
        }
        if(!o.getClass().isAssignableFrom(desiredType)) {
            throw new ClassCastException("Object '" + key + "' is not a " + desiredType.getName());
        }
        return (T)o;
    }
    
    public Object call() throws Exception {
        final Model model = require(Launcher.MODEL_KEY, Model.class);
        final LauncherListener listener = (LauncherListener) get(Launcher.LISTENER_KEY);
        
        log.info("Setting OSGi framework properties");
        final Map<String, String> fprops = new FrameworkProperties(model).getProperties(null);
                
        log.info("Starting the OSGi framework");
        final FrameworkFactory factory = (FrameworkFactory)getClass().getClassLoader().loadClass("org.apache.felix.framework.FrameworkFactory").newInstance();
        final Framework framework = factory.newFramework(fprops);
        framework.start();
        
        final RunModeFilter rmFilter = new RunModeFilter();
        
        final Configurations cfg = new Configurations(framework.getBundleContext(), model, rmFilter);
        setShutdownHook(framework, new Closeable[] { cfg });
        log.info("OSGi framework started");
        
        log.info("Installing bundles from provisioning model");
        final BundlesInstaller bi = new BundlesInstaller(model, rmFilter);
        final BundleContext bc = framework.getBundleContext();
        bi.installBundles(bc, Launcher.NOT_CRANKSTART_FILTER);
        cfg.maybeConfigure();
        
        // TODO shall we gradually increase start levels like the launchpad does?? Reuse that DefaultStartupHandler code?
        final Bundle [] bundles = bc.getBundles();
        log.info("Starting all bundles ({} bundles installed)", bundles.length);
        int started = 0;
        int failed = 0;
        for(Bundle b : bundles) {
            if(isFragment(b)) {
                started++;
            } else {
                try { 
                    b.start();
                    started++;
                } catch(BundleException be) {
                    failed++;
                    log.warn("Error starting bundle " + b.getSymbolicName(), be);
                }
            }
            cfg.maybeConfigure();
        }
        
        if(failed == 0) {
            log.info("All {} bundles started.", started);
        } else {
            log.info("{} bundles started, {} failed to start, total {}", started, failed, bundles.length);
        }
        
        log.info("OSGi setup done, waiting for framework to stop");
        if ( listener != null) {
            listener.onStartup(started, failed, bundles.length);
        }
        framework.waitForStop(0);
        if ( listener != null) {
            listener.onShutdown();
        }

        return null;
    }
    
    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }
    
    private void setShutdownHook(final Framework osgiFramework, final Closeable ... toClose) {
        // Shutdown the framework when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if(osgiFramework != null && osgiFramework.getState() == Bundle.ACTIVE) {
                    try {
                        log.info("Stopping the OSGi framework");
                        osgiFramework.stop();
                        log.info("Waiting for the OSGi framework to exit");
                        osgiFramework.waitForStop(0);
                        log.info("OSGi framework stopped");
                    } catch(Exception e) {
                        log.error("Exception while stopping OSGi framework", e);
                    } finally {
                        for(Closeable c : toClose) {
                            try {
                                c.close();
                            } catch(IOException ignore) {
                            }
                        }
                    }
                }
            }
        });
    }
}