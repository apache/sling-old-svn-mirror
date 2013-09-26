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
package org.apache.sling.hc.samples.impl;

import javax.script.Bindings;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.hc.util.FormattingResultLog;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Deactivate;

/** The OsgiBinding is meant to be bound as an "osgi" global variable
 *  in scripted rules, to allow for checking some OSGi states in
 *  a simple way.
 */
@Component
@Service
@Property(name="context", value="healthcheck")
public class OsgiScriptBindingsProvider implements BindingsValuesProvider {
    private BundleContext bundleContext;
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String OSGI_BINDING_NAME = "osgi";
    
    public static class OsgiBinding {
        private final BundleContext bundleContext;
        private final FormattingResultLog resultLog;
        
        public OsgiBinding(BundleContext bc, FormattingResultLog r) {
            bundleContext = bc;
            resultLog = r;
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
    
    @Activate
    protected void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        bundleContext = null;
    }
    
    @Override
    public void addBindings(Bindings b) {
        final String logBindingName = FormattingResultLog.class.getName();
        final Object resultLog = b.get(logBindingName);
        if(resultLog == null) {
            log.info("No {} found in Bindings, cannot activate {} binding", logBindingName, OSGI_BINDING_NAME);
            return;
        }
        try {
            b.put("osgi", new OsgiBinding(bundleContext, (FormattingResultLog)resultLog));
        } catch(Exception e) {
            log.error("Exception while activating " + OSGI_BINDING_NAME, e);
        }
    }
}