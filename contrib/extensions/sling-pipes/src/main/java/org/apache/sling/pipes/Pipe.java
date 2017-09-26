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

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Pipe interface
 */
@ConsumerType
public interface Pipe {
    /**
     * Name of the pipe
     */
    String PN_NAME = "name";

    /**
     * expression of the pipe, usage depends on the pipe implementation
     */
    String PN_EXPR = "expr";

    /**
     * resource's path associated to the path, usage depends on the pipe implementation
     */
    String PN_PATH = "path";

    /**
     * Node name for the pipe's configuration
     */
    String NN_CONF = "conf";

    /**
     * Distribution agent (for distributing output resources)
     */
    String PN_DISTRIBUTION_AGENT = "distribution.agent";

    /**
     * returns true if that pipe modifies content during its execution
     * @return true for write / false for read
     */
    boolean modifiesContent();

    /**
     * returns true if that pipe is set not to write content
     * @return true if dry run, false otherwise
     */
    boolean isDryRun();

    /**
     * Return the name of that pipe
     * @return name of the pipe
     */
    String getName();

    /**
     * set the pipe parent
     * @param parent container pipe
     */
    void setParent(ContainerPipe parent);

    /**
     * Return parent's pipe (can be null)
     * @return pipe's container parent
     */
    ContainerPipe getParent();

    /**
     * Get the pipe's optional configured resource or null
     * @return input if configured
     */
    Resource getConfiguredInput();

    /**
     * Get pipe current's resource *before* next execution, meaning either the
     * configured resource, either previous' pipe output resource
     * @return input, configured or previous pipe
     */
    Resource getInput();


    /**
     * get the pipe configuration resource
     * @return Pipe configruation root resource
     */
    Resource getResource();

    /**
     * returns the binding output used in container pipe's expression
     * @return object, either value map or something else, that will be used in nashorn for computing expressions
     */
    Object getOutputBinding();

    /**
     * returns the pipe's bindings
     * @return PipeBindings instance containing all bindings of that pipe
     */
    PipeBindings getBindings();

    /**
     * set the pipe's bindings
     * @param bindings bindings to set
     */
    void setBindings(PipeBindings bindings);

    /**
     * Executes the pipe, can be contained in a parent or not
     * @return iterator of resource resulting from execution of this pipe
     */
    Iterator<Resource> getOutput();

    /**
     * Get Distribution agent
     * @return configured distribution agent
     */
    String getDistributionAgent();

    /**
     * sets the reference pipe this pipe is referred by
     * @param pipe referrer that refers to this instance
     */
    void setReferrer(ReferencePipe pipe);
}
