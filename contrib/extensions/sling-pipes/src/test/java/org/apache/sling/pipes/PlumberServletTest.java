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
package org.apache.sling.pipes;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.pipes.impl.CustomWriter;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * testing the servlet logic (parameters & output)
 */
public class PlumberServletTest  extends AbstractPipeTest {

    String containersPath = PATH_PIPE + "/" + "containers";

    String dummyTreePath = containersPath + "/" + ContainerPipeTest.NN_DUMMYTREE;

    String writePath = PATH_PIPE + "/" + "write";

    String pipedWritePath = writePath + "/" + WritePipeTest.NN_PIPED;

    StringWriter stringResponse;

    SlingHttpServletResponse response;

    PlumberServlet servlet = new PlumberServlet();

    @Before
    public void setup() {
        super.setup();
        context.load().json("/plumber.json", PATH_PIPE);
        context.load().json("/container.json", containersPath);
        context.load().json("/write.json", writePath);
        servlet.plumber = plumber;
        stringResponse = new StringWriter();
        try {
            response = mockPlumberServletResponse(stringResponse);
        } catch (Exception e){

        }
    }

    private void assertDummyTree() throws JSONException {
        String finalResponse = stringResponse.toString();
        assertFalse("There should be a response", StringUtils.isBlank(finalResponse));
        JSONObject object = new JSONObject(finalResponse);
        assertEquals("response should be an obj with size value equals to 4", object.getInt(OutputWriter.KEY_SIZE), 4);
        assertEquals("response should be an obj with items value equals to a 4 valued array", object.getJSONArray(OutputWriter.KEY_ITEMS).length(), 4);
    }

    @Test
    public void testDummyTreeThroughRT() throws Exception {
        SlingHttpServletRequest request = mockPlumberServletRequest(context.resourceResolver(), dummyTreePath, null, null, null, null);
        servlet.execute(request, response, false);
        assertDummyTree();
    }

    @Test
    public void testDummyTreeThroughPlumber() throws Exception {
        SlingHttpServletRequest request = mockPlumberServletRequest(context.resourceResolver(), PATH_PIPE, dummyTreePath, null, null, null);
        servlet.execute(request, response, false);
        assertDummyTree();
    }

    @Test
    public void testWriteExecute() throws ServletException {
        SlingHttpServletRequest request = mockPlumberServletRequest(context.resourceResolver(), pipedWritePath, null, null, null, null);
        servlet.execute(request, response, true);
        String finalResponse = stringResponse.toString();
        assertFalse("There should be a response", StringUtils.isBlank(finalResponse));
        assertFalse("There should be no more pending changes", context.resourceResolver().hasChanges());
    }

    /**
     * in this test we execute a pipe that modifies content, with a flag mocking the GET request:
     * the execution should fail.
     */
    @Test
    public void testGetOnWriteExecute() throws ServletException {
        SlingHttpServletRequest request = mockPlumberServletRequest(context.resourceResolver(), pipedWritePath, null, null, null, null);
        boolean hasFailed = true;
        try {
            servlet.execute(request, response, false);
            hasFailed = false;
        } catch (Exception e){

        }
        assertTrue("Execution should have failed", hasFailed);
    }

    @Test
    public void testAdditionalBindingsAndWriter() throws Exception {
        String testBinding = "testBinding";
        String testBindingLength = testBinding + "Length";
        String bindingValue = "testBindingValue";
        String pathLengthParam = "pathLength";
        JSONObject bindings = new JSONObject("{'" + testBinding + "':'" + bindingValue + "'}");
        JSONObject respObject = new JSONObject("{'" + pathLengthParam + "':'${path.get(\"dummyGrandChild\").length}','" + testBindingLength + "':'${" + testBinding + ".length}'}");
        SlingHttpServletRequest request =
                mockPlumberServletRequest(context.resourceResolver(), dummyTreePath, null, bindings.toString(), respObject.toString(), null);
        servlet.execute(request, response, false);
        assertDummyTree();
        JSONObject response = new JSONObject(stringResponse.toString());
        JSONArray array = response.getJSONArray(OutputWriter.KEY_ITEMS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            assertNotNull("there should be an object returned at each time", object);
            String path = object.optString(CustomWriter.PATH_KEY);
            assertNotNull("the string path should be returned for each item, containing the path of the resource");
            String pathLength = object.optString(pathLengthParam);
            assertNotNull("there should be a pathLength param, as specified in the writer", pathLength);
            assertEquals("Pathlength should be the string representation of the path length", path.length() + "", pathLength);
            String testBindingLengthValue = object.optString(testBindingLength);
            assertNotNull("testBindingLength should be there", testBindingLengthValue);
            assertEquals("testBindingLength should be the string representation of the additional binding length",
                    bindingValue.length() + "", testBindingLengthValue);
        }
    }

    @Test
    public void testDryRun() throws Exception {
        SlingHttpServletRequest dryRunRequest =
                mockPlumberServletRequest(context.resourceResolver(), pipedWritePath, null, null, null, "true");
        servlet.execute(dryRunRequest, response, true);
        Resource resource = context.resourceResolver().getResource("/content/fruits");
        ValueMap properties = resource.adaptTo(ValueMap.class);
        assertFalse("property fruits shouldn't have been written", properties.containsKey("fruits"));
        SlingHttpServletRequest request =
                mockPlumberServletRequest(context.resourceResolver(), pipedWritePath, null, null, null, "false");
        servlet.execute(request, response, true);
        WritePipeTest.assertPiped(resource);
    }

    public static SlingHttpServletRequest mockPlumberServletRequest(ResourceResolver resolver,
                                                                    String path,
                                                                    String pathParam,
                                                                    String bindings,
                                                                    String writer,
                                                                    String dryRun){
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = resolver.getResource(path);
        when(request.getResourceResolver()).thenReturn(resolver);
        when(request.getResource()).thenReturn(resource);
        when(request.getParameter(PlumberServlet.PARAM_PATH)).thenReturn(pathParam);
        when(request.getParameter(PlumberServlet.PARAM_BINDINGS)).thenReturn(bindings);
        when(request.getParameter(CustomWriter.PARAM_WRITER)).thenReturn(writer);
        when(request.getParameter(BasePipe.DRYRUN_KEY)).thenReturn(dryRun);
        return request;
    }

    public static SlingHttpServletResponse mockPlumberServletResponse(StringWriter writer) throws IOException {
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
        PrintWriter printWriter = new PrintWriter(writer);
        when(response.getWriter()).thenReturn(printWriter);
        return response;
    }
}
