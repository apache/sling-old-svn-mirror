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

import java.util.Map;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that sets a default value for a variable */
public class Defaults implements CrankstartCommand {
    public static final String I_DEFAULTS = "defaults";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public String getDescription() {
        return I_DEFAULTS + ": set the default value of a crankstart variable";
    }
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_DEFAULTS.equals(commandLine.getVerb());
    }

    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final String [] parts = commandLine.getQualifier().split(" ");
        final String key = parts[0];
        final StringBuilder sb = new StringBuilder();
        for(int i=1 ; i < parts.length; i++) {
            if(sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(parts[i]);
        }
        final String value = sb.toString();
        crankstartContext.getDefaults().put(key, value);
        log.info("[{}] has default value [{}]", key, value);
    }
}
