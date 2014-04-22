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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.launcher.commands.InstallBundle;
import org.apache.sling.crankstart.launcher.commands.Log;
import org.apache.sling.crankstart.launcher.commands.SetOsgiFrameworkProperty;
import org.apache.sling.crankstart.launcher.commands.StartBundles;
import org.apache.sling.crankstart.launcher.commands.StartFramework;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process a crankstart file */
public class CrankstartFileProcessor {
    private final CrankstartContext crankstartContext = new CrankstartContext();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<CrankstartCommand> commands = new ArrayList<CrankstartCommand>();
    
    public CrankstartFileProcessor() {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        
        commands.add(new InstallBundle());
        commands.add(new Log());
        commands.add(new SetOsgiFrameworkProperty());
        commands.add(new StartBundles());
        commands.add(new StartFramework());
    }
    
    public void process(Reader input) throws IOException, BundleException {
        final BufferedReader r = new BufferedReader(input);
        String line = null;
        while((line = r.readLine()) != null) {
            if(line.length() == 0 || line.startsWith("#")) {
                // ignore comments and blank lines
            } else {
                for(CrankstartCommand c : commands) {
                    if(c.appliesTo(line)) {
                        try {
                            c.execute(crankstartContext, line);
                        } catch(Exception e) {
                            log.warn("Command execution failed", e);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    public void waitForExit() throws InterruptedException {
        if(crankstartContext.getOsgiFramework() == null) {
            throw new IllegalStateException("OSGi framework not started");
        }
        log.info("Waiting for OSGi framework to exit...");
        crankstartContext.getOsgiFramework().waitForStop(0);
        log.info("OSGi framework exited");
    }
}
