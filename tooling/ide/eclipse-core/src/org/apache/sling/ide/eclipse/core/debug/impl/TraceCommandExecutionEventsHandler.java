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
package org.apache.sling.ide.eclipse.core.debug.impl;

import java.text.DateFormat;
import java.util.Date;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * The <tt>TraceCommandExecutionEventsHandler</tt> listens to events related to command execution and writes them using
 * the PluginLogger
 *
 */
public class TraceCommandExecutionEventsHandler implements EventHandler {

    private Logger logger;

    @Override
    public void handleEvent(Event event) {

        Long start = (Long) event.getProperty(CommandExecutionProperties.TIMESTAMP_START);
        Long end = (Long) event.getProperty(CommandExecutionProperties.TIMESTAMP_END);
        String type = (String) event.getProperty(CommandExecutionProperties.ACTION_TYPE);
        String flags = (String) event.getProperty(CommandExecutionProperties.ACTION_FLAGS);
        String target = (String) event.getProperty(CommandExecutionProperties.ACTION_TARGET);
        String result = (String) event.getProperty(CommandExecutionProperties.RESULT_TEXT);
        Throwable t = (Throwable) event.getProperty(CommandExecutionProperties.RESULT_THROWABLE);

        // TODO format copied from SlingConsoleEventListener
        StringBuilder message = new StringBuilder();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        message.append("[").append(format.format(new Date(start))).append("] ").append(type);
        if (flags != null && flags.length() > 0) {
            message.append(" (").append(flags).append(")");
        }
        message.append(" -> ").append(target);
        message.append(" : ").append(result).append(" (").append(end - start).append(" ms)");

        logger.trace(message.toString(), t);
    }

    protected void bindLogger(Logger logger) {
        this.logger = logger;
    }

    protected void unbindLogger(Logger logger) {
        if (this.logger == logger)
            this.logger = null;
    }

}
