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

import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.api.SystemAttribute;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** RuleBuilder about OSGi bundles */
public class BundlesRuleBuilder implements RuleBuilder {

    public static final String NAMESPACE = "osgi";
    public static final String BUNDLE_STATE_RULE = "bundle.state";
    public static final String BUNDLE_NOT_FOUND = "BUNDLE_NOT_FOUND";
    private final BundleContext bundleContext;
    
    static class BundleAttribute implements SystemAttribute {
        private final SystemAttribute attr;
        private final String name;
        
        BundleAttribute(String name, SystemAttribute attr) {
            this.name = name;
            this.attr = attr;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public Object getValue() {
            return attr.getValue();
        }
    }
    
    BundlesRuleBuilder(BundleContext ctx) {
        bundleContext = ctx;
    }
    
    private Bundle findBundle(String symbolicName) {
        for(Bundle b : bundleContext.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }
    
    private String bundleStateToString(int state) {
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
    
    @Override
    public Rule buildRule(String namespace, String ruleName, final String qualifier, String expression) {
        if(!NAMESPACE.equals(namespace)) {
            return null;
        }
        
        SystemAttribute attr = null;
        
        if(BUNDLE_STATE_RULE.equals(ruleName) && qualifier != null) {
            // Get the state of a bundle
            attr = new BundleAttribute(ruleName + ":" + qualifier, new SystemAttribute() {
                @Override
                public Object getValue() {
                    Bundle b = findBundle(qualifier);
                    if(b == null) {
                        return BUNDLE_NOT_FOUND;
                    } else {
                        return bundleStateToString(b.getState());
                    }
                }
            });
        }
        
        if(attr != null) {
            return new Rule(attr, expression);
        }
        
        return null;
    }
}
