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
package org.apache.sling.ide.eclipse.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/**
 * Exposes all preferences bound to Eclipse's preference service with qualifier = Activator.PLUGIN_ID (i.e. bound to the core plugin symbolic name)
 * @see IPreferencesService
 */
public class Preferences {

    public static final String IGNORED_FILE_NAMES_FOR_SYNC = "ignoredFileNamesForSync";
    public static final String LIST_SEPARATOR = ";";

    private final IPreferencesService preferenceService;

    public Preferences() {
        // this is a hierachical view on the different scopes
        preferenceService = Platform.getPreferencesService();
    }

    public Set<String> getIgnoredFileNamesForSync() {
        return new HashSet<String>(Arrays.asList(preferenceService.getString(Activator.PLUGIN_ID, IGNORED_FILE_NAMES_FOR_SYNC, null, null).split(LIST_SEPARATOR)));
    }
}
