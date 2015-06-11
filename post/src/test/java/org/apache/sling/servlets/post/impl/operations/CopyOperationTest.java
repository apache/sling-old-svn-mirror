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
package org.apache.sling.servlets.post.impl.operations;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class CopyOperationTest extends TestCase {
    private Mockery context = new JUnit4Mockery();
    private int counter;
    
    private void assertResult(final String srcPath, final String destPath, Boolean expectedResult) throws RepositoryException {
        counter++;
        final Node src = context.mock(Node.class, "src" + counter);
        final Node dest = context.mock(Node.class, "dest" + counter);
        
        context.checking(new Expectations() {
            {
                allowing(src).getPath();
                will(returnValue(srcPath));
                allowing(dest).getPath();
                will(returnValue(destPath));
            }
        });

        final boolean result = CopyOperation.isAncestorOrSameNode(src, dest);
        assertEquals(
                "Expecting isAncestorOrSameNode to be " + expectedResult + " for " + srcPath + " and " + destPath,
                expectedResult.booleanValue(), result);
    }
    
    @Test
    public void testIsAncestorOrSameNode() throws RepositoryException {
        final Object [] testCases = {
                "/", "/", true,
                "/a", "/a", true,
                "/a/bee/ceee", "/a/bee/ceee", true,
                "/", "/tmp", true,
                "/a", "/a/b", true,
                "/a", "/a/b/c/dee/eeee", true,
                "/a", "/ab", false,
                "/ab/cd", "/ab/cde", false,
                "/ab", "/cd", false,
        };
        
        for(int i=0; i < testCases.length; i+=3) {
            assertResult((String)testCases[i], (String)testCases[i+1], (Boolean)(testCases[i+2]));
        }
        
    }
}