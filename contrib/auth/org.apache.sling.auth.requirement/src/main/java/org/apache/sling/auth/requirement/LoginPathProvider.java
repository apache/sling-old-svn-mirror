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
package org.apache.sling.auth.requirement;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides a login path for a given {@link HttpServletRequest}.
 */
public interface LoginPathProvider {

    /**
     * Provides a login path for the given  {@link HttpServletRequest}
     * or {@code null} if no matching login path has been defined by this
     * module.
     *
     * The format of the login path is an implementation detail; it could be an
     * path pointing to any resource or to a node in the JCR repository,
     * an url linking to an external resource or any other suitable format.
     *
     * @param request The target request
     * @return A login path or {@code null}.
     */
    @CheckForNull
    String getLoginPath(@Nonnull HttpServletRequest request);
}