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

import org.eclipse.wst.server.core.IServerWorkingCopy;

public class SlingLaunchpadConfigurationDefaults {

    /**
     * Applies the default values to the specified <tt>workingCopy</tt>
     * 
     * <p>Currently sets the <tt>auto-publish-time</tt> and 
     * <tt>{@code ISlingLaunchpadServer.PROP_INSTALL_LOCALLY}</tt> values. It assumes that these
     * values were not previously set by the user and should only be used as a fallback when the
     * user has not specified them.</p>
     * 
     * <p>This method does not call {@link IServerWorkingCopy#save(boolean, org.eclipse.core.runtime.IProgressMonitor)},
     * it is the responsability of the caller to do so afterwards.</p>
     * 
     * @param workingCopy working copy
     */
    public static void applyDefaultValues(IServerWorkingCopy wc) {
        
        // auto-publish set to zero
        wc.setAttribute("auto-publish-time", 0);
        
        // only set publishing mechanism to 'local' for localhost
        wc.setAttribute(ISlingLaunchpadServer.PROP_INSTALL_LOCALLY, ServerUtil.runsOnLocalhost(wc));   
    }
}
