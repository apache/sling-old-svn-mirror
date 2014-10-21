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
package org.apache.sling.models.impl.injectors;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * OSGi service interface for getting a sling request for the current thread.
 * 
 * TODO: it would be nice to move this to some central part e.g. in the Sling API.
 */
public interface SlingObjectInjectorRequestContext {

    /**
     * Returns sling request associated with the current thread.
     * The request is stored in a threadlocal set by a servlet filter.
     * @return Sling request or null if none is associated.
     */
    SlingHttpServletRequest getThreadRequest();

}
