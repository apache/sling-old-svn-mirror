/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * <p>The {@code org.apache.sling.scripting.api.resolver} package provides a unified API for scripting bundles that need to perform script
 * resolution across the {@link org.apache.sling.api.resource.Resource} space.</p>
 *
 * <p>Some API methods might indicate that they are <i>request-bound</i>. In this case it should be noted that <i>usage outside of the
 * context of a Servlet API Request might lead to improper cleaning of objects whose life-cycle should not be longer than the request to
 * which they're bound to (for example per-thread objects)</i>.</p>
 *
 * <p>This package depends on the {@link org.apache.sling.api.resource} API, version &gt;= than 2.10.0.</p>
 */
@Version("1.0.0")
package org.apache.sling.scripting.api.resource;

import org.osgi.annotation.versioning.Version;
