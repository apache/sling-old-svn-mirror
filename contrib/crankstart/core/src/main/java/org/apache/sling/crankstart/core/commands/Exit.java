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
package org.apache.sling.crankstart.core.commands;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that exits if its qualifier is true.
 *  Used for warmup runs where we just want to create the
 *  initial Sling state.
 */
public class Exit implements CrankstartCommand {
    public static final String I_EXIT = "exit.iftrue";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_EXIT.equals(commandLine.getVerb());
    }

    public String getDescription() {
        return I_EXIT + ": exit if the command qualifier is 'true'";
    }
    
    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        if(Boolean.valueOf(commandLine.getQualifier())) {
            log.info("Qualifier is true, stopping the OSGi framework");
            crankstartContext.getOsgiFramework().stop();
        }
    }
}
