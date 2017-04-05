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
package org.apache.sling.api.adapter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>Adaptable</code> interface identifies objects which can be adapted
 * to other types or representations of the same object. For example a JCR Node
 * based {@link org.apache.sling.api.resource.Resource} can adapt to the
 * underlying JCR Node or a file based resource could adapt to the underlying
 * <code>java.io.File</code>.
 */
@ConsumerType
public interface Adaptable {

    /**
     * Adapts the adaptable to another type.
     * <p>
     * Please note that it is explicitly left as an implementation detail whether
     * each call to this method with the same <code>type</code> yields the same
     * object or a new object on each call.
     * <p>
     * Implementations of this method should document their adapted types as
     * well as their behaviour with respect to returning newly created or same
     * instances on each call.
     *
     * @param <AdapterType> The generic type to which this object is adapted
     *            to
     * @param type The Class object of the target type, such as
     *            <code>javax.jcr.Node.class</code> or
     *            <code>java.io.File.class</code>
     * @return The adapter target or <code>null</code> if the object cannot
     *         adapt to the requested type
     */
    @CheckForNull <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> type);

}
