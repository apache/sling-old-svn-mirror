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
package org.apache.sling.scripting.sightly.impl.compiler.util.stream;

import org.apache.sling.scripting.sightly.impl.compiler.ris.Command;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandHandler;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandStream;

/**
 * A stream that can be written into
 */
public class PushStream implements CommandStream {

    private BroadcastHandler handler = new BroadcastHandler();
    private boolean closed;

    /**
     * Add a command handler
     * @param handler - a command handler
     */
    @Override
    public void addHandler(CommandHandler handler) {
        this.handler.addHandler(handler);
    }

    /**
     * Emit the specified command
     * @param command - the emitted command
     * @throws UnsupportedOperationException - if the stream is closed
     */
    public void emit(Command command) {
        if (closed) {
            throw new UnsupportedOperationException("Stream is closed");
        }
        this.handler.onEmit(command);
    }

    /**
     * Signal an error
     * @param message - the error message
     * @throws UnsupportedOperationException - if the stream is already closed
     */
    public void signalError(String message) {
        if (closed) {
            throw new UnsupportedOperationException("Stream is closed");
        }
        closed = true;
        this.handler.onError(message);
    }

    /**
     * Close the stream
     * @throws UnsupportedOperationException - if the stream is already closed
     */
    public void signalDone() {
        if (closed) {
            throw new UnsupportedOperationException("Stream is already closed");
        }
        closed = true;
        this.handler.onDone();
    }
}
