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
package org.apache.sling.pipes;

import org.apache.sling.api.resource.Resource;

import java.util.Iterator;

/**
 * Pipe interface
 */
public interface Pipe {
    /**
     * Name of the pipe
     */
    public static final String PN_NAME = "name";

    /**
     * expression of the pipe, usage depends on the pipe implementation
     */
    public static final String PN_EXPR = "expr";

    /**
     * resource's path associated to the path, usage depends on the pipe implementation
     */
    public static final String PN_PATH = "path";

    /**
     * Node name for the pipe's configuration
     */
    public static final String NN_CONF = "conf";

    public static final String PN_DISTRIBUTION_AGENT = "distribution.agent";

    /**
     * returns true if that pipe will modify content during its execution
     * @return
     */
    boolean modifiesContent();

    /**
     * returns true if that pipe is set not to write content
     * @return
     */
    boolean isDryRun();

    /**
     * Return the name of that pipe
     * @return
     */
    String getName();

    /**
     * Set parent
     */
    void setParent(ContainerPipe parent);

    /**
     * Return parent's pipe (can be null)
     * @return
     */
    ContainerPipe getParent();

    /**
     * Get the pipe's optional configured resource or null
     * @return
     */
    Resource getConfiguredInput();

    /**
     * Get pipe current's resource *before* next execution, meaning either the
     * configured resource, either previous' pipe output resource
     * @return
     */
    Resource getInput();

    /**
     * returns the binding output used in container pipe's expression
     * @return
     */
    Object getOutputBinding();

    /**
     * returns the pipe's bindings
     * @return
     */
    PipeBindings getBindings();

    /**
     * set the pipe's bindings
     */
    void setBindings(PipeBindings bindings);

    /**
     * Executes the pipe, can be contained in a parent or not
     * @return
     */
    Iterator<Resource> getOutput();

    /**
     * Get Distribution agent
     */
    String getDistributionAgent();
}