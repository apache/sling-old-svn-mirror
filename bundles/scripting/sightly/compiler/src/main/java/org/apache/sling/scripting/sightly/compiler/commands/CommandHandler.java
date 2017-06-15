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
package org.apache.sling.scripting.sightly.compiler.commands;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@code CommandHandler} is the mechanism through which a {@link CommandStream} can be processed synchronously, as the stream is
 * written.
 */
@ProviderType
public interface CommandHandler {

    /**
     * Allows this handler to process the {@link Command} that was just written into the stream to which this handler was attached.
     *
     * @param command the received command
     */
    void onEmit(Command command);

    /**
     * Allows this handler to process error states.
     *
     * @param errorMessage the error's message
     */
    void onError(String errorMessage);

    /**
     * This method is called when the stream has been closed. The contract is that after this call, no other commands or errors will be
     * emitted.
     */
    void onDone();

}
