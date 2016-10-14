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
package org.apache.sling.distribution.trigger.impl;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class AbstractJcrEventTriggerTest {

    private MockHelper helper;
    private MockResourceResolverFactory rrf;

    @Before
    public void setUp() throws Exception {
        rrf = new MockResourceResolverFactory();
        ResourceResolver resourceResolver = rrf.getResourceResolver(null);
        helper = MockHelper.create(resourceResolver).resource("/a").resource("b").resource("c").resource("d")
                .resource("e").resource("f").resource("g").p("foo", true).resource(".h").p("foo", false);
        helper.commit();
    }

    @Test//"SLING-6054"
    public void addToListTest() throws Exception {
        SlingRepository repository = mock(SlingRepository.class);
        Scheduler scheduler = mock(Scheduler.class);
        String path = "/";
        String serviceUser = "service-user";
        AbstractJcrEventTrigger trigger = new AbstractJcrEventTrigger(repository, scheduler, rrf, path, serviceUser) {
            @Override
            protected DistributionRequest processEvent(Event event) throws RepositoryException {
                return null;
            }
        };

        String descendant = "/a/b/c/d/e/f/h";
        String ancestor = "/a/b/c/d";

        List<DistributionRequest> requests = new LinkedList<DistributionRequest>();
        requests.add(new SimpleDistributionRequest(DistributionRequestType.ADD, descendant));
        DistributionRequest newRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, ancestor);
        trigger.addToList(newRequest, requests);

        assertEquals(1, requests.size());
        assertEquals(3, requests.get(0).getPaths().length);
        String[] paths = requests.get(0).getPaths();
        assertEquals(ancestor, paths[0]);
        assertEquals("/a/b/c/d/e/f/g", paths[1]); // the missing path is added
        assertEquals(descendant, paths[2]);

        // invert order of requests
        requests = new LinkedList<DistributionRequest>();
        requests.add(new SimpleDistributionRequest(DistributionRequestType.ADD, ancestor));
        newRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, descendant);
        trigger.addToList(newRequest, requests);

        assertEquals(1, requests.size());
        assertEquals(3, requests.get(0).getPaths().length);
        paths = requests.get(0).getPaths();
        assertEquals(ancestor, paths[0]);
        assertEquals("/a/b/c/d/e/f/g", paths[1]); // the missing path is added
        assertEquals(descendant, paths[2]);
    }

}