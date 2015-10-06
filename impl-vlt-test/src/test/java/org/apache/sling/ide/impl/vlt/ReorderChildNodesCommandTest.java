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
package org.apache.sling.ide.impl.vlt;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.ResourceProxy;
import org.junit.Test;

public class ReorderChildNodesCommandTest {

    private Logger logger = new Slf4jLogger();

    @Test
    public void singleReordering() throws Exception {

        doReorderingTest(asList("first", "second", "third"), asList("first", "third", "second"));
    }

    @Test
    public void multipleReorderings() throws Exception {

        doReorderingTest(asList("first", "second", "third", "fourth"), asList("fourth", "second", "first", "third"));
    }

    @Test
    public void noReorderingNeeded() throws Exception {

        doReorderingTest(asList("first", "second", "third"), asList("first", "third", "second"));
    }

    @Test
    public void reorderingSkippedDueToDifferentChildren() throws Exception {

        doReorderingTest(asList("first", "second", "third"), asList("first", "fourth", "second"),
                asList("first", "second", "third"));
    }

    private void doReorderingTest(List<String> nodeNames, List<String> resourceNames) throws Exception {

        doReorderingTest(nodeNames, resourceNames, null);
    }

    private void doReorderingTest(List<String> nodeNames, List<String> resourceNames, List<String> expected)
            throws Exception {

        if (expected == null) {
            expected = resourceNames;
        }

        File out = new File(new File("target"), "jackrabbit");

        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);
        List<String> finalOrder;
        try {
            Node content = session.getRootNode().addNode("content");

            for (String nodeName : nodeNames) {
                content.addNode(nodeName);
            }

            session.save();

            ResourceProxy resource = newResource("/content", "nt:unstructured");

            for (String resourceName : resourceNames) {
                resource.addChild(newResource("/content/" + resourceName, "nt:unstructured"));
            }

            ReorderChildNodesCommand cmd = new ReorderChildNodesCommand(repo, credentials, resource, logger);
            cmd.execute().get();

            session.refresh(false);

            finalOrder = new ArrayList<>();

            NodeIterator nodes = session.getNode("/content").getNodes();
            while (nodes.hasNext()) {
                finalOrder.add(nodes.nextNode().getName());
            }
        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();
        }

        assertThat("Incorrect node order", finalOrder, equalTo(expected));
    }

    private ResourceProxy newResource(String path, String primaryType) {

        ResourceProxy resource = new ResourceProxy(path);
        resource.addProperty("jcr:primaryType", primaryType);
        return resource;
    }

}
