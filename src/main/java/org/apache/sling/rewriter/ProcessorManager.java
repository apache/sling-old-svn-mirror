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

import java.util.List;

/**
 * This service manages the processor configurations.
 */
public interface ProcessorManager {

    /**
     * Return the list of currently available processor configurations.
     * @return The list of processor configurations in the order to check.
     */
    List<ProcessorConfiguration> getProcessorConfigurations();

    /**
     * Return a pipeline for a pipeline configuration.
     * @throws org.apache.sling.api.SlingException If an error occurs during setup
     */
    Processor getProcessor(ProcessorConfiguration configuration,
                           ProcessingContext       context);
}
