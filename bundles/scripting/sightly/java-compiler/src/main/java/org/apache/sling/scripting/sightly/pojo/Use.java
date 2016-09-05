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
package org.apache.sling.scripting.sightly.pojo;

import javax.script.Bindings;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>Use</code> interface can be implemented by Java objects which are instantiated as part of processing {@code data-sly-use}
 * attributes.
 *
 * @see <a href="https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/master/SPECIFICATION.md#221-use">HTL Block Statements - Use</a>
 */
@ConsumerType
public interface Use {

    /**
     * <p>
     *     Called to initialize the Java object with the current Java Scripting API bindings.
     * </p>
     * <p>
     *     This method is called only if the object has been instantiated by HTL as part of processing the {@code data-sly-use}
     *     attribute. The Java Scripting API bindings provide all the global variables known to a script being executed.
     * </p>
     *
     * @param bindings The Java Scripting API bindings.
     */
    void init(Bindings bindings);

}
