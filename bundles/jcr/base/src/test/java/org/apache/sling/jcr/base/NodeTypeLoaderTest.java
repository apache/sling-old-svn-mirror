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
package org.apache.sling.jcr.base;

import javax.jcr.RepositoryException;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeTypeLoaderTest {
    
    @Test
    public void testisReRegisterBuiltinNodeType() {
        assertFalse("Exception does not match", 
                NodeTypeLoader.isReRegisterBuiltinNodeType(new Exception()));
        assertFalse("Plain RepositoryException does not match", 
                NodeTypeLoader.isReRegisterBuiltinNodeType(new RepositoryException()));
        assertFalse("Non-reregister RepositoryException does not match", 
                NodeTypeLoader.isReRegisterBuiltinNodeType(new RepositoryException("builtin that's it")));
        assertTrue("Reregister RepositoryException matches", 
                NodeTypeLoader.isReRegisterBuiltinNodeType(
                        new RepositoryException("{http://www.jcp.org/jcr/mix/1.0}language: can't reregister built-in node type")));
    }
}
