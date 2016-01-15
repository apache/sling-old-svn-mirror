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

import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import javax.script.ScriptException;

/**
 * testing binding's expressions instanciations
 */
public class PipeBindingsTest extends AbstractPipeTest {

    private static final String NN_MOREBINDINGS = "moreBindings";

    @Before
    public void setup() {
        super.setup();
        context.load().json("/container.json", PATH_PIPE);
    }

    private PipeBindings getDummyTreeBinding(){
        Resource resource = context.resourceResolver().getResource(PATH_PIPE + "/" + ContainerPipeTest.NN_DUMMYTREE);
        return new PipeBindings(resource);
    }

    @Test
    public void testEvaluateSimpleString() throws ScriptException {
        PipeBindings bindings = getDummyTreeBinding();
        String simple = "simple string";
        String evaluated = (String)bindings.evaluate(simple);
        assertEquals("evaluated should be the same than input", evaluated, simple);
    }

    @Test
    public void computeEcma5Expression() {
        PipeBindings bindings = getDummyTreeBinding();
        Map<String,String> expressions = new HashMap<>();
        expressions.put("blah ${blah} blah", "'blah ' + blah + ' blah'");
        expressions.put("${blah}", "blah");
        expressions.put("${blah} blah", "blah + ' blah'");
        expressions.put("blah ${blah}", "'blah ' + blah");
        expressions.put("${blah}${blah}", "blah + '' + blah");
        expressions.put("+[${blah}]", "'+[' + blah + ']'");
        for (Map.Entry<String,String> test : expressions.entrySet()){
            assertEquals(test.getKey() + " should be transformed in " + test.getValue(), test.getValue(), bindings.computeECMA5Expression(test.getKey()));
        }
    }

    @Test
    public void testInstantiateExpression() throws Exception {
        PipeBindings bindings = getDummyTreeBinding();
        Map<String, String> testMap = new HashMap<>();
        testMap.put("a", "apricots");
        testMap.put("b", "bananas");
        bindings.getBindings().put("test", testMap);
        String newExpression = bindings.instantiateExpression("${test.a} and ${test.b}");
        assertEquals("expression should be correctly instantiated", "apricots and bananas", newExpression);
    }

    @Test
    public void testEvaluateNull() throws Exception {
        PipeBindings bindings = getDummyTreeBinding();
        assertNull("${null} object should be instantiated as null", bindings.instantiateObject("${null}"));
        assertNull("${null} expression should be instantiated as null", bindings.instantiateExpression("${null}"));
    }

    @Test
    public void testInstantiateObject() throws Exception {
        PipeBindings bindings = getDummyTreeBinding();
        Map<String, String> testMap = new HashMap<>();
        testMap.put("a", "apricots");
        testMap.put("b", "bananas");
        bindings.getBindings().put("test", testMap);
        String newExpression = (String)bindings.instantiateObject("${test.a} and ${test.b}");
        assertEquals("expression should be correctly instantiated", "apricots and bananas", newExpression);
        Calendar cal = (Calendar)bindings.instantiateObject("${new Date(2012,04,12)}");
        assertNotNull("calendar should be instantiated", cal);
        assertEquals("year should be correct", 2012, cal.get(Calendar.YEAR));
        assertEquals("month should be correct", 4, cal.get(Calendar.MONTH));
        assertEquals("date should be correct", 11, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testAdditionalBindings() throws Exception {
        Resource resource = context.resourceResolver().getResource(PATH_PIPE + "/" + NN_MOREBINDINGS);
        PipeBindings bindings = new PipeBindings(resource);
        String expression = bindings.instantiateExpression("${three}");
        assertEquals("computed expression should be taking additional bindings 'three' in account", "3", expression);
    }

    @Test
    public void testAdditionalScript() throws Exception {
        context.load().binaryFile("/testSum.js", "/content/test/testSum.js");
        Resource resource = context.resourceResolver().getResource(PATH_PIPE + "/" + NN_MOREBINDINGS);
        PipeBindings bindings = new PipeBindings(resource);
        Number expression = (Number)bindings.instantiateObject("${testSumFunction(1,2)}");
        assertEquals("computed expression have testSum script's functionavailable", 3, expression.intValue());
    }
}
