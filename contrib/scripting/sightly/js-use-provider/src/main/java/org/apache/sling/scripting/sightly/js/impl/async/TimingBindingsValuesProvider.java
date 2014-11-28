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
package org.apache.sling.scripting.sightly.js.impl.async;

import javax.script.Bindings;

import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.sightly.js.impl.Variables;

/**
 * Value provider for timing functions
 */
public final class TimingBindingsValuesProvider implements BindingsValuesProvider {

    public static final TimingBindingsValuesProvider INSTANCE = new TimingBindingsValuesProvider();

    private TimingBindingsValuesProvider() {
    }

    @Override
    public void addBindings(Bindings bindings) {
        bindings.put(Variables.SET_TIMEOUT, TimingFunction.INSTANCE);
        bindings.put(Variables.SET_IMMEDIATE, TimingFunction.INSTANCE);
    }
}
