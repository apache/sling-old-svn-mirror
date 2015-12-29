/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.acldef.jcr;

import java.util.Random;

import javax.jcr.RepositoryException;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test the creation and delete of service users */
public class CreateServiceUsersTest {
    
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private static final Random random = new Random(42);
    private String namePrefix;
    private TestUtil U;
    
    @Before
    public void setup() throws RepositoryException {
        U = new TestUtil(context);
        namePrefix = "user_" + random.nextInt();
    }

    @Test
    public void createDeleteSingleTest() throws Exception {
        final String userId = namePrefix + "_cdst";
        U.assertServiceUser("at start of test", userId, false);
        U.parseAndExecute("create service user " + userId);
        U.assertServiceUser("after creating user", userId, true);
        U.parseAndExecute("delete service user " + userId);
        U.assertServiceUser("after deleting user", userId, false);
    }
    
    private String user(int index) {
        return namePrefix + "_" + index;
    }
    
    @Test
    public void createUserMultipleTimes() throws Exception {
        final String username = namePrefix + "_multiple";
        U.assertServiceUser("before test", username, false);
        final String input = "create service user " + username;
        for(int i=0; i < 50; i++) {
            U.parseAndExecute(input);
        }
        U.assertServiceUser("after creating it multiple times", username, true);
    }
    
    @Test
    public void createDeleteMultipleTest() throws Exception {
        final int n = 50;
        
        {
            final StringBuilder input = new StringBuilder();
            for(int i=0; i < n; i++) {
                U.assertServiceUser("at start of test", user(i), false);
                input.append("create service user ").append(user(i)).append("\n");
            }
            U.parseAndExecute(input.toString());
        }
        
        {
            final StringBuilder input = new StringBuilder();
            for(int i=0; i < n; i++) {
                U.assertServiceUser("before deleting user", user(i), true);
                input.append("delete service user ").append(user(i)).append("\n");
            }
            U.parseAndExecute(input.toString());
        }
        

        for(int i=0; i < n; i++) {
            U.assertServiceUser("after deleting users", user(i), false);
        }
    }
}
