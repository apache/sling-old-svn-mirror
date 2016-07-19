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
package org.apache.sling.auth.requirement.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Internal interface used by the {@link RequirementObserver} to report content
 * changes that affect the authentication requirements.
 */
public interface RequirementHandler {

    /**
     * A new authentication requirement has been added at the given {@code path}.
     *
     * @param path The absolute path of the subtree that requires authentication
     * @param loginPath Optional login path
     */
    void requirementAdded(@Nonnull String path, @Nullable String loginPath);

    /**
     * An existing authentication requirement has been removed at the given {@code path}.
     *
     * @param path The absolute path of the subtree that no longer requires authentication
     * @param loginPath The login path that was defined with the requirement or
     * {@code null} if none was defined.
     */
    void requirementRemoved(@Nonnull String path, @Nullable String loginPath);

    /**
     * A login path has been added to an existing authentication requirement.
     *
     * @param path The absolute path of the subtree that requires authentication
     * @param loginPath The new login path that has been added.
     */
    void loginPathAdded(@Nonnull String path, @Nonnull String loginPath);

    /**
     * The login path defined with an existing authentication requirement has changed
     * to a new value.
     *
     * @param path The absolute path of the subtree that requires authentication
     * @param loginPathBefore The previous value of the login path.
     * @param loginPathAfter The new value of the login path.
     */
    void loginPathChanged(@Nonnull String path, @Nonnull String loginPathBefore, @Nonnull String loginPathAfter);

    /**
     * The login path defined with an existing authentication requirement has been removed.
     *
     * @param path The absolute path of the subtree that requires authentication
     * @param loginPath The login path that has been deleted.
     */
    void loginPathRemoved(@Nonnull String path, @Nonnull String loginPath);

    /**
     * Returns the login path for the given absolute path or {@code null} if no
     * matching login path can be found.
     *
     * @param path The absolute path for which to find the login path.
     * @return A login path or {@code null}
     */
    @CheckForNull
    String getLoginPath(@Nonnull String path);
}
