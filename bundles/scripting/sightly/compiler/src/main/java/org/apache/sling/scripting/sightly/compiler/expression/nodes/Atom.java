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
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An {@code Atom} defines an {@link ExpressionNode} that can be translated to simple text, like identifiers or primitive constants
 * (strings, booleans and numbers).
 */
@ProviderType
public interface Atom extends ExpressionNode {

    /**
     * Get the text content for this nodes
     *
     * @return the text content
     */
    String getText();

}

