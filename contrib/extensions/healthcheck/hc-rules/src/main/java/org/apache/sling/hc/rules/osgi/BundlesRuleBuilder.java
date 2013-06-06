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
package org.apache.sling.hc.rules.osgi;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.api.SystemAttribute;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;

/** RuleBuilder about OSGi bundles */
public class BundlesRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "osgi";
    public static final String BUNDLE_STATE_RULE = "bundle.state";
    public static final String BUNDLES_INACTIVE_RULE = "inactive.bundles.count";
    private final BundleContext ctx;
    
    class BundleStateAttribute implements SystemAttribute {
        private final String name;
        private final String qualifier;
        private final BundleContext ctx;
        
        BundleStateAttribute(BundleContext ctx, String name, String qualifier) {
            this.ctx = ctx;
            this.name = name;
            this.qualifier = qualifier;
        }
        
        @Override
        public String toString() {
            return name + ":" + qualifier;
        }
        
        @Override
        public Object getValue(Logger logger) {
            String result = null;
            Bundle b = findBundle(ctx, qualifier);
            if(b == null) {
                logger.error("Bundle not found: {}", qualifier);
            } else {
                result = bundleStateToString(b.getState());
                logger.debug("Bundle {} found, state={} ({})", 
                        new Object[] { b.getSymbolicName(), result, b.getState()});
            }
            return result;
        }
    }
    
    static class InactiveBundlesCount implements SystemAttribute {
        private final BundleContext ctx;
        
        InactiveBundlesCount(BundleContext ctx) {
            this.ctx = ctx;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
        
        @Override
        public Object getValue(Logger logger) {
            int inactiveCount=0;
            for(Bundle b : ctx.getBundles()) {
                if(!isFragment(b) && Bundle.ACTIVE != b.getState()) {
                    inactiveCount++;
                    logger.debug("Bundle {} is not active, state={} ({})", 
                            new Object[] { b.getSymbolicName(), b.getState(), bundleStateToString(b.getState())});
                }
            }
            
            if(inactiveCount > 0) {
                logger.debug("{} bundles found inactive", inactiveCount);
            }
            
            return inactiveCount;
        }
    }
    
    BundlesRuleBuilder(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    private static Bundle findBundle(BundleContext ctx, String symbolicName) {
        for(Bundle b : ctx.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }
    
    private static String bundleStateToString(int state) {
        // TODO this must exist somewhere already...
        if(state == Bundle.ACTIVE) {
            return "active";
        } else if(state == Bundle.RESOLVED) {
            return "resolved";
        } else if(state == Bundle.INSTALLED) {
            return "installed";
        } else if(state == Bundle.STOPPING) {
            return "stopping";
        } else if(state == Bundle.UNINSTALLED) {
            return "uninstalled";
        } else {
            return String.valueOf(state);
        }
    }
    
    private static boolean isFragment(Bundle b) {
        final String header = (String) b.getHeaders().get( Constants.FRAGMENT_HOST );
        return header!= null && header.trim().length() > 0;
    }
    
    @Override
    public Rule buildRule(String namespace, String ruleName, final String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace)) {
            return null;
        }
        
        if(BUNDLE_STATE_RULE.equals(ruleName) && qualifier != null) {
            return new Rule(new BundleStateAttribute(ctx, ruleName,qualifier), expression);
        } else if(BUNDLES_INACTIVE_RULE.equals(ruleName)) {
            return new Rule(new InactiveBundlesCount(ctx), expression);
        }
        
        return null;
    }
}