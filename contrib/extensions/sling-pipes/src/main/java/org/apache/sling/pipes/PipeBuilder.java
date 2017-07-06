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
 * Builder & Runner of a pipe, based on a fluent API, for script & java usage.
 */
public interface PipeBuilder {
    /**
     * attach a new pipe to the current context
     * @param type
     * @return
     */
    PipeBuilder pipe(String type);

    /**
     * attach a move pipe to the current context
     * @param expr
     * @return
     */
    PipeBuilder mv(String expr);

    /**
     * attach a write pipe to the current context
     * @param conf configuration parameters
     * @return
     */
    PipeBuilder write(Object... conf) throws IllegalAccessException;

    /**
     * attach a filter pipe to the current context
     * @param conf configuration parameters
     * @return
     */
    PipeBuilder filter(Object... conf) throws IllegalAccessException;

    /**
     * attach an authorizable pipe to the current context
     * @param conf
     * @return
     */
    PipeBuilder auth(Object... conf) throws IllegalAccessException;

    /**
     * attach a xpath pipe to the current context
     * @param expr xpath expression
     * @return
     */
    PipeBuilder xpath(String expr) throws IllegalAccessException;

    /**
     * attach a sling query pipe to the current context
     * @param expr sling query expression
     * @return
     */
    PipeBuilder $(String expr) throws IllegalAccessException;

    /**
     * attach a rm pipe to the current context
     * @return
     */
    PipeBuilder rm();

    /**
     * attach a json pipe to the current context
     * @param expr json expr or URL
     * @return
     */
    PipeBuilder json(String expr) throws IllegalAccessException;

    /**
     * Attach a path pipe to the current context
     * @param expr
     * @return
     */
    PipeBuilder mkdir(String expr) throws IllegalAccessException;

    /**
     * attach a base pipe to the current context
     * @param path pipe path
     * @return
     */
    PipeBuilder echo(String path) throws IllegalAccessException;

    /**
     * attach a new pipe to the current context
     * @return
     */
    PipeBuilder parent();

    /**
     * parameterized current pipe in the context
     * @param param
     * @param value
     * @return
     */
    PipeBuilder with(String param, String value) throws IllegalAccessException;

    /**
     * add an expr configuration to the current pipe in the context
     * @param value
     * @return
     */
    PipeBuilder expr(String value) throws IllegalAccessException;

    /**
     * sets a pipe name, important in case you want to reuse it in another expression
     * @param name
     * @return
     * @throws IllegalAccessException
     */
    PipeBuilder name(String name) throws IllegalAccessException;

    /**
     * add a path configuration to the current pipe in the context
     * @param value
     * @return
     */
    PipeBuilder path(String value) throws IllegalAccessException;

    /**
     * Building up a set of configurations for the current pipe
     * @param properties
     * @return
     */
    PipeBuilder conf(Object... properties) throws IllegalAccessException;

    /**
     * builds a configured pipe
     * @return
     */
    Pipe build() throws PersistenceException;

    /**
     * builds & run configured pipe
     * @return
     * @throws Exception
     */
    Set<String> run() throws Exception;
}
