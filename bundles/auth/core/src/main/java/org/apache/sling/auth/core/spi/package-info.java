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
 * Provides API to be implemented by provides of authentication
 * mechanisms. The most important interface (and service definition) is
 * the {@link org.apache.sling.auth.core.spi.AuthenticationHandler}
 * interface with the
 * {@link org.apache.sling.auth.core.spi.AbstractAuthenticationHandler}
 * being an abstract base implementation from which concrete
 * implementations may inherit.
 *
 * @version 1.2.0
 */
@Version("1.2.0")
package org.apache.sling.auth.core.spi;

import aQute.bnd.annotation.Version;

