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
package org.apache.sling.bgservlets.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

/** Test the DeepNodeCreator class **/ 
public class DeepNodeCreatorTest {

    @Test
    public void testExistingNode() throws Exception {
        final Mockery mockery = new Mockery(); 
        final DeepNodeCreator c = new DeepNodeCreator();
        final String path = "/foo/bar";
        final Session s = mockery.mock(Session.class);
        final Node n = mockery.mock(Node.class);
        
        mockery.checking(new Expectations() {{
            allowing(s).itemExists(path);
            will(returnValue(true));

            allowing(s).getItem(path);
            will(returnValue(n));
            
            allowing(n).isNode();
            will(returnValue(true));
        }});

        final Node result = c.deepCreateNode(path, s, null);
        assertTrue("Expecting deepCreate to return existing node", result == n);
    }
    
    @Test
    public void testCreateFromRoot() throws Exception {
        final Mockery mockery = new Mockery(); 
        final DeepNodeCreator c = new DeepNodeCreator();
        final String rootPath = "/";
        final String fooPath = "/foo";
        final String barPath = "/foo/bar";
        final Session s = mockery.mock(Session.class);
        final Node root = mockery.mock(Node.class, rootPath);
        final Node foo = mockery.mock(Node.class, fooPath);
        final Node bar = mockery.mock(Node.class, barPath);
        final String testNodeType = "NT_TEST";
        
        mockery.checking(new Expectations() {{
            allowing(s).itemExists(barPath);
            will(returnValue(false));

            allowing(s).itemExists(fooPath);
            will(returnValue(false));

            allowing(s).itemExists(rootPath);
            will(returnValue(true));

            allowing(s).getItem(rootPath);
            will(returnValue(root));
            
            allowing(root).isNode();
            will(returnValue(true));
            
            allowing(root).addNode("foo", testNodeType);
            will(returnValue(foo));
            
            allowing(foo).addNode("bar", testNodeType);
            will(returnValue(bar));
            
            allowing(s).getRootNode();
            will(returnValue(root));
            
            allowing(s).save();
        }});
        
        final Node result = c.deepCreateNode(barPath, s, testNodeType);
        assertTrue("Expecting deepCreate to return created node", result == bar);
    }
    
    @Test
    public void testCreateWithVariousTypes() throws Exception {
        final Mockery mockery = new Mockery();
        
        final String fooPath = "/foo";
        final String barPath = "/foo/bar";
        final String wiiPath = "/foo/bar/wii";
        final Session s = mockery.mock(Session.class);
        final Node foo = mockery.mock(Node.class, fooPath);
        final Node bar = mockery.mock(Node.class, barPath);
        final Node wii = mockery.mock(Node.class, wiiPath);
        
        mockery.checking(new Expectations() {{
            allowing(s).itemExists(wiiPath);
            will(returnValue(false));

            allowing(s).itemExists(barPath);
            will(returnValue(false));

            allowing(s).itemExists(fooPath);
            will(returnValue(true));

            allowing(s).getItem(fooPath);
            will(returnValue(foo));
            
            allowing(foo).isNode();
            will(returnValue(true));
            
            allowing(foo).getPath();
            will(returnValue(fooPath));
            
            allowing(foo).addNode("bar", "NT_/foo.bar");
            will(returnValue(bar));
            
            allowing(bar).getPath();
            will(returnValue(barPath));
            
            allowing(bar).addNode("wii", "NT_/foo/bar.wii");
            will(returnValue(wii));
            
            allowing(s).getRootNode();
            
            allowing(s).save();
        }});
        
        final AtomicInteger counter = new AtomicInteger();
        final DeepNodeCreator c = new DeepNodeCreator() {

            @Override
            protected String getNodeType(Node parent, String childPath, String suggestedNodeType) 
            throws RepositoryException {
                return "NT_" + parent.getPath() + "." + childPath;
            }

            @Override
            protected void nodeCreated(Node n) throws RepositoryException {
                counter.addAndGet(1);
            }
        };
        final Node result = c.deepCreateNode(wiiPath, s, null);
        assertTrue("Expecting deepCreate to return created node", result == wii);
        assertEquals("Expecting correct count of nodeCreated calls", 2, counter.get());
    }
    
    @Test
    public void testCannotReadFoo() throws Exception {
        final Mockery mockery = new Mockery();
        final Session s = mockery.mock(Session.class);
        final String fooPath = "/foo";
        final Node foo = mockery.mock(Node.class, fooPath);
        final String barPath = "/foo/bar";
        final Node bar = mockery.mock(Node.class, barPath);
        final Node root = mockery.mock(Node.class, "/");
        
        mockery.checking(new Expectations() {{
            allowing(s).itemExists(with(any(String.class)));
            will(returnValue(false));
            
            allowing(s).itemExists(barPath);
            will(returnValue(true));
            
            allowing(s).getItem(fooPath);
            will(returnValue(foo));
            
            allowing(s).getItem(barPath);
            will(returnValue(bar));
            
            allowing(s).getRootNode();
            will(returnValue(root));
            
            allowing(root).addNode(with(any(String.class)), with(any(String.class)));
            will(throwException(new ItemExistsException("As if the child node was not readable")));
            
            allowing(s).save();
       }});
        
        final DeepNodeCreator c = new DeepNodeCreator();
        
        try {
            c.deepCreateNode("/foo/bar/something", s, "nt:unstructured");
            fail("Expecting an exception as /foo is not accessible");
        } catch(ItemExistsException asExpected) {
            // all is good
        }
    }
}