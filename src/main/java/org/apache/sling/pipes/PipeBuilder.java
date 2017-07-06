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

import org.apache.sling.api.resource.PersistenceException;

import java.util.Set;

/**
 * Builder and Runner of a pipe, based on a fluent API, for script and java usage.
 */
public interface PipeBuilder {
    /**
     * attach a new pipe to the current context
     * @param type resource type (should be registered by the plumber)
     * @return updated instance of PipeBuilder
     */
    PipeBuilder pipe(String type);

    /**
     * attach a move pipe to the current context
     * @param expr target of the resource to move
     * @return updated instance of PipeBuilder
     */
    PipeBuilder mv(String expr);

    /**
     * attach a write pipe to the current context
     * @param conf configuration parameters
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder write(Object... conf) throws IllegalAccessException;

    /**
     * attach a filter pipe to the current context
     * @param conf configuration parameters
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder filter(Object... conf) throws IllegalAccessException;

    /**
     * attach an authorizable pipe to the current context
     * @param conf configuration key value pairs for authorizable (see pipe's doc)
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder auth(Object... conf) throws IllegalAccessException;

    /**
     * attach a xpath pipe to the current context
     * @param expr xpath expression
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder xpath(String expr) throws IllegalAccessException;

    /**
     * attach a sling query pipe to the current context
     * @param expr sling query expression
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder $(String expr) throws IllegalAccessException;

    /**
     * attach a rm pipe to the current context
     * @return updated instance of PipeBuilder
     */
    PipeBuilder rm();

    /**
     * attach a json pipe to the current context
     * @param expr json expr or URL
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder json(String expr) throws IllegalAccessException;

    /**
     * Attach a path pipe to the current context
     * @param expr path to create
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder mkdir(String expr) throws IllegalAccessException;

    /**
     * attach a base pipe to the current context
     * @param path pipe path
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder echo(String path) throws IllegalAccessException;

    /**
     * attach a parent pipe to the current context
     * @return updated instance of PipeBuilder
     */
    PipeBuilder parent();

    /**
     * parameterized current pipe in the context
     * @param param key (property name) of the property
     * @param value value of te property
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder with(String param, String value) throws IllegalAccessException;

    /**
     * set an expr configuration to the current pipe in the context
     * @param value expression value
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder expr(String value) throws IllegalAccessException;

    /**
     * sets a pipe name, important in case you want to reuse it in another expression
     * @param name to overwrite default binding name (otherwise it will be "one", "two", ...)
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder name(String name) throws IllegalAccessException;

    /**
     * set a path configuration to the current pipe in the context
     * @param value path value
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder path(String value) throws IllegalAccessException;

    /**
     * Building up a set of configurations for the current pipe
     * @param properties configuration key value pairs (must be an even number of arguments)
     * @return updated instance of PipeBuilder
     * @throws IllegalAccessException in case it's called in a bad time
     */
    PipeBuilder conf(Object... properties) throws IllegalAccessException;

    /**
     * builds a configured pipe
     * @return Created (not executed) Pipe instance
     * @throws PersistenceException error occuring when saving the pipe configuration
     */
    Pipe build() throws PersistenceException;

    /**
     * builds and run configured pipe
     * @return set of resource path, output of the pipe execution
     * @throws Exception exceptions thrown by the build or the pipe execution itself
     */
    Set<String> run() throws Exception;
}
