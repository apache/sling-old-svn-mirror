/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.rules;

import org.apache.sling.testing.rules.util.Action;
import org.apache.sling.testing.clients.util.config.InstanceConfig;
import org.apache.sling.testing.clients.util.config.impl.EmptyInstanceConfig;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class InstanceConfigRule implements TestRule {
    public static final Logger LOG = LoggerFactory.getLogger(InstanceConfigRule.class);
    
    private final boolean withRestore;
    private InstanceConfig instanceConfig;
    private List<Action> actions;

    public <T extends InstanceConfig> InstanceConfigRule(T instanceConfig, boolean withRestore) {
        this.instanceConfig = instanceConfig;
        this.withRestore = withRestore;
        this.actions = new ArrayList<Action>();
    }

    public <T extends InstanceConfig> InstanceConfigRule(T instanceConfig) {
        this(instanceConfig, true);
    }

    /**
     * Uses an EmptyInstanceConfig (e.g. does nothing)
     */
    public InstanceConfigRule() {
        this(new EmptyInstanceConfig(), true);
    }

    public <T extends Action> InstanceConfigRule withAction(T action) {
        this.actions.add(action);
        return this;
    }

    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // save the instance config
                LOG.debug("Saving instance config {}", instanceConfig.getClass());
                instanceConfig.save();
                
                // Call any actions if any
                for (Action action : actions) {
                    LOG.debug("Calling action {}", action.getClass());
                    action.call();
                }
                
                // run the base statement
                LOG.debug("Running base statement");
                base.evaluate();
                
                if (withRestore) {
                    LOG.debug("Restoring instance config {}", instanceConfig.getClass());
                    instanceConfig.restore();
                }
            }
        };
    }
}
