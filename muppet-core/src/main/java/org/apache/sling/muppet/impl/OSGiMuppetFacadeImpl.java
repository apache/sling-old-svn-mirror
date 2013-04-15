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
package org.apache.sling.muppet.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.muppet.api.MuppetFacade;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.api.RuleBuilder;
import org.apache.sling.muppet.api.RulesEngine;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/** {@link MuppetFacade} for an OSGi environment, where
 *  {@link RuleBuilder} are provided as OSGi services.
 */
public class OSGiMuppetFacadeImpl implements MuppetFacade {

    private TextRulesParser parser;
    private List<RuleBuilder> ruleBuilders;
    private final ServiceTracker ruleBuilderTracker;
    private int trackingCount;
    
    public OSGiMuppetFacadeImpl(BundleContext ctx) {
        ruleBuilderTracker = new ServiceTracker(ctx, RuleBuilder.class.getName(), null);
        ruleBuilderTracker.open();
    }
    
    public void close() {
        ruleBuilderTracker.close();
    }
    
    @Override
    public RulesEngine getNewRulesEngine() {
        return new RulesEngineImpl();
    }

    @Override
    public List<Rule> parseSimpleTextRules(Reader input) throws IOException {
        maybeSetupParser();
        return parser.parse(input);
    }
    
    @Override
    public List<RuleBuilder> getRuleBuilders() {
        maybeSetupParser();
        return Collections.unmodifiableList(ruleBuilders);
    }
    
    private void maybeSetupParser() {
        if(parser == null || ruleBuilderTracker.getTrackingCount() != trackingCount) {
            synchronized (this) {
                trackingCount = ruleBuilderTracker.getTrackingCount();
                parser = new TextRulesParser();
                ruleBuilders = new ArrayList<RuleBuilder>();
                final Object [] services = ruleBuilderTracker.getServices();
                for(Object o : services) {
                    final RuleBuilder rb = (RuleBuilder)o;
                    ruleBuilders.add(rb);
                    parser.addBuilder(rb);
                }
            }
        }
    }
}