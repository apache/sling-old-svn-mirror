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
package org.apache.sling.ide.impl.resource.transport;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.impl.resource.util.Tracer;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandExecutionProperties;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.Result;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

class TracingCommand<T> implements Command<T> {

    private final AbstractCommand<T> command;
    private final Tracer tracer;
    private final EventAdmin eventAdmin;

    public TracingCommand(AbstractCommand<T> command, Tracer tracer, EventAdmin eventAdmin) {
        this.command = command;
        this.tracer = tracer;
        this.eventAdmin = eventAdmin;
    }

    @Override
    public Result<T> execute() {

        long start = System.currentTimeMillis();
        Result<T> result = command.execute();
        long end = System.currentTimeMillis();

        if (tracer != null)
            tracer.trace("{0} -> {1}", command, result.toString());

        if (eventAdmin != null) {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(CommandExecutionProperties.RESULT_TEXT, result.toString());
            try {
                result.get();
            } catch (RepositoryException e) {
                props.put(CommandExecutionProperties.RESULT_THROWABLE, e);
            }
            props.put(CommandExecutionProperties.ACTION_TYPE, command.getClass().getSimpleName());
            props.put(CommandExecutionProperties.ACTION_TARGET, command.getPath());
            props.put(CommandExecutionProperties.TIMESTAMP_START, start);
            props.put(CommandExecutionProperties.TIMESTAMP_END, end);
            Event event = new Event("org/apache/sling/ide/transport", props);
            eventAdmin.postEvent(event);
        }

        return result;
    }

}