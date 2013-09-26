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
package org.apache.sling.hc.healthchecks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.support.impl.DefaultLoginsHealthCheck;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultLoginsHealthCheckTest {
    
    private Result getTestResult(String login) throws Exception {
        final DefaultLoginsHealthCheck c = new DefaultLoginsHealthCheck();
        SetField.set(c, "logins", Arrays.asList(new String[] { login }));
        
        final SlingRepository repo = Mockito.mock(SlingRepository.class);
        SetField.set(c, "repository", repo);
        final Session s = Mockito.mock(Session.class);
        Mockito.when(repo.login(Matchers.any(Credentials.class))).thenAnswer(new Answer<Session>() {
            @Override
            public Session answer(InvocationOnMock invocation) {
                final SimpleCredentials c = (SimpleCredentials)invocation.getArguments()[0];
                if("admin".equals(c.getUserID())) {
                    return s;
                }
                return null;
            }
        });
        
        return c.execute();
    }
    
    @Test
    public void testHealthCheckFails() throws Exception {
        assertFalse("Expecting failed check", getTestResult("admin:admin").isOk());
    }
    
    @Test
    public void testHealthCheckSucceeds() throws Exception {
        assertTrue("Expecting successful check", getTestResult("FOO:bar").isOk());
    }
}