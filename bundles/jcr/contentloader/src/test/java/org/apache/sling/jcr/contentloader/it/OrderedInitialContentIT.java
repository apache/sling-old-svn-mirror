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
package org.apache.sling.jcr.contentloader.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.testing.junit.Retry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Bundle;

/** Test the SLING-5682 ordered content loading */
@RunWith(PaxExam.class)
public class OrderedInitialContentIT extends ContentBundleTestBase {
    
    protected TinyBundle setupTestBundle(TinyBundle b) throws IOException {
        b.set(SLING_INITIAL_CONTENT_HEADER, DEFAULT_PATH_IN_BUNDLE + ";path:=" + contentRootPath);
        addContent(b, DEFAULT_PATH_IN_BUNDLE, "ordered-content.ordered-json");
        return b;
    }
    
    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    public void bundleStarted() {
        final Bundle b = PaxExamUtilities.findBundle(bundleContext, bundleSymbolicName);
        assertNotNull("Expecting bundle to be found:" + bundleSymbolicName, b);
        assertEquals("Expecting bundle to be active:" + bundleSymbolicName, Bundle.ACTIVE, b.getState());
    }
    
    private void assertProperty(Session session, String path, String expected) throws RepositoryException {
        assertTrue("Expecting property " + path, session.itemExists(path));
        final String actual = session.getProperty(path).getString();
        assertEquals("Expecting correct value at " + path, expected, actual);
    }
    
    @Test
    @Retry(intervalMsec=RETRY_INTERVAL, timeoutMsec=RETRY_TIMEOUT)
    public void initialContentInstalled() throws RepositoryException {
        assertProperty(session, contentRootPath + "/ordered-content/first/title", "This comes first"); 
        assertProperty(session, contentRootPath + "/ordered-content/second/title", "This comes second"); 
    }
}