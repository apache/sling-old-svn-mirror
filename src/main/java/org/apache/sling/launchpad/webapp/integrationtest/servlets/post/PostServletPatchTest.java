/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;

/**
 * Integration test of the @Patch method in the post servlet.
 */
public class PostServletPatchTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    public void testPatch() throws Exception {
        final NameValuePairList props = new NameValuePairList();
        
        // 1. create multi-value property
        props.add("prop@TypeHint", "String[]");
        props.add("prop", "alpha");
        props.add("prop", "beta");
        props.add("prop", "beta");
        props.add("prop", "gamma");
        props.add("prop", "epsilon"); // make sure both epsilons are kept, as they are not touched
        props.add("prop", "epsilon"); // by the patch operations below
        
        String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props, null, false);
        props.clear();
        
        // 2. update mv prop through Patch method
        props.add("prop@Patch", "true");
        //props.add("prop@TypeHint", "String[]");
        props.add("prop", "-beta");  // remove all betas
        props.add("prop", "+delta"); // add one delta
        props.add("prop", "+alpha"); // already exists, do not add a second alpha
        
        testClient.createNode(location, props, null, false);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("true", content, "out.println(data.prop.length == 5)");
        assertJavascript("alpha", content, "out.println(data.prop[0])");
        assertJavascript("gamma", content, "out.println(data.prop[1])");
        assertJavascript("epsilon", content, "out.println(data.prop[2])");
        assertJavascript("epsilon", content, "out.println(data.prop[3])");
        assertJavascript("delta", content, "out.println(data.prop[4])");
    }

    public void testInvalidPatch() throws Exception {
        final NameValuePairList props = new NameValuePairList();
        
        // 1. create multi-value property
        props.add("prop@TypeHint", "String[]");
        props.add("prop", "alpha");
        props.add("prop", "beta");
        props.add("prop", "gamma");
        props.add("prop", "delta");
        
        String location = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props, null, false);
        props.clear();
        
        // 2. update mv prop through Patch method
        // but use only invalid values
        props.add("prop@Patch", "true");
        props.add("prop", "wrong");
        props.add("prop", "#noop");
        
        testClient.createNode(location, props, null, false);
        String content = getContent(location + ".json", CONTENT_TYPE_JSON);
        assertJavascript("true", content, "out.println(data.prop.length == 4)");
        assertJavascript("alpha", content, "out.println(data.prop[0])");
        assertJavascript("beta", content, "out.println(data.prop[1])");
        assertJavascript("gamma", content, "out.println(data.prop[2])");
        assertJavascript("delta", content, "out.println(data.prop[3])");
    }
}
