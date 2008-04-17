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
package org.apache.sling.scripting.wrapper;

import javax.jcr.Node;

import org.apache.sling.scripting.RepositoryScriptingTestBase;
import org.apache.sling.scripting.ScriptEngineHelper;

public class SlingWrapFactoryTest extends RepositoryScriptingTestBase {

    private Node node;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        node = getNewNode();
        node.addMixin("mix:versionable");
        getSession().save();

        node.setProperty("Modified", "Just making sure we have a second version");
        getSession().save();
    }

    public void testGetVersionHistoryNotWrapped() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", node);
        Object result = script.eval("node.getVersionHistory().getAllVersions()", data);
        assertNotNull(result);
    }

    public void testVersionNotWrapped() throws Exception {
        final ScriptEngineHelper.Data data = new ScriptEngineHelper.Data();
        data.put("node", node);
        Object result = script.eval("node.getBaseVersion().getCreated()", data);
        assertNotNull(result);
    }

}
