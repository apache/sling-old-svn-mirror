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
 * The <code>TransformerFactory</code> is a service which creates
 * {@link Transformer}s on demand. The created transformers form
 * the middle part of the rewriter pipeline.
 *
 * The factories itself are not chained but the resulting transformers
 * are. On each pipeline call new instances are created.
 *
 * The factory is referenced using a service property named
 * 'pipeline.type'. Each factory should have a unique value
 * for this property.
 *
 * With the optional property 'pipeline.mode' set to the value
 * 'global' the transformer is used for each and every pipeline regardless
 * of the actual configuration for this pipeline.
 * All available global transformers with a service ranking below
 * zero are chained right after the generator. All available global
 * transformers with a service ranking higher or equal to zero are
 * chained right before the serializer. Therefore the property
 * "service.ranking" should be used for the factory in combination
 * with 'pipeline.mode'.
 * To be compatible with possible future uses of the 'pipeline.mode'
 * property, it should only be used with the value 'global'.
 */
public interface TransformerFactory {

    /**
     * Create a new transformer for the pipeline.
     * @return A new transformer.
     */
    Transformer createTransformer();
}
