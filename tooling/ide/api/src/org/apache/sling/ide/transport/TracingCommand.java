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
package org.apache.sling.ide.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.ide.transport.Repository.CommandExecutionFlag;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TracingCommand<T> implements Command<T> {

    public static <T> TracingCommand<T> wrap(Command<T> command, EventAdmin eventAdmin) {
        return new TracingCommand<>(command, eventAdmin);
    }

    private final Command<T> command;
    private final EventAdmin eventAdmin;

    public TracingCommand(Command<T> command, EventAdmin eventAdmin) {
        this.command = command;
        this.eventAdmin = eventAdmin;
    }

    @Override
    public Result<T> execute() {

        long start = System.currentTimeMillis();
        Result<T> result = command.execute();
        long end = System.currentTimeMillis();

        if (eventAdmin != null) {
            Map<String, Object> props = new HashMap<>();
            props.put(CommandExecutionProperties.RESULT_TEXT, result.toString());
            props.put(CommandExecutionProperties.RESULT_STATUS, result.isSuccess());
            try {
                result.get();
            } catch (RepositoryException e) {
                props.put(CommandExecutionProperties.RESULT_THROWABLE, e);
            }
            props.put(CommandExecutionProperties.ACTION_TYPE, command.getClass().getSimpleName());
            Set<CommandExecutionFlag> flags = command.getFlags();
            if (!flags.isEmpty()) {
                StringBuilder flagsString = new StringBuilder();
                for (CommandExecutionFlag flag : flags) {
                    flagsString.append(flag).append(',');
                }
                flagsString.deleteCharAt(flagsString.length() - 1);
                props.put(CommandExecutionProperties.ACTION_FLAGS, flagsString.toString());
            }

            props.put(CommandExecutionProperties.ACTION_TARGET, command.getPath());
            props.put(CommandExecutionProperties.TIMESTAMP_START, start);
            props.put(CommandExecutionProperties.TIMESTAMP_END, end);
            Event event = new Event(CommandExecutionProperties.REPOSITORY_TOPIC, props);
            eventAdmin.postEvent(event);
        }

        return result;
    }

    public String getPath() {
        return command.getPath();
    }

    @Override
    public Set<CommandExecutionFlag> getFlags() {
        return command.getFlags();
    }
    
    @Override
    public Kind getKind() {
        return command.getKind();
    }

}