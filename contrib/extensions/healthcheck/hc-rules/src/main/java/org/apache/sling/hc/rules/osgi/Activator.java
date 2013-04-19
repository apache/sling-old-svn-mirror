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
package org.apache.sling.muppet.rules.osgi;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.rules.jmx.JmxBeansRuleBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Register our {@link RuleBuilder} for OSGi */
public class Activator implements BundleActivator {
    private List<ServiceRegistration> regs;

    public void start(BundleContext ctx) throws Exception {
        regs = new ArrayList<ServiceRegistration>();
        regs.add(ctx.registerService(RuleBuilder.class.getName(), new BundlesRuleBuilder(ctx), null));
        regs.add(ctx.registerService(RuleBuilder.class.getName(), new JmxBeansRuleBuilder(), null));
    }

    public void stop(BundleContext ctx) throws Exception {
        if(regs != null) {
            for(ServiceRegistration reg : regs) {
                reg.unregister();
            }
        }
        regs = null;
    }
}
