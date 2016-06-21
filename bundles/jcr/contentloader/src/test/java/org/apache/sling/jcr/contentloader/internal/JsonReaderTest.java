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
package org.apache.sling.jcr.contentloader.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.contentloader.ContentCreator;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class JsonReaderTest {

    protected JsonReader jsonReader;

    protected Mockery mockery = new JUnit4Mockery();

    protected ContentCreator creator;

    protected Sequence mySequence;

    protected void setReader() {
        this.jsonReader = new JsonReader();
    }

    @org.junit.Before public void setUp() throws Exception {
        setReader();
        this.creator = this.mockery.mock(ContentCreator.class);
        this.mySequence = this.mockery.sequence("my-sequence");
    }

    @org.junit.After public void tearDown() throws Exception {
        this.jsonReader = null;
    }

    @org.junit.Test public void testEmptyObject() throws Exception {
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse("");
    }

    @org.junit.Test public void testEmpty() throws IOException, RepositoryException {
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse("{}");
    }

    @org.junit.Test public void testDefaultPrimaryNodeTypeWithSurroundWhitespace() throws Exception {
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        String json = "     {  }     ";
        this.parse(json);
    }

    @org.junit.Test public void testDefaultPrimaryNodeTypeWithoutEnclosingBracesWithSurroundWhitespace() throws Exception {
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        String json = "             ";
        this.parse(json);
    }

    @org.junit.Test public void testExplicitePrimaryNodeType() throws Exception {
        final String type = "xyz:testType";
        String json = "{ \"jcr:primaryType\": \"" + type + "\" }";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, type, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testMixinNodeTypes1() throws Exception {
        final String[] mixins = new String[]{ "xyz:mix1" };
        String json = "{ \"jcr:mixinTypes\": " + this.toJsonArray(mixins) + "}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, mixins); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testMixinNodeTypes2() throws Exception {
        final String[] mixins = new String[]{ "xyz:mix1", "abc:mix2" };
        String json = "{ \"jcr:mixinTypes\": " + this.toJsonArray(mixins) + "}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, mixins); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesEmpty() throws Exception {
        String json = "{ \"property\": \"\"}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("property", PropertyType.UNDEFINED, ""); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesSingleValue() throws Exception {
        String json = "{ \"p1\": \"v1\"}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.UNDEFINED, "v1"); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesSingleDateValue() throws Exception {
        String json = "{ \"p1\": \"2009-09-24T16:32:57.948-07:00\"}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.DATE, "2009-09-24T16:32:57.948-07:00"); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesTwoSingleValue() throws Exception {
        String json = "{ \"p1\": \"v1\", \"p2\": \"v2\"}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.UNDEFINED, "v1"); inSequence(mySequence);
            allowing(creator).createProperty("p2", PropertyType.UNDEFINED, "v2"); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesMultiValue() throws Exception {
        String json = "{ \"p1\": [\"v1\"]}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.UNDEFINED, new String[] {"v1"}); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesMultiDateValue() throws Exception {
        String json = "{ \"p1\": [\"2009-09-24T16:32:57.948-07:00\"]}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.DATE, new String[] {"2009-09-24T16:32:57.948-07:00"}); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testPropertiesMultiValueEmpty() throws Exception {
        String json = "{ \"p1\": []}";

        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createProperty("p1", PropertyType.STRING, new String[0]); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testChild() throws Exception {
        String json = "{ " +
                      " c1 : {}" +
                      "}";
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createNode("c1", null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testChildWithMixin() throws Exception {
        String json = "{ " +
        " c1 : {" +
              "\"jcr:mixinTypes\" : [\"xyz:TestType\"]" +
              "}" +
        "}";
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createNode("c1", null, new String[] {"xyz:TestType"}); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testTwoChildren() throws Exception {
        String json = "{ " +
        " c1 : {}," +
        " c2 : {}" +
        "}";
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createNode("c1", null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).createNode("c2", null, null); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    @org.junit.Test public void testChildWithProperty() throws Exception {
        String json = "{ " +
        " c1 : {" +
        "      c1p1 : \"v1\"" +
              "}" +
        "}";
        this.mockery.checking(new Expectations() {{
            allowing(creator).createNode(null, null, null); inSequence(mySequence);
            allowing(creator).createNode("c1", null, null); inSequence(mySequence);
            allowing(creator).createProperty("c1p1", PropertyType.UNDEFINED, "v1");
            allowing(creator).finishNode(); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }


    @org.junit.Test public void testCreateAcl() throws Exception {
    	String json = " { " +
    			"\"security:acl\" : [ " +
    			"  { " +
    			"    \"principal\" : \"username1\"," +
    			"    \"granted\" : [\"jcr:read\",\"jcr:write\"]," +
    			"    \"denied\" : []" +
    			"  }," +
    			"  {" +
    			"    \"principal\" : \"groupname1\"," +
    			"    \"granted\" : [\"jcr:read\",\"jcr:write\"]" +
    			"  }," +
    			"  {" +
    			"    \"principal\" : \"groupname2\"," +
    			"    \"granted\" : [\"jcr:read\"]," +
    			"    \"denied\" : [\"jcr:write\"]," +
    			"    \"order\" : \"first\"" +
    			"  }" +
    			"]" +
    			"}";
        this.mockery.checking(new Expectations() {{
        	allowing(creator).createNode(null, null, null); inSequence(mySequence);

            allowing(creator).createAce("username1",new String[]{"jcr:read","jcr:write"},new String[]{}, null); inSequence(mySequence);
            allowing(creator).createAce("groupname1",new String[]{"jcr:read","jcr:write"},null, null); inSequence(mySequence);
            allowing(creator).createAce("groupname2",new String[]{"jcr:read"},new String[]{"jcr:write"}, "first"); inSequence(mySequence);
            allowing(creator).finishNode(); inSequence(mySequence);
        }});
        this.parse(json);
    }

    //---------- internal helper ----------------------------------------------

    protected void parse(String json) throws IOException, RepositoryException {
        String charSet = "ISO-8859-1";
        json = "#" + charSet + "\r\n" + json;
        InputStream ins = new ByteArrayInputStream(json.getBytes(charSet));
        this.jsonReader.parse(ins, this.creator);
    }

    protected JSONArray toJsonArray(String[] array) throws JSONException {
        return new JSONArray(Arrays.asList(array));
    }
}
