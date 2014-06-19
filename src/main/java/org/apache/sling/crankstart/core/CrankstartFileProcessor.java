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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartConstants;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.crankstart.api.CrankstartException;
import org.apache.sling.crankstart.core.commands.Configure;
import org.apache.sling.crankstart.core.commands.Defaults;
import org.apache.sling.crankstart.core.commands.InstallBundle;
import org.apache.sling.crankstart.core.commands.Log;
import org.apache.sling.crankstart.core.commands.NullCommand;
import org.apache.sling.crankstart.core.commands.SetOsgiFrameworkProperty;
import org.apache.sling.crankstart.core.commands.StartBundles;
import org.apache.sling.crankstart.core.commands.StartFramework;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process a crankstart file */
public class CrankstartFileProcessor implements Callable<Object> {
    private final CrankstartContext crankstartContext = new CrankstartContext();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Map<String, String> defaults = new HashMap<String, String>();
    
    private List<CrankstartCommand> builtinCommands = new ArrayList<CrankstartCommand>();
    private List<CrankstartCommand> extensionCommands = new ArrayList<CrankstartCommand>();
    
    public CrankstartFileProcessor() {
        builtinCommands.add(new InstallBundle());
        builtinCommands.add(new Log());
        builtinCommands.add(new SetOsgiFrameworkProperty());
        builtinCommands.add(new StartBundles());
        builtinCommands.add(new StartFramework());
        builtinCommands.add(new Configure());
        builtinCommands.add(new Defaults(defaults));
        
        // Need a null "classpath" command as our launcher uses it
        // outside of the usual command mechanism - it shouldn't cause
        // an error in the normal processing
        builtinCommands.add(new NullCommand("classpath", "set the classpath used by the Crankstart launcher"));
        
        log.info("{} ready, built-in commands: {}", getClass().getSimpleName(), getDescriptions(builtinCommands));
    }
    
    public Object call() throws Exception {
        final String inputFilename = System.getProperty(CrankstartConstants.CRANKSTART_INPUT_FILENAME);
        if(inputFilename == null) {
            throw new CrankstartException("Missing system property " + CrankstartConstants.CRANKSTART_INPUT_FILENAME);
        }
        process(new FileReader(new File(inputFilename)));
        waitForExit();
        return null;
    }
    
    /** Execute supplied command line with all commands that accept it, and return
     *  the number of commands that did.
     */
    private int execute(CrankstartCommandLine cmdLine, Collection<CrankstartCommand> candidateCommands) throws Exception {
        int executed = 0;
        
        for(CrankstartCommand cmd : candidateCommands) {
            if(cmd.appliesTo(cmdLine)) {
                cmd.execute(crankstartContext, cmdLine);
                executed++;
            }
        }
        return executed;
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
            
            // First try the built-in commands
            // If no command found try existing extension commands
            // If no command found reload extension commands and try them again
            // If no command found fail
            if(execute(cmdLine, builtinCommands) == 0) {
                if(execute(cmdLine, extensionCommands) == 0) {
                    reloadExtensionCommands();
                    if(execute(cmdLine, extensionCommands) == 0) {
                        throw new CrankstartException(
                                "Invalid command '" + cmdLine.getVerb()
                                + "', built-in commands:" + getDescriptions(builtinCommands)
                                + ", extension commands: " + getDescriptions(extensionCommands)
                                );
                    }
                }
            }
            
        }
    }
    
    /** Reload our extension commands from the OSGi framework 
     * @throws InvalidSyntaxException */
    private synchronized void reloadExtensionCommands() throws InvalidSyntaxException {
        if(crankstartContext.getOsgiFramework() == null) {
            return;
        }
        extensionCommands.clear();
        final BundleContext bc = crankstartContext.getOsgiFramework().getBundleContext();
        final ServiceReference [] refs = bc.getServiceReferences(CrankstartCommand.class.getName(), null);
        if(refs != null) {
            for(ServiceReference ref : refs) {
                extensionCommands.add((CrankstartCommand)bc.getService(ref));
            }
        }
        log.info("Reloaded extension commands: {}", getDescriptions(extensionCommands));
    }
    
    public void waitForExit() throws InterruptedException {
        if(crankstartContext.getOsgiFramework() == null) {
            throw new IllegalStateException("OSGi framework not started");
        }
        log.info("Waiting for OSGi framework to exit...");
        crankstartContext.getOsgiFramework().waitForStop(0);
        log.info("OSGi framework exited");
    }
    
    private String getDescriptions(Collection<CrankstartCommand> commands) throws CrankstartException {
        final StringBuilder sb = new StringBuilder();
        for(CrankstartCommand cmd : commands) {
            if(sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(cmd.getDescription());
        }
        if(sb.length() == 0) {
            sb.append("<none>");
        }
        return sb.toString();
    }
}