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
package org.apache.sling.ide.eclipse.ui.internal.console;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.sling.ide.eclipse.ui.console.SlingConsoleFactory;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class SlingConsoleEventListener implements EventHandler {

    private final Object sync = new Object();

    private final Set<Event> delayedEvents = Collections.newSetFromMap(new LinkedHashMap<Event, Boolean>() {
        private static final long serialVersionUID = 1L;
        protected boolean removeEldestEntry(java.util.Map.Entry<Event, Boolean> eldest) {
            return size() >= 500;
        };
    });

    private MessageConsole slingConsole;
    private IConsoleListener listener;

    @Override
    public void handleEvent(Event event) {

        synchronized (sync) {

            initSlingConsole();

            if (slingConsole != null) {
                logEvent(event, slingConsole);
                return;
            }

            delayedEvents.add(event);

            final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

            if (listener == null) {
                listener = new IConsoleListener() {

                    @Override
                    public void consolesRemoved(IConsole[] consoles) {
                        synchronized (sync) {
                            for (IConsole console : consoles) {
                                if (console.equals(slingConsole)) {
                                    slingConsole = null;
                                }
                            }
                        }
                    }

                    @Override
                    public void consolesAdded(IConsole[] consoles) {
                        synchronized (sync) {
                            for (IConsole console : consoles) {
                                if (console.getType().equals(SlingConsoleFactory.CONSOLE_TYPE_SLING)) {
                                    slingConsole = (MessageConsole) console;
                                    synchronized (delayedEvents) {
                                        for (Iterator<Event> it = delayedEvents.iterator(); it.hasNext();) {
                                            logEvent(it.next(), slingConsole);
                                            it.remove();
                                        }
                                    }
                                    consoleManager.removeConsoleListener(listener);
                                    listener = null;
                                    break;
                                }
                            }
                        }
                    }
                };

                consoleManager.addConsoleListener(listener);
            }
        }
    }

    private void initSlingConsole() {
        if (slingConsole == null) {
            final IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
            for (IConsole console : consoleManager.getConsoles()) {
                if (console.getType().equals(SlingConsoleFactory.CONSOLE_TYPE_SLING)) {
                    slingConsole = (MessageConsole) console;
                    break;
                }
            }
        }
    }

    private void logEvent(Event event, MessageConsole console) {

        try (MessageConsoleStream messageStream = console.newMessageStream()) {

            Long start = (Long) event.getProperty(CommandExecutionProperties.TIMESTAMP_START);
            Long end = (Long) event.getProperty(CommandExecutionProperties.TIMESTAMP_END);
            String type = (String) event.getProperty(CommandExecutionProperties.ACTION_TYPE);
            String flags = (String) event.getProperty(CommandExecutionProperties.ACTION_FLAGS);
            String target = (String) event.getProperty(CommandExecutionProperties.ACTION_TARGET);
            String result = (String) event.getProperty(CommandExecutionProperties.RESULT_TEXT);
            Throwable t = (Throwable) event.getProperty(CommandExecutionProperties.RESULT_THROWABLE);

            StringBuilder message = new StringBuilder();
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
            message.append("[").append(format.format(new Date(start))).append("] ").append(type);
            if (flags != null && flags.length() > 0) {
                message.append(" (").append(flags).append(")");
            }
            message.append(" -> ").append(target);
            message.append(" : ").append(result).append(" (").append(end - start).append(" ms)").append('\n');

            messageStream.write(message.toString());
            if (t != null) {
                t.printStackTrace(new PrintStream(messageStream));
            }
        } catch (IOException e) {
            Activator.getDefault().getPluginLogger().warn("Failed writing to the console", e);
        }
    }

}
