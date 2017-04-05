/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.serversetup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** This is an evolution of the SlingTestBase/JarExecutor 
 *  combination that we had at revision 1201491, used
 *  to control the server side of integration tests.
 *  
 *  This class allows a number of startup and shutdown phases
 *  to be defined, and executes some or all of them in a specified
 *  order, according to a property which lists their names.
 *  
 *  Flexibility in those startup/shutdown phases allows for 
 *  creating test scenarios like automated testing of
 *  system upgrades, where you would for example:
 *  
 *  <pre>
 *  1. Start the old runnable jar
 *  2. Wait for it to be ready
 *  3. Install some bundles and wait for them to be ready
 *  4. Create some content in that version
 *  5. Stop that jar
 *  6. Start the new runnable jar
 *  7. Wait for it to be ready
 *  8. Run tests against that new jar to verify the upgrade 
 *  </pre>
 *
 *  Running the whole thing might take a long time, so when
 *  debugging the upgrade or the tests you might want to 
 *  restart from a state saved at step 5, and only run steps
 *  6 to 8, for example.
 *  
 *  Those steps are SetupPhase objects identified by
 *  their name, and specifying a partial list of names allows you
 *  to run only some of them in a given test run, speeding up
 *  development and troubleshooting as much as possible.
 *  
 *  TODO: the companion samples/integration-tests module 
 *  should be updated to use this class to setup the Sling server
 *  that it tests, instead of the SlingTestBase class that it
 *  currently uses.
 */
public class ServerSetup {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** Context that our SetupPhase objects can use to exchange data */
    private final Map<String, Object> context = new HashMap<String, Object>();
    
    private final List<String> phasesToRun = new ArrayList<String>();
    
    /** Our configuration */
    private Properties config;
    
    /** Prefix used for our property names */
    public static final String PROP_NAME_PREFIX = "server.setup";
    
    /** Config property name: comma-separated list of phases to run */
    public static final String PHASES_TO_RUN_PROP = PROP_NAME_PREFIX + ".phases";
    
    /** Standard suffix for shutdown tasks IDs */
    public static final String SHUTDOWN_ID_SUFFIX = ".shutdown";
    
    /** Our SetupPhases, keyed by their id which must be unique */
    private final Map<String, SetupPhase> phases = new HashMap<String, SetupPhase>();
    
    /** List of phases that already ran */
    private final Set<String> donePhases = new HashSet<String>();
    
    /** List of phases that failed */
    private final Set<String> failedPhases = new HashSet<String>();
    
    /** Context attribute: server access URL */
    public static final String SERVER_BASE_URL = "server.base.url";
    
    /** Shutdown hook thread */
    private Thread shutdownHook;
    
    @SuppressWarnings("serial")
    public static class SetupException extends Exception {
        public SetupException(String reason) {
            super(reason);
        }
        
        public SetupException(String reason, Throwable cause) {
            super(reason, cause);
        }
    };
    
    /** Runs all startup phases that have not run yet,
     *  and throws an Exception or call Junit's fail()
     *  method if one of them fails or failed in a 
     *  previous call of this method.
     *  
     *  This can be called several times, will only run
     *  setup phases that have not run yet.
     */
    public synchronized void setupTestServer() throws Exception {
        
        // On the first call, list our available phases
        if(donePhases.isEmpty()) {
            if(log.isInfoEnabled()) {
                final List<String> ids = new ArrayList<String>();
                ids.addAll(phases.keySet());
                Collections.sort(ids);
                log.info("Will run SetupPhases {} out of {}", phasesToRun, ids);
            }
        }
        
        // Run all startup phases that didn't run yet
        runRemainingPhases(true);
        
        // And setup our shutdown hook
        if(shutdownHook == null) {
            shutdownHook = new Thread(getClass().getSimpleName() + "Shutdown") {
                public void run() {
                    try {
                        shutdown();
                    } catch(Exception e) {
                        log.warn("Exception in shutdown hook", e);
                    }
                    
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            log.info("Shutdown hook added to run shutdown phases");
        }
    }
    
    /** Run phases that haven't run yet */
    private void runRemainingPhases(boolean isStartup) throws Exception {
        final String mode = isStartup ? "startup" : "shutdown";
        
        // In startup mode, fail if any phases failed previously
        // (in shutdown mode it's probably safer to try to run cleanup phases)
        if(isStartup && !failedPhases.isEmpty()) {
            throw new SetupException("Some SetupPhases previously failed: " + failedPhases);
        }
        
        for(String id : phasesToRun) {
            final SetupPhase p = phases.get(id);
            
            if(donePhases.contains(id)) {
                log.debug("SetupPhase ({}) with id {} already ran, ignored", mode, id);
                continue;
            }
            
            if(p == null) {
                log.info("SetupPhase ({}) with id {} not found, ignored", mode, id);
                donePhases.add(id);
                continue;
            }
            
            if(p.isStartupPhase() == isStartup) {
                log.info("Executing {} phase: {}", mode, p); 
                try {
                    p.run(this);
                } catch(Exception e) {
                    failedPhases.add(id);
                    throw e;
                } catch(AssertionFailedError ae) {
                    // Some of our tools throw this, might not to avoid it in the future
                    failedPhases.add(id);
                    throw new Exception("AssertionFailedError in runRemainingPhases", ae);
                } finally {
                    donePhases.add(id);
                }
            }
        }
    }
    
    /** Called by a shutdown hook to run
     *  all shutdown phases, but can also
     *  be called explicitly, each shutdown
     *  phase only runs once anyway.
     */
    public void shutdown() throws Exception {
        runRemainingPhases(false);
    }
    
    /** Return a context that <code>@SetupPhase</code> can use to 
     *  communicate among them and with the outside.
     */
    public Map<String, Object> getContext() {
        return context;
    }
    
    /** Set configuration and reset our lists of phases
     *  that already ran or failed.
     */
    public void setConfig(Properties props) {
        config = props;

        final String str = props.getProperty(PHASES_TO_RUN_PROP);
        phasesToRun.clear();
        final String [] phases = str == null ? new String [] {} : str.split(",");
        for(int i=0 ; i < phases.length; i++) {
            phases[i] = phases[i].trim();
        }
        phasesToRun.addAll(Arrays.asList(phases));
        
        if(phasesToRun.isEmpty()) {
            log.warn("No setup phases defined, {} is empty, is that on purpose?", PHASES_TO_RUN_PROP);
        }
        
        donePhases.clear();
        failedPhases.clear();
    }
    
    /** Return the configuration Properties that were set
     *  by {@link #setConfig}
     */
    public Properties getConfig() {
        return config;
    }
    
    /** Return the IDs of phases that should run */
    public List<String> getPhasesToRun() {
        return Collections.unmodifiableList(phasesToRun);
    }

    /** Add a SetupPhase to our list. Its ID must be
     *  unique in that list.
     */
    public void addSetupPhase(SetupPhase p) throws SetupException {
        if(phases.containsKey(p.getId())) {
            throw new SetupException("A SetupPhase with ID=" + p.getId() + " is already in our list:" + phases.keySet());
        }
        phases.put(p.getId(), p);
    }
}
