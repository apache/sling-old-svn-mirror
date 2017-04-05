/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.junit.rules;

import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.instance.InstanceConfiguration;
import org.apache.sling.testing.junit.rules.instance.ExistingInstance;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.apache.sling.testing.serversetup.instance.SlingTestBase;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Junit Rule that allows access to a Sling instance.
 * It is wrapped by a {@link SlingClassRule}
 */
@SuppressWarnings("ALL")
public class SlingInstanceRule implements TestRule {

    // Until we extract this logic from SlingTestBase, we can just reuse it here
    // #getServerBaseUrl() starts the instance if needed, internally.
    private static final SlingTestBase S = new SlingTestBase();

    public static final InstanceConfiguration DEFAULT_INSTANCE =
            new InstanceConfiguration(URI.create(S.getServerBaseUrl()), "default");
    private static final Logger LOG = LoggerFactory.getLogger(SlingInstanceRule.class);

    /** Sling rules to be executed at class level */
    public final SlingClassRule slingClassRule = new SlingClassRule();


    /** ExistingInstance for default instance */
    public final Instance defaultInstance = new ExistingInstance().withRunMode("default").orDefault(DEFAULT_INSTANCE);

    protected TestRule ruleChain = RuleChain.outerRule(slingClassRule).around(defaultInstance);


    public <T extends SlingClient> T getAdminClient(Class<T> clientClass) {
        return defaultInstance.getClient(clientClass, S.getServerUsername(), S.getServerPassword());
    }

    public SlingClient getAdminClient() {
        return getAdminClient(SlingClient.class);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return ruleChain.apply(base, description);
    }
}
