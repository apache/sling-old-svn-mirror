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
package org.apache.sling.ide.test.impl;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class ConnectionTest {

    @Rule
    public SlingWstServer serverRule = new SlingWstServer();

    @Test
    public void deployBundleOnServer() throws CoreException, InterruptedException, BackingStoreException {

        // TODO - needs configuration of a test sling launchpad instance, probably in the pom.xml

        // prevent status prompts, since it can lead to the test Eclipse instance hanging
        // TODO - move to rule/utility class
        IEclipsePreferences debugPrefs = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
        debugPrefs.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false);
        debugPrefs.flush();

        serverRule.waitForServerToStart();
    }
}
