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
package org.apache.sling.rewriter;


/**
 * The <code>SerializerFactory</code> is a service which creates
 * {@link Serializer}s on demand. The created serializers are the
 * end point for the rewriter pipeline.
 *
 * The factories itself are not chained but the resulting serializers
 * are. On each pipeline call new instances are created.
 *
 * The factory is referenced using a service property named
 * 'pipeline.type'. Each factory should have a unique value
 * for this property.
 */
public interface SerializerFactory {

    /**
     * Create a new serializer for the pipeline.
     * @return A new serializer.
     */
    Serializer createSerializer();
}
