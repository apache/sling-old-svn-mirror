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

import org.apache.sling.scripting.sightly.compiler.commands.CommandStream;
import org.apache.sling.scripting.sightly.compiler.commands.CommandVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.PushStream;

/**
 * Utility functions for streams.
 */
public class Streams {

    /**
     * Attach the visitor as a handle to the inStream and propagate
     * the done signal from the inStream to the outStream
     *
     * @param inStream  - the input stream
     * @param outStream - the output stream
     * @param visitor   - a command visitor
     */
    public static void connect(CommandStream inStream, final PushStream outStream, CommandVisitor visitor) {
        inStream.addHandler(new VisitorHandler(visitor) {
            @Override
            public void onDone() {
                outStream.close();
            }
        });
    }

    /**
     * Attach the emitting visitor to the inStream.
     *
     * @param inStream       - the input stream
     * @param emitterVisitor - the emitter visitor
     * @return - the output stream of the emitter
     */
    public static CommandStream map(CommandStream inStream, EmitterVisitor emitterVisitor) {
        PushStream outStream = emitterVisitor.getOutputStream();
        connect(inStream, outStream, emitterVisitor);
        return outStream;
    }

}
