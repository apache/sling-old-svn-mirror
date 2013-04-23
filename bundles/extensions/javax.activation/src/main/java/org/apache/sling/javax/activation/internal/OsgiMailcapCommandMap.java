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
package org.apache.sling.javax.activation.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.CommandInfo;
import javax.activation.CommandMap;
import javax.activation.DataContentHandler;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.activation.registries.MailcapFile;

/**
 * The <tt>OsgiMailcapCommandMap</tt> is a <tt>CommandMap</tt> which ensures that {@link DataCommandHandler} classes are
 * loaded by their containing bundles.
 * 
 * <p>
 * This allows the javax.activation bundle to obey classloading contraints in an OSGi environment, while preserving most
 * of the functionality available in an unmodified version of the bundle. Notably, this implementation does not support
 * loading <tt>mailcap</tt> files which are not placed inside a bundle.
 * </p>
 * 
 */
public class OsgiMailcapCommandMap extends CommandMap {

    private static final Logger log = LoggerFactory.getLogger(OsgiMailcapCommandMap.class);

    private final Map<Bundle, MailcapFile> db = new HashMap<Bundle, MailcapFile>();
    private final Object sync = new Object();

    public void addMailcapEntries(InputStream mailcapFile, Bundle originatingBundle) throws IOException {

        synchronized (sync) {
            db.put(originatingBundle, new MailcapFile(mailcapFile));
        }

        log.debug("Added mailcap entries from bundle {}", originatingBundle);
    }

    public void removeMailcapEntriesForBundle(Bundle bundle) {

        boolean removed;
        synchronized (sync) {
            removed = db.remove(bundle) != null;
        }

        if (removed) {
            log.debug("Removed mailcap entries from bundle {}", bundle);
        }
    }

    @Override
    public CommandInfo[] getPreferredCommands(String mimeType) {

        List<CommandInfo> commands = new ArrayList<CommandInfo>();

        if (mimeType != null) {
            mimeType = mimeType.toLowerCase(Locale.ENGLISH);
        }

        synchronized (sync) {
            getPreferredCommands(mimeType, commands, false);
            getPreferredCommands(mimeType, commands, true);
        }

        return commands.toArray(new CommandInfo[commands.size()]);
    }

    private void getPreferredCommands(String mimeType, List<CommandInfo> accumulator, boolean fallback) {

        for (Map.Entry<Bundle, MailcapFile> entry : db.entrySet()) {
            Map<?, ?> commandMap = fallback ? entry.getValue().getMailcapFallbackList(mimeType) : entry.getValue()
                    .getMailcapList(mimeType);

            if (commandMap == null) {
                continue;
            }

            for (Object verbObject : commandMap.keySet()) {
                String verb = (String) verbObject;

                if (!commandsHaveVerb(accumulator, verb)) {
                    List<?> commands = (List<?>) commandMap.get(verb);
                    String className = (String) commands.get(0);
                    accumulator.add(new CommandInfo(verb, className));
                }
            }
        }
    }

    @Override
    public CommandInfo[] getAllCommands(String mimeType) {
        List<CommandInfo> commands = new ArrayList<CommandInfo>();
        if (mimeType != null) {
            mimeType = mimeType.toLowerCase(Locale.ENGLISH);
        }

        synchronized (sync) {
            getAllCommands(mimeType, commands, false);
            getAllCommands(mimeType, commands, true);
        }

        return commands.toArray(new CommandInfo[commands.size()]);
    }

    private void getAllCommands(String mimeType, List<CommandInfo> accumulator, boolean fallback) {
        for (Map.Entry<Bundle, MailcapFile> entry : db.entrySet()) {
            Map<?, ?> commandMap = fallback ? entry.getValue().getMailcapFallbackList(mimeType) : 
                entry.getValue() .getMailcapList(mimeType);

            if (commandMap == null) {
                continue;
            }

            for (Object verbAsObject : commandMap.keySet()) {
                String verb = (String) verbAsObject;

                List<?> commands = (List<?>) commandMap.get(verb);
            
                for (Object command : commands) {
                    accumulator.add(new CommandInfo(verb, (String) command));
                }

            }
        }
    }

    @Override
    public CommandInfo getCommand(String mimeType, String cmdName) {
        if (mimeType != null) {
            mimeType = mimeType.toLowerCase(Locale.ENGLISH);
        }

        CommandInfo command = null;

        synchronized (sync) {
            command = getCommand(mimeType, cmdName, false);
            if (command != null) {
                return command;
            }

            command = getCommand(mimeType, cmdName, true);
        }

        return command;
    }
    
    private CommandInfo getCommand(String mimeType, String commandName, boolean fallback) {
        
        for (Map.Entry<Bundle, MailcapFile> entry : db.entrySet()) {
            Map<?, ?> commandMap = fallback ? entry.getValue().getMailcapFallbackList(mimeType)
                    : entry.getValue().getMailcapList(mimeType);
            if (commandMap != null) {
                List<?> commands = (List<?>) commandMap.get(commandName);
                if (commands == null) {
                    continue;
                }

                String cmdClassName = (String) commands.get(0);

                if (cmdClassName != null) {
                    return new CommandInfo(commandName, cmdClassName);
                }
            }
        }
        
        return null;
    }

    @Override
    public DataContentHandler createDataContentHandler(String mimeType) {
        if (mimeType != null) {
            mimeType = mimeType.toLowerCase(Locale.ENGLISH);
        }

        synchronized (sync) {
            DataContentHandler dch = findDataContentHandler(mimeType, false);

            if (dch != null) {
                return dch;
            }

            return findDataContentHandler(mimeType, true);
        }
    }

    private DataContentHandler findDataContentHandler(String mimeType, boolean fallback) {

        for (Map.Entry<Bundle, MailcapFile> entry : db.entrySet()) {
            Map<?, ?> commandMap = fallback ? entry.getValue().getMailcapFallbackList(mimeType) : entry.getValue()
                    .getMailcapList(mimeType);
            if (commandMap != null) {
                List<?> v = (List<?>) commandMap.get("content-handler");
                if (v == null) {
                    continue;
                }

                String name = (String) v.get(0);
                DataContentHandler dch = getDataContentHandler(name, entry.getKey());
                if (dch != null) {
                    return dch;
                }
            }
        }

        return null;
    }

    public String[] getMimeTypes() {
        List<String> mimeTypesList = new ArrayList<String>();

        synchronized (sync) {
            for (Map.Entry<Bundle, MailcapFile> entry : db.entrySet()) {
                String[] mimeTypes = entry.getValue().getMimeTypes();
                for (String mimeType : mimeTypes) {
                    if (!mimeTypesList.contains(mimeType)) {
                        mimeTypesList.add(mimeType);
                    }
                }
            }
        }

        return mimeTypesList.toArray(new String[mimeTypesList.size()]);
    }

    private DataContentHandler getDataContentHandler(String name, Bundle bundle) {
        try {
            return (DataContentHandler) bundle.loadClass(name).newInstance();
        } catch (InstantiationException e) {
            log.warn("Unable to instantiate " + DataContentHandler.class.getSimpleName() + " class ' " + name
                    + " ' from bundle " + bundle, e);
        } catch (IllegalAccessException e) {
            log.warn("Unable to instantiate " + DataContentHandler.class.getSimpleName() + " class ' " + name
                    + " ' from bundle " + bundle, e);
        } catch (ClassNotFoundException e) {
            log.warn("Unable to instantiate " + DataContentHandler.class.getSimpleName() + " class ' " + name
                    + " ' from bundle " + bundle, e);
        }

        return null;
    }

    private boolean commandsHaveVerb(List<CommandInfo> commands, String verb) {

        for (CommandInfo commandInfo : commands) {
            if (commandInfo.getCommandName().equals(verb)) {
                return true;
            }
        }

        return false;
    }
}
