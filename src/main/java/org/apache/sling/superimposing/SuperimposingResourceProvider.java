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
package org.apache.sling.superimposing;

import org.apache.sling.api.resource.ResourceProvider;

/**
 * Superimposing resource provider.
 * Maps a single source path to the target root path, with or without overlay depending on configuration.
 */
public interface SuperimposingResourceProvider extends ResourceProvider {

    /**
     * Mixin for superimposing.
     */
    String MIXIN_SUPERIMPOSE = "sling:Superimpose";

    /**
     * Property pointing to an absolute or relative repository path, which this superimpose definition points to.
     */
    String PROP_SUPERIMPOSE_SOURCE_PATH = "sling:superimposeSourcePath";

    /**
     * Property indicating if the node itself is used as root for the superimpose definition (default),
     * of it it's parent should be used. The latter is useful in a Page/PageContent scenario
     * where the mixin cannot be added on the parent node itself.
     */
    String PROP_SUPERIMPOSE_REGISTER_PARENT = "sling:superimposeRegisterParent";

    /**
     * Property indicating whether this superimposing definition allows the superimposed content
     * to be overlayed by real nodes created below the superimposing root node.
     * Default value is false.
     */
    String PROP_SUPERIMPOSE_OVERLAYABLE = "sling:superimposeOverlayable";

    /**
     * @return Root path (source path)
     */
    String getRootPath();

    /**
     * @return Target path (destination path)
     */
    String getSourcePath();

    /**
     * @return Overlayable yes/no
     */
    boolean isOverlayable();

}
