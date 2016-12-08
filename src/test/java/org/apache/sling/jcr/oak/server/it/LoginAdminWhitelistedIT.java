/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.oak.server.it;

import javax.inject.Inject;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/** Test login admin with the test bundle whitelisted (which
 *  is a default from our base class)
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class LoginAdminWhitelistedIT extends OakServerTestSupport {

    @Inject
    private SlingRepository repository;
            
    @Inject
    private ResourceResolverFactory resolverFactory;
    
    @Test
    public void testLoginAdmin() throws Exception {
        repository.loginAdministrative(null).logout();
    }
    
    @Test
    public void testGetAdminResourceResolver() throws Exception {
        resolverFactory.getAdministrativeResourceResolver(null).close();
    }
}
