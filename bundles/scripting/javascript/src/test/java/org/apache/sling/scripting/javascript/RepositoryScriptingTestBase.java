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
package org.apache.sling.scripting.javascript;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.naming.NamingException;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.scripting.javascript.internal.ScriptEngineHelper;


/** Base class for tests which need a Repository
 *  and scripting functionality */
public class RepositoryScriptingTestBase extends RepositoryTestBase {
    protected ScriptEngineHelper script;
    private int counter;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script = new ScriptEngineHelper();
    }
    
    protected Node getNewNode() throws RepositoryException, NamingException {
        return getTestRootNode().addNode("test-" + (++counter));
    }

}
