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
package org.apache.sling.scripting.sightly.compiler;

import org.apache.sling.api.resource.Resource;

/**
 * The {@code SightlyCompileService} allows for simple instantiation of arbitrary classes that are either stored in the repository
 * or in regular OSGi bundles. It also compiles Java sources on-the-fly and can discover class' source files based on
 * Resources (typically Sling components). It supports Sling Resource type inheritance.
 *
 * The Java compiler only recompiles a class in case the source's timestamp is older than the compiled class if already
 * existing. Otherwise it will load the class directly through the Sling ClassLoaderWriter service.
 */
public interface SightlyCompileService {

    /**
     * This method returns an Object instance based on a class that is either found through regular classloading mechanisms or on-the-fly
     * compilation. In case the requested class does not denote a fully qualified classname, this service will try to find the class through
     * Sling's servlet resolution mechanism and compile the class on-the-fly if required.
     *
     * @param resource    the lookup will be performed based on this resource
     * @param className   name of class to use for object instantiation
     * @param attemptLoad attempt to load the object from the class loader's cache
     * @return object instance of the requested class
     * @throws CompilerException in case of any runtime exception
     */
    public Object getInstance(Resource resource, String className, boolean attemptLoad);

    /**
     * Compiles a class using the passed fully qualified classname and based on the resource that represents the class' source.
     *
     * @param scriptResource resource that constitutes the class' source
     * @param className      Fully qualified name of the class to compile
     * @return object instance of the class to compile
     * @throws CompilerException in case of any runtime exception
     */
    public Object compileSource(Resource scriptResource, String className);
}
