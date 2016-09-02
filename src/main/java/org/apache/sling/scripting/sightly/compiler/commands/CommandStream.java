/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.compiler.commands;

import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 *     This interface defines a stream to which {@link Command}s are pushed during the compilation of a HTL script by the {@link
 *     org.apache.sling.scripting.sightly.compiler.SightlyCompiler}. Depending on how a consumer wants to use the stream there are several
 *     options:
 *      <ul>
 *          <li>if the stream needs to be consumed immediately then a {@link CommandHandler} can be attached to the stream; the stream,
 *          in turn, will notify the handler for every command that has been pushed;</li>
 *          <li>if the stream can be consumed after the actual compilation there's no need to attach a {@link CommandHandler}, as the
 *          {@link Command}s that were written to the stream can be replayed in the exact order in which they have been pushed.</li>
 *      </ul>
 * </p>
 */
@ProviderType
public interface CommandStream {

    /**
     * Registers a listening {@link CommandHandler} to the stream. The {@link CommandHandler} will be notified for every new {@link Command}
     * pushed to this stream.
     */
    void addHandler(CommandHandler handler);

    /**
     * Returns the {@link List} of commands that were written into this stream.
     *
     * @return the commands written into this stream
     */
    List<Command> getCommands();

}
