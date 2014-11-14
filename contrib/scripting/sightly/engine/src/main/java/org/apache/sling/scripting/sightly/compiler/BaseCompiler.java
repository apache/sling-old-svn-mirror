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
package org.apache.sling.scripting.sightly.compiler;

import org.apache.sling.scripting.sightly.compiler.api.SightlyCompiler;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandStream;
import org.apache.sling.scripting.sightly.compiler.debug.SanityChecker;
import org.apache.sling.scripting.sightly.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.compiler.api.SightlyCompiler;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandStream;
import org.apache.sling.scripting.sightly.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.compiler.util.stream.PushStream;

/**
 * A base implementation of a compiler
 */
public abstract class BaseCompiler implements SightlyCompiler {

    @Override
    public void compile(String source, CompilerBackend backend) {
        PushStream stream = new PushStream();
        SanityChecker.attachChecker(stream);
        CommandStream optimizedStream = optimizedStream(stream);
        //optimizedStream.addHandler(LoggingHandler.INSTANCE);
        backend.handle(optimizedStream);
        getFrontend().compile(stream, source);
    }

    /**
     * Get the stream optimizer for this compiler
     * @return - a stream transformer that will optimize the command stream
     */
    protected abstract StreamTransformer getOptimizer();

    /**
     * Get the front-end for this compiler
     * @return a compiler front end
     */
    protected abstract CompilerFrontend getFrontend();

    private CommandStream optimizedStream(CommandStream stream) {
        return getOptimizer().transform(stream);
    }
}
