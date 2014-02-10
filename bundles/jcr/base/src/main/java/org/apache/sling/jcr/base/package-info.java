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

/**
 * The {@code org.apache.sling.jcr.base} package provides basic support
 * to expose JCR repositories in Sling. The primary classes to implement are
 * {@code org.apache.sling.jcr.base.AbstractSlingRepositoryManager} to
 * manage the actual JCR repository instance and
 * {@link org.apache.sling.jcr.base.AbstractSlingRepository2} being the
 * basis for the repository service instance handed to using bundles.
 * <p>
 * The old {@link org.apache.sling.jcr.base.AbstractSlingRepository}
 * class is being deprecated in favor of the new method of providing access
 * to JCR repositories. Likewise the
 * {@link org.apache.sling.jcr.base.AbstractNamespaceMappingRepository} is
 * deprecated in favor of the new
 * {@link org.apache.sling.jcr.base.NamespaceMappingSupport} abstract class
 * and said repository manager.
 */
@Version("2.3")
package org.apache.sling.jcr.base;

import aQute.bnd.annotation.Version;

