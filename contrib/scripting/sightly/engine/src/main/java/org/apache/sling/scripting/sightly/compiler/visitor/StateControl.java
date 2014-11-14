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

package org.apache.sling.scripting.sightly.compiler.visitor;

import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;
import org.apache.sling.scripting.sightly.compiler.api.ris.CommandVisitor;

/**
 * Interface to control stateful visitors
 */
public interface StateControl {

    /**
     * Push a new visitor as the current state
     * @param visitor - the new active visitor
     */
    void push(CommandVisitor visitor);

    /**
     * Pop the current visitor and set the previous visitor active
     * @return - the previously active visitor
     */
    CommandVisitor pop();

    /**
     * Replace the current active visitor with another
     * @param visitor the new active visitor
     * @return the previously active visitor
     */
    CommandVisitor replace(CommandVisitor visitor);

}
