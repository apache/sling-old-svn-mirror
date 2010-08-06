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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

/**
 * Integration test of reference type hints in the post servlet.
 */
public class ReferenceTypeHintTest extends HttpTestBase {
    public static final String TEST_BASE_PATH = "/sling-tests";
    private String postUrl;
    private String firstCreatedNodeUrl;
    private String firstUuid;
    private String firstPath;
    private String secondCreatedNodeUrl;
    private String secondUuid;
    private String secondPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();


        Map<String,String> m = new HashMap<String,String>();
        m.put("sv", "http://www.jcp.org/jcr/sv/1.0");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);


        final NameValuePairList props = new NameValuePairList();
        props.add("a", "");
        props.add("jcr:mixinTypes", "mix:referenceable");

        firstCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props, null, false);
        firstUuid = getProperty(firstCreatedNodeUrl, "jcr:uuid");
        firstPath = getPath(firstCreatedNodeUrl);

        secondCreatedNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props, null, false);
        secondUuid = getProperty(secondCreatedNodeUrl, "jcr:uuid");
        secondPath = getPath(secondCreatedNodeUrl);
    }

    public void testReferenceTypesCreatedFromUuids() throws Exception {
        final NameValuePairList props = new NameValuePairList();
        props.add("r", firstUuid);
        props.add("r@TypeHint", "Reference");
        props.add("w", firstUuid);
        props.add("w@TypeHint", "WeakReference");
        props.add("rs", firstUuid);
        props.add("rs", secondUuid);
        props.add("rs@TypeHint", "Reference[]");
        props.add("ws", firstUuid);
        props.add("ws", secondUuid);
        props.add("ws@TypeHint", "WeakReference[]");
        final String referencingNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props, null, false);

        verifyReferences(referencingNodeUrl);
    }

    public void testReferenceTypesCreatedFromPath() throws Exception {
        final NameValuePairList props = new NameValuePairList();
        props.add("r", firstPath);
        props.add("r@TypeHint", "Reference");
        props.add("w", firstPath);
        props.add("w@TypeHint", "WeakReference");
        props.add("rs", firstPath);
        props.add("rs", secondPath);
        props.add("rs@TypeHint", "Reference[]");
        props.add("ws", firstPath);
        props.add("ws", secondPath);
        props.add("ws@TypeHint", "WeakReference[]");
        final String referencingNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX,
                props, null, false);

        verifyReferences(referencingNodeUrl);
    }

    private void verifyReferences(final String referencingNodeUrl) throws Exception {
        final String refCreatedValue = getProperty(referencingNodeUrl, "r");
        final String weakrefCreatedValue = getProperty(referencingNodeUrl, "w");

        final String[] refCreatedValues = getPropertyArray(referencingNodeUrl, "rs");
        final String[] weakrefCreatedValues = getPropertyArray(referencingNodeUrl, "ws");

        assertEquals(firstUuid, refCreatedValue);
        assertEquals(firstUuid, weakrefCreatedValue);
        assertEquals(firstUuid, refCreatedValues[0]);
        assertEquals(firstUuid, weakrefCreatedValues[0]);
        assertEquals(secondUuid, refCreatedValues[1]);
        assertEquals(secondUuid, weakrefCreatedValues[1]);

        final String sysView = getSystemView(referencingNodeUrl);

        XMLAssert.assertXpathEvaluatesTo("Reference", "//sv:property[@sv:name='r']/@sv:type", sysView);
        XMLAssert.assertXpathEvaluatesTo("WeakReference", "//sv:property[@sv:name='w']/@sv:type", sysView);
        XMLAssert.assertXpathEvaluatesTo("Reference", "//sv:property[@sv:name='rs']/@sv:type", sysView);
        XMLAssert.assertXpathEvaluatesTo("WeakReference", "//sv:property[@sv:name='ws']/@sv:type", sysView);
    }

    private String getSystemView(String url) throws IOException, SAXException {
        return getContent(url + ".sysview.xml", CONTENT_TYPE_XML);
    }

    private String getPath(String url) {
        return url.substring(HTTP_BASE_URL.length());
    }

    private String getProperty(String url, String name) throws Exception {
        return getContent(url + "/" + name + ".txt", CONTENT_TYPE_PLAIN);
    }

    private String[] getPropertyArray(String url, String name) throws Exception {
        JSONObject jo = new JSONObject(getContent(url + "/" + name + ".json", CONTENT_TYPE_JSON));
        JSONArray arr = jo.getJSONArray(name);
        String[] result = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            result[i] = arr.getString(i);
        }
        return result;
    }
}
