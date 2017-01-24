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
package org.apache.sling.servlets.post;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.apache.sling.servlets.post.impl.helper.MockSlingHttpServlet3Request;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractPostOperationTest {

    @Test
    public void testRemainingPostfixCausesFailure() {
        TestingResourceResolver resourceResolver = new TestingResourceResolver();

        MockSlingHttpServlet3Request request = new MockSlingHttpServlet3Request("/test", null, null, null, null);
        request.setResourceResolver(resourceResolver);

        final PostOperation operation = new AbstractPostOperation() {
            @Override
            protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) throws RepositoryException {
                changes.add(Modification.onChange(ModificationType.CREATE, "/content/test"));
                changes.add(Modification.onChange(ModificationType.CREATE, "/content/test@Postfix"));
            }
        };

        HtmlResponse response = new HtmlResponse();
        operation.run(request, response, new SlingPostProcessor[0]);
        assertFalse(response.isSuccessful());
        assertFalse(resourceResolver.commitCalled);
        assertTrue(resourceResolver.revertCalled);
    }

    @Test
    public void testNoRemainingPostfixIsSuccessful() {
        TestingResourceResolver resourceResolver = new TestingResourceResolver();

        MockSlingHttpServlet3Request request = new MockSlingHttpServlet3Request("/test", null, null, null, null);
        request.setResourceResolver(resourceResolver);

        final PostOperation operation = new AbstractPostOperation() {
            @Override
            protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) throws RepositoryException {
                changes.add(Modification.onChange(ModificationType.CREATE, "/content/test"));
            }
        };

        HtmlResponse response = new HtmlResponse();
        operation.run(request, response, new SlingPostProcessor[0]);
        assertTrue(response.isSuccessful());
        assertTrue(resourceResolver.commitCalled);
        assertFalse(resourceResolver.revertCalled);
    }

    @Test
    public void testRemainingPostfixWithoutUnPostfixedIsSuccessful() {
        TestingResourceResolver resourceResolver = new TestingResourceResolver();

        MockSlingHttpServlet3Request request = new MockSlingHttpServlet3Request("/test", null, null, null, null);
        request.setResourceResolver(resourceResolver);

        final PostOperation operation = new AbstractPostOperation() {
            @Override
            protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes) throws RepositoryException {
                changes.add(Modification.onChange(ModificationType.CREATE, "/content/test@Postfix"));
            }
        };

        HtmlResponse response = new HtmlResponse();
        operation.run(request, response, new SlingPostProcessor[0]);
        assertTrue(response.isSuccessful());
        assertTrue(resourceResolver.commitCalled);
        assertFalse(resourceResolver.revertCalled);
    }

    private class TestingResourceResolver extends MockResourceResolver {
        private boolean revertCalled;
        private boolean commitCalled;

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == Session.class) {
                return null;
            } else {
                return super.adaptTo(type);
            }
        }

        @Override
        public boolean hasChanges() {
            return !commitCalled;
        }

        @Override
        public void commit() {
            commitCalled = true;
        }

        @Override
        public void revert() {
            revertCalled = true;
        }
    }
}
