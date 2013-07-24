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
package org.apache.sling.hc.sling.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.management.DynamicMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheckFacade;
import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RulesEngine;
import org.apache.sling.hc.sling.api.RulesResourceParser;
import org.apache.sling.hc.util.RuleDynamicMBean;
import org.apache.sling.hc.util.TaggedRuleFilter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers MBeans for our health check rules */
@Component(configurationFactory=true, policy=ConfigurationPolicy.REQUIRE, metatype=true)
public class RulesMBeans {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Property
    public static final String RULES_PATHS_PROP = "rules.path";
    
    @Property(value=RulesMBeans.DEFAULT_BEAN_NAME)
    public static final String BEAN_NAME_PROP = "bean.name";
    public static final String DEFAULT_BEAN_NAME = "rules";
    
    @Property(cardinality=20)
    public static final String RULE_TAGS_PROP = "rules.tags";
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private HealthCheckFacade healthcheck;
    
    @Reference
    private RulesResourceParser parser;
    
    private String rulesPath;
    private RulesEngine engine;
    private List<ServiceRegistration> mBeansRegistrations;
    private ResourceResolver resolver;
    private String beanName;
    private String [] rulesTags;
    
    @Activate
    public void activate(ComponentContext ctx) throws Exception {
        rulesPath = PropertiesUtil.toString(ctx.getProperties().get(RULES_PATHS_PROP), null);
        if(rulesPath == null) {
            throw new IllegalStateException("rulesPath is null, cannot activate");
        }
        
        beanName = PropertiesUtil.toString(ctx.getProperties().get(BEAN_NAME_PROP), DEFAULT_BEAN_NAME);
        rulesTags = PropertiesUtil.toStringArray(ctx.getProperties().get(RULE_TAGS_PROP), new String[] {});
        
        resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        final Resource rulesRoot = resolver.getResource(rulesPath);
        if(rulesRoot == null) {
            throw new IllegalStateException("Resource not found, cannot parse Rules: " + rulesPath);
        }

        // Parse Rules found under our configured root
        engine = healthcheck.getNewRulesEngine();
        final List<Rule> rules = parser.parseResource(rulesRoot); 
        engine.addRules(rules);
        
        // And register MBeans for those Rules
        mBeansRegistrations = new ArrayList<ServiceRegistration>();
        final String RESOURCE_PATH_PROP = "sling.resource.path";
        final TaggedRuleFilter filter = rulesTags.length > 0 ? new TaggedRuleFilter(rulesTags) : null;
        for(Rule r : rules) {
            if(filter != null && !filter.accept(r)) {
                log.debug("{} rejected by {}, ignoring this Rule", r, filter);
                continue;
            }
            final Object rulePath = r.getInfo().get(RESOURCE_PATH_PROP);
            if(rulePath == null) {
                // TODO this happens with scripted rules
                log.warn("Rule {} does not have a {} property, ignored", r, RESOURCE_PATH_PROP);
                continue;
            }
            final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
            mbeanProps.put("jmx.objectname", "org.apache.sling.healthcheck:type=" + beanName + ",service=" + rulePath);
            final RuleDynamicMBean mbean = new RuleDynamicMBean(r);
            mBeansRegistrations.add(ctx.getBundleContext().registerService(DynamicMBean.class.getName(), mbean, mbeanProps));
            log.debug("Registered {} with properties {}", mbean, mbeanProps);
        }
        log.info("Registered {} Rule MBeans", mBeansRegistrations.size());
    }
    
    @Deactivate
    public void deactivate(ComponentContext ctx) {
        for(ServiceRegistration r : mBeansRegistrations) {
            r.unregister();
        }
        log.info("Unregistered {} Rule MBeans", mBeansRegistrations.size());
        mBeansRegistrations = null;
        resolver.close();
        resolver = null;
    }
}
