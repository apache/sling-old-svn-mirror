/*
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
 */
package org.apache.sling.installer.api.tasks;

/**
 * A resource transformer transform a registered resource
 * before it can be installed.
 * Based on the transformation result, the installer creates
 * one or more {@link TaskResource}s from the registered
 * resources and passes them on to the {@link InstallTaskFactory}.
 */
public interface ResourceTransformer {

    /**
     * Try to transform the registered resource.
     * If the transformer is not responsible for transforming the
     * resource, it should return <code>null</code>
     *
     * @param resource The resource
     * @return An array of transformation results or <code>null</code>
     */
    TransformationResult[] transform(RegisteredResource resource);
}
