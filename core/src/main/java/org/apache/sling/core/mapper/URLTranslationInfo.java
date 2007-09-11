/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.mapper;

import javax.jcr.Node;

/**
 * The URLTranslationInfo interface provides information of a translation from
 * a internal href to an external URL. This is usually returned by the
 * {@link URLMapper#externalizeHref} call.
 */
public interface URLTranslationInfo {

    /**
     * Returns the node that is referenced by the internal href or
     * <code>null</code> of the node could not be determined.
     */
    public Node getNode();

    /**
     * Returns the translated href.
     */
    public String getExternalHref();

    /**
     * Returns the original href.
     */
    public String getInternalHref();
}
