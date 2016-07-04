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
package org.apache.sling.repoinit.jcr;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.repoinit.jcr.impl.TestUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Test the creation of paths with specific node types */
public class CreatePathsTest {
    
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    private TestUtil U;
    
    @Before
    public void setup() throws RepositoryException, IOException {
        U = new TestUtil(context);
        RepositoryUtil.registerSlingNodeTypes(U.adminSession);
    }

    @Test
    public void createSimplePath() throws Exception {
        final String path = "/one/two/three";
        U.parseAndExecute("create path " + path);
        U.assertNodeExists(path);
    }
    
    @Test
    public void createPathWithTypes() throws Exception {
        final String path = "/four/five(sling:Folder)/six(nt:folder)";
        U.parseAndExecute("create path " + path);
        U.assertNodeExists("/four", "nt:unstructured");
        U.assertNodeExists("/four/five", "sling:Folder");
        U.assertNodeExists("/four/five/six", "nt:folder");
    }
    
    @Test
    public void createPathWithSpecificDefaultType() throws Exception {
        final String path = "/seven/eight(nt:unstructured)/nine";
        U.parseAndExecute("create path (sling:Folder) " + path);
        U.assertNodeExists("/seven", "sling:Folder");
        U.assertNodeExists("/seven/eight", "nt:unstructured");
        U.assertNodeExists("/seven/eight/nine", "sling:Folder");
    }
    
    @Test
    public void createPathWithJcrDefaultType() throws Exception {
        final String path = "/ten/eleven(sling:Folder)/twelve";
        U.parseAndExecute("create path " + path);
        U.assertNodeExists("/ten", "nt:unstructured");
        U.assertNodeExists("/ten/eleven", "sling:Folder");
        U.assertNodeExists("/ten/eleven/twelve", "sling:Folder");
    }
}
