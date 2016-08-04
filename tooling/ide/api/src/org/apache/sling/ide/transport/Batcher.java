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

import java.util.List;

/**
 * The <tt>Batcher</tt> potentially optimises the way multiple commands are executed
 *
 * <p>Some potential optimisations:
 * 
 * <ol>
 *   <li>Filter out duplicate commands</li>
 *   <li>Compact multiple delete commands into a large one with the same semantics</li>
 * </ol>
 * 
 * </p>
 */
public interface Batcher {
    
    /**
     * Adds a command to the current processing session
     * 
     * @param command the command, must not be <code>null</code>
     */
    void add(Command<?> command);
    
    /**
     * Returns a list of optimised commands, based on the commands added so far
     * 
     * <p>Once the list is returned, the added commands are removed and will not be
     * returned by subsequent invocations of this method.</p>
     * 
     * @return the list of potentially optimised commands
     */
    List<Command<?>> get();
}
