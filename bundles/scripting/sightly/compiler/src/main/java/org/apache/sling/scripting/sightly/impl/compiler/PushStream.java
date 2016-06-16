/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler;

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.commands.Command;
import org.apache.sling.scripting.sightly.compiler.commands.CommandHandler;
import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.BroadcastHandler;

/**
 * A stream that can be written into.
 */
public final class PushStream implements CommandStream {

    private BroadcastHandler handler = new BroadcastHandler();
    private boolean closed;
    private List<Command> commands = new LinkedList<>();
    private List<Warning> warnings = new LinkedList<>();

    @Override
    public void addHandler(CommandHandler handler) {
        this.handler.addHandler(handler);
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    public List<Warning> getWarnings() {
        return warnings;
    }

    public void write(Command command) {
        if (closed) {
            throw new UnsupportedOperationException("Stream is closed");
        }
        commands.add(command);
        this.handler.onEmit(command);
    }

    /**
     * Signal an error to the attached {@link CommandHandler}.
     *
     * @param message the error message
     * @throws UnsupportedOperationException if the stream has been closed
     */
    public void signalError(String message) {
        if (closed) {
            throw new UnsupportedOperationException("Stream has already been closed.");
        }
        closed = true;
        this.handler.onError(message);
    }

    /**
     * Closes this stream. Once the stream has been closed no other methods can be called on the stream any more.
     *
     * @throws UnsupportedOperationException if the stream has already been closed
     */
    public void close() {
        if (closed) {
            throw new UnsupportedOperationException("Stream has already been closed.");
        }
        closed = true;
        this.handler.onDone();
    }

    public void write(Warning warning) {
        warnings.add(warning);
    }

    public static class Warning {
        private String message;
        private String code;

        public Warning(String message, String code) {
            this.message = message;
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }
    }
}
