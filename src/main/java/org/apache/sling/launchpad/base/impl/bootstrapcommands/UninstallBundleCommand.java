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
package org.apache.sling.launchpad.base.impl.bootstrapcommands;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** A Command that uninstalls a bundle, see 
 *  {@link UninstallBundleCommandTest} for examples
 */
class UninstallBundleCommand implements Command {
    
    public static String CMD_PREFIX = "uninstall ";
    private final String bundleSymbolicName;
    private final VersionRange versionRange;

    /** Used to create a prototype object, for parsing */
    UninstallBundleCommand() {
        bundleSymbolicName = null;
        versionRange = null;
    }
    
    /** Used to create an actual command */
    private UninstallBundleCommand(String bundleSymbolicName, String versionRangeStr) {
        this.bundleSymbolicName = bundleSymbolicName;
        
        // If versionRangeStr is not a range, make it strict
        if(!versionRangeStr.contains(",")) {
            versionRangeStr = "[" + versionRangeStr + "," + versionRangeStr + "]";
        }
        this.versionRange = VersionRange.parse(versionRangeStr);
    }
    
    public void execute(Logger logger, BundleContext ctx) throws Exception {
        // Uninstall all instances of our bundle within our version range
        for(Bundle b : ctx.getBundles()) {
            if(b.getSymbolicName().equals(bundleSymbolicName)) {
                if(versionRange.isInRange(b.getVersion())) {
                    logger.log(Logger.LOG_INFO, 
                            this + ": uninstalling bundle version " + b.getVersion());
                    b.uninstall();
                } else {
                    logger.log(Logger.LOG_INFO, 
                            this + ": bundle version (" + b.getVersion()+ " not in range, ignored");
                }
            }
        }
    }

    public Command parse(String commandLine) throws ParseException {
        if(commandLine.startsWith(CMD_PREFIX)) {
            final String [] s = commandLine.split(" ");
            if(s.length == 3) {
                return new UninstallBundleCommand(s[1].trim(), s[2].trim());
            } else {
                throw new Command.ParseException("Syntax error: '" + commandLine + "'");
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + bundleSymbolicName + " " + versionRange;
    }
}