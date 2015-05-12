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

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>Use</code> interface can be implemented by Java objects
 * which are instantiated as part of processing {@code data-sly-use}
 * attributes.
 *
 * @see <a href="http://docs.adobe.com/docs/en/aem/6-0/develop/sightly.html#use">Sightly Block Statements - Use</a>
 */
@ConsumerType
public interface Use {

    /**
     * Called to initialize the Java object with the current Java Scripting
     * API bindings.
     * <p>
     * This method is called only if the object has been instantiated by
     * Sightly as part of processing the {@code data-sly-use} attribute.
     * <p>
     * The Java Scripting API bindings provide all the global variables
     * known to a script being executed. Consider these bindings of a map
     * from variable name to the variable's value.
     *
     * @param bindings The Java Scripting API bindings.
     */
    public void init(Bindings bindings);

}
