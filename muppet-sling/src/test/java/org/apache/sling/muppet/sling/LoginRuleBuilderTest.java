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
package org.apache.sling.muppet.sling;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.muppet.api.EvaluationResult;
import org.apache.sling.muppet.api.Rule;
import org.apache.sling.muppet.sling.impl.rules.LoginRuleBuilder;
import org.junit.Before;
import org.junit.Test;

public class LoginRuleBuilderTest {
    private LoginRuleBuilder builder;
    
    @Before
    public void setup() throws Exception {
        builder = new LoginRuleBuilder();
        final Field f = builder.getClass().getDeclaredField("repository");
        f.setAccessible(true);
        final SlingRepository r = RepositoryProvider.instance().getRepository();
        f.set(builder, r);
    }

    @Test
    public void testAdminLoginSucceeds() {
        final Rule r = builder.buildRule("sling", "login", "admin#admin", "LOGIN_OK");
        assertEquals("Expecting admin login to succeed", EvaluationResult.Status.OK, r.execute());
    }
    
    @Test
    public void testAdminBadPasswordFails() {
        final Rule r = builder.buildRule("sling", "login", "admin#bad", "OK");
        assertEquals("Expecting admin login to succeed", EvaluationResult.Status.ERROR, r.execute());
    }
    
    @Test
    public void testFooLoginFails() {
        final Rule r = builder.buildRule("sling", "login", "foo#bar", "OK");
        assertEquals("Expecting admin login to succeed", EvaluationResult.Status.ERROR, r.execute());
    }
}
