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
 * The <code>RewriterTransformerFactory</code> is an optional component
 * which can be used to enhance the rewriting pipeline.
 * All available rewriting transformers with a service ranking below
 * zero are chained before the default link rewriter. All available
 * transformers with a service ranking higher or equal to zero are
 * chained after the default link rewriter. Therefore the property
 * "service.ranking" should be used for the factory.
 *
 * The factories itself are not chained but the resulting transformers
 * are. On each pipeline call new instances are created.
 */
public interface RewriterTransformerFactory {

    /**
     * Create a new transformer for the pipeline.
     * @return A new transformer.
     */
    Transformer createTransformer();
}
