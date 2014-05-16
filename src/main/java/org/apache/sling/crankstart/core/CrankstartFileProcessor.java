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
package org.apache.sling.crankstart.core;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartConstants;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.core.commands.Configure;
import org.apache.sling.crankstart.core.commands.Defaults;
import org.apache.sling.crankstart.core.commands.InstallBundle;
import org.apache.sling.crankstart.core.commands.Log;
import org.apache.sling.crankstart.core.commands.SetOsgiFrameworkProperty;
import org.apache.sling.crankstart.core.commands.StartBundles;
import org.apache.sling.crankstart.core.commands.StartFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process a crankstart file */
public class CrankstartFileProcessor implements Callable<Object> {
    private final CrankstartContext crankstartContext = new CrankstartContext();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<CrankstartCommand> commands = new ArrayList<CrankstartCommand>();
    private Map<String, String> defaults = new HashMap<String, String>();
    
    public CrankstartFileProcessor() {
        commands.add(new InstallBundle());
        commands.add(new Log());
        commands.add(new SetOsgiFrameworkProperty());
        commands.add(new StartBundles());
        commands.add(new StartFramework());
        commands.add(new Configure());
        commands.add(new Defaults(defaults));
    }
    
    public Object call() throws Exception {
        final String inputFilename = System.getProperty(CrankstartConstants.CRANKSTART_INPUT_FILENAME);
        if(inputFilename == null) {
            throw new IllegalStateException("Missing system property " + CrankstartConstants.CRANKSTART_INPUT_FILENAME);
        }
        process(new FileReader(new File(inputFilename)));
        waitForExit();
        return null;
    }
    
    public void process(Reader input) throws Exception {
        final CrankstartParserImpl parser = new CrankstartParserImpl() {
            @Override
            protected String getVariable(String name) {
                String result = System.getProperty(name);
                if(result == null) {
                    result = defaults.get(name);
                }
                if(result == null) {
                    result = super.getVariable(name); 
                }
                if(result != null) {
                    result = result.trim();
                }
                return result;
            }
        };
        final Iterator<CrankstartCommandLine> it = parser.parse(input);
        while(it.hasNext()) {
            final CrankstartCommandLine cmdLine = it.next();
            for(CrankstartCommand c : commands) {
                if(c.appliesTo(cmdLine)) {
                    c.execute(crankstartContext, cmdLine);
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
