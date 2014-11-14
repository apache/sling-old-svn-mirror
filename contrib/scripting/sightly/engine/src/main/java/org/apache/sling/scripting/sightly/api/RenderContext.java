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

package org.apache.sling.scripting.sightly.api;

import java.util.Map;

import javax.script.Bindings;
import javax.script.SimpleBindings;

/**
 * Rendering context for Sightly rendering units.
 * @see RenderUnit
 */
public class RenderContext {

    private final StackedWriter writer;
    private final Bindings bindings;
    private final ObjectModel objectModel;
    private final SightlyRuntime runtime;
    private final UnitLocator unitLocator;

    public RenderContext(StackedWriter writer, Bindings bindings, ObjectModel objectModel, SightlyRuntime runtime, UnitLocator unitLocator) {
        this.writer = writer;
        this.bindings = bindings;
        this.objectModel = objectModel;
        this.runtime = runtime;
        this.unitLocator = unitLocator;
    }

    /**
     * Get the writer where the content should be written
     * @return - a stacked writer
     */
    public StackedWriter getWriter() {
        return writer;
    }

    /**
     * Provide the bindings for this script
     * @return - the list of global bindings available to the script
     */
    public Bindings getBindings() {
        return bindings;
    }

    /**
     * Get an instance of the objectModel object model
     * @return - a objectModel object model instance
     */
    public ObjectModel getObjectModel() {
        return objectModel;
    }

    /**
     * Get the available Sightly runtime
     * @return - an instance of the Sightly runtime
     */
    public SightlyRuntime getRuntime() {
        return runtime;
    }

    /**
     * Get the unit locator
     * @return - a unit locator
     */
    public UnitLocator getUnitLocator() {
        return unitLocator;
    }

    /**
     * Create a new render context with different bindings
     * @param newBindings - the new set of bindings
     * @return - the new render context
     */
    public RenderContext withBindings(Bindings newBindings) {
        return new RenderContext(this.writer, newBindings, this.objectModel, this.runtime, this.unitLocator);
    }

    /**
     * Create a new render context with additional bindings
     * @param additionalBindings - a dictionary of variables
     * @return - the new render context
     */
    public RenderContext withAddedBindings(Map additionalBindings) {
        Bindings newBindings = new SimpleBindings();
        newBindings.putAll(this.bindings);
        //noinspection unchecked
        newBindings.putAll(additionalBindings);
        return withBindings(newBindings);
    }
}
