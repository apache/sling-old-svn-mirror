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

import java.io.IOException;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;

interface Command {

    @SuppressWarnings("serial")
    static class ParseException extends IOException {
        ParseException(String reason) {
            super(reason);
        }
    };

    /** Try to parse given command line
     * @return null if we don't know the specified command
     * @throws ParseException if we know the command but syntax is wrong
     */
    Command parse(String commandLine) throws ParseException;

    /**
     * Execute this command.
     * @return Return true if system bundle needs a restart.
     */
    boolean execute(Logger logger, BundleContext ctx) throws Exception;
}
