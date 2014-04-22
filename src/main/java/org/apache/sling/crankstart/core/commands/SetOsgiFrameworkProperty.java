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
import org.apache.sling.crankstart.api.CrankstartContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that logs a message */
public class SetOsgiFrameworkProperty implements CrankstartCommand {
    public static final String I_OSGI_PROPERTY = "osgi.property";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public boolean appliesTo(String commandLine) {
        return commandLine.startsWith(I_OSGI_PROPERTY);
    }

    @Override
    public void execute(CrankstartContext crankstartContext, String commandLine) throws Exception {
        final String args = U.removePrefix(I_OSGI_PROPERTY, commandLine);
        final String [] parts = args.split(" ");
        if(parts.length != 2) {
            log.warn("Invalid OSGi property statement, ignored: [{}]", commandLine);
            return;
        }
        final String key = parts[0].trim();
        final String value = parts[1].trim();
        log.info("Setting OSGI property {}={}", key, value);
        crankstartContext.setOsgiFrameworkProperty(key, value);
    }
}
