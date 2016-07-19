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

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class TestHandler implements RequirementHandler {

    final Set<String[]> calls = new HashSet<String[]>();

    @Override
    public void requirementAdded(@Nonnull String path, @Nullable String loginPath) {
        appendCall("requirementAdded", path, loginPath);

    }

    @Override
    public void requirementRemoved(@Nonnull String path, @Nullable String loginPath) {
        appendCall("requirementRemoved", path, loginPath);
    }

    @Override
    public void loginPathAdded(@Nonnull String path, @Nonnull String loginPath) {
        appendCall("loginPathAdded", path, loginPath);
    }

    @Override
    public void loginPathChanged(@Nonnull String path, @Nonnull String loginPathBefore, @Nonnull String loginPathAfter) {
        appendCall("loginPathChanged", path, loginPathBefore, loginPathAfter);
    }

    @Override
    public void loginPathRemoved(@Nonnull String path, @Nonnull String loginPath) {
        appendCall("loginPathRemoved", path, loginPath);
    }

    @Override
    public String getLoginPath(@Nonnull String path) {
        return path + "/loginPath";
    }

    private void appendCall(@Nonnull String... args) {
        calls.add(args);
    }
}