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
package org.apache.sling.testing.mock.sling.loader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractContentLoaderAutoCommitTest {

    private static String DEST_RES_NAME = "dest";
    private static String MIME_TYPE_JSON = "application/json";
    private static String CLP_CONTENT = "/json-import-samples/content.json";
    private static byte[] MEM_CONTENT = ("{"
            + "\"jcr:primaryType\":\"sling:Folder\""
            + "}").getBytes();

    private int destResCount = 1;

    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    protected abstract ResourceResolverType getResourceResolverType();

    @Test
    public void testJsonAutocommitExplicitly() {
        doTestJsonAutocommitExplicitly(false, new HasChangesAssertion());
        doTestJsonAutocommitExplicitly(true, new NoChangesAssertion());
    }

    private void doTestJsonAutocommitExplicitly(final boolean commit, final Runnable assertion) {
        final ContentLoader loader = context.load(commit);

        final Resource a1 = context.create().resource(nextDestResource());
        loader.json(CLP_CONTENT, a1, "child");
        assertion.run();

        loader.json(CLP_CONTENT, nextDestResource());
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        final Resource a3 = context.create().resource(nextDestResource());
        loader.json(data, a3, "child");
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.json(data2, nextDestResource());
        assertion.run();
    }

    @Test
    public void testJsonAutocommitImplicitly() {
        final ContentLoader loader = context.load();
        final Runnable assertion = new NoChangesAssertion();

        final Resource r = context.create().resource(nextDestResource());
        loader.json(CLP_CONTENT, r, "child");
        assertion.run();

        loader.json(CLP_CONTENT, nextDestResource());
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r2 = context.create().resource(nextDestResource());
        loader.json(data, r2, "child");
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.json(data2, nextDestResource());
        assertion.run();
    }

    @Test
    public void testBinaryFileAutocommitExplicitly() {
        doTestBinaryFileAutocommitExplicitly(false, new HasChangesAssertion());
        doTestBinaryFileAutocommitExplicitly(true, new NoChangesAssertion());
    }

    private void doTestBinaryFileAutocommitExplicitly(final boolean commit, final Runnable assertion) {
        final ContentLoader loader = context.load(commit);

        loader.binaryFile(CLP_CONTENT, nextDestResource());
        assertion.run();

        loader.binaryFile(CLP_CONTENT, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryFile(data, nextDestResource());
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryFile(data2, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data3 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r = context.create().resource(nextDestResource());
        loader.binaryFile(data3, r, "child");
        assertion.run();

        final InputStream data4 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r2 = context.create().resource(nextDestResource());
        loader.binaryFile(data4, r2, "child", MIME_TYPE_JSON);
        assertion.run();
    }

    @Test
    public void testBinaryFileAutocommitImplicitly() {
        final ContentLoader loader = context.load();
        final Runnable assertion = new NoChangesAssertion();

        loader.binaryFile(CLP_CONTENT, nextDestResource());
        assertion.run();

        loader.binaryFile(CLP_CONTENT, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryFile(data, nextDestResource());
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryFile(data2, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data3 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r = context.create().resource(nextDestResource());
        loader.binaryFile(data3, r, "child");
        assertion.run();

        final InputStream data4 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r2 = context.create().resource(nextDestResource());
        loader.binaryFile(data4, r2, "child", MIME_TYPE_JSON);
        assertion.run();
    }

    @Test
    public void testBinaryResourceAutocommitExplicitly() {
        doTestBinaryResourceAutocommitExplicitly(false, new HasChangesAssertion());
        doTestBinaryResourceAutocommitExplicitly(true, new NoChangesAssertion());
    }

    private void doTestBinaryResourceAutocommitExplicitly(final boolean commit, final Runnable assertion) {
        final ContentLoader loader = context.load(commit);

        loader.binaryResource(CLP_CONTENT, nextDestResource());
        assertion.run();

        loader.binaryResource(CLP_CONTENT, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryResource(data, nextDestResource());
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryResource(data2, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data3 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r = context.create().resource(nextDestResource());
        loader.binaryResource(data3, r, "child");
        assertion.run();

        final InputStream data4 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r2 = context.create().resource(nextDestResource());
        loader.binaryResource(data4, r2, "child", MIME_TYPE_JSON);
        assertion.run();
    }

    @Test
    public void testBinaryResourceAutocommitImplicitly() {
        final ContentLoader loader = context.load();
        final Runnable assertion = new NoChangesAssertion();

        loader.binaryResource(CLP_CONTENT, nextDestResource());
        assertion.run();

        loader.binaryResource(CLP_CONTENT, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryResource(data, nextDestResource());
        assertion.run();

        final InputStream data2 = new ByteArrayInputStream(MEM_CONTENT);
        loader.binaryResource(data2, nextDestResource(), MIME_TYPE_JSON);
        assertion.run();

        final InputStream data3 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r = context.create().resource(nextDestResource());
        loader.binaryResource(data3, r, "child");
        assertion.run();

        final InputStream data4 = new ByteArrayInputStream(MEM_CONTENT);
        final Resource r2 = context.create().resource(nextDestResource());
        loader.binaryResource(data4, r2, "child", MIME_TYPE_JSON);
        assertion.run();
    }

    private synchronized String nextDestResource() {
        return '/' + DEST_RES_NAME + destResCount++;
    }

    private class HasChangesAssertion implements Runnable {
        @Override
        public void run() {
            assertTrue(context.resourceResolver().hasChanges());
        }
    }

    private class NoChangesAssertion implements Runnable {
        @Override
        public void run() {
            assertFalse(context.resourceResolver().hasChanges());
        }
    }
}
