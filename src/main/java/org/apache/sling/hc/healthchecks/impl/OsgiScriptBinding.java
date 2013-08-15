/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.healthchecks.impl;

import org.apache.sling.hc.healthchecks.util.FormattingResultLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/** The OsgiBinding is meant to be bound as an "osgi" global variable
 *  in scripted rules, to allow for checking some OSGi states in
 *  a simple way
 */
public class OsgiScriptBinding {
    private final FormattingResultLog resultLog;
    private final BundleContext bundleContext;
    
    public OsgiScriptBinding(BundleContext ctx, FormattingResultLog resultLog) {
        this.resultLog = resultLog;
        this.bundleContext = ctx;
    }
    
    public int inactiveBundlesCount() {
        int count = 0;
        for(Bundle b : bundleContext.getBundles()) {
            if(!isActive(b)) {
                count++;
            }
        }
        resultLog.debug("inactiveBundlesCount={}", count);
        return count;
    }
    
    private boolean isActive(Bundle b) {
        boolean active = true;
        if(!isFragment(b) && Bundle.ACTIVE != b.getState()) {
            active = false;
            resultLog.info("Bundle {} is not active, state={} ({})", b.getSymbolicName(), b.getState(), b.getState());
        }
        return active;
    }
    
    private boolean isFragment(Bundle b) {
        final String header = (String) b.getHeaders().get( Constants.FRAGMENT_HOST );
        if(header!= null && header.trim().length() > 0) {
            resultLog.debug("{} is a fragment bundle, state won't be checked", b);
            return true;
        } else {
            return false;
        }
    }
}