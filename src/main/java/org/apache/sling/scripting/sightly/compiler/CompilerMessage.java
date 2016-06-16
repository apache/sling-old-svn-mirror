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

import aQute.bnd.annotation.ProviderType;

/**
 * This class describes the messages (warnings or errors) the {@link SightlyCompiler} will produce when compiling a script.
 */
@ProviderType
public interface CompilerMessage {

    /**
     * Returns the script name associated with this message.
     *
     * @return the script name associated with this message
     */
    String getScriptName();

    /**
     * Returns the compiler's message.
     *
     * @return the compiler's message
     */
    String getMessage();

    /**
     * Returns the line number of the script text that generated this message.
     *
     * @return the line number of the script text that generated this message
     */
    int getLine();

    /**
     * Returns the column number of the script text that generated this message.
     *
     * @return the column number of the script text that generated this message
     */
    int getColumn();
}
