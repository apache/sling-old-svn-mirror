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
package org.apache.sling.content.jcr;

/**
 * The <code>SyntheticContent</code> class is a simple implementation of the
 * <code>Conteent</code> interface which may be used to provide a content object
 * which is not mapped from real repository content.
 * <p>
 * This class is not mapped by the JCR OCM mapper. Implementors wishing to
 * implement simple <code>Content</code> interfaces to be mapped should consider
 * extending the {@link BaseContent} or {@link SimpleContent} classes directly.
 */
public class SyntheticContent extends BaseContent {

    /**
     * Default constructor setting no fields. After creating the instance,
     * the path and component Id must be set by calling the respective setter
     * methods.
     */
    public SyntheticContent() {
    }

    /**
     * Creates a synthetic content with the given path and component Id.
     *
     * @param path The path of the synthetic content
     * @param componentId The ID of the component rendering the synthetic content
     */
    public SyntheticContent(String path, String componentId) {
        setPath(path);
        setComponentId(componentId);
    }
}
