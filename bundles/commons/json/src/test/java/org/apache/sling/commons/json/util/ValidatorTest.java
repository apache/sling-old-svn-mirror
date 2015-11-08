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
package org.apache.sling.commons.json.util;

import org.apache.sling.commons.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the Validator.
 */
public class ValidatorTest {

    @Test
    public void testSimpleJSON() throws JSONException {
        Validator.validate("");
        Validator.validate("[]");
        Validator.validate("{}");
    }

    @Test
    public void testBasicJSON() throws JSONException {
        Validator.validate("[1,true,\"hallo\"]");
        Validator.validate("{a:\"you\", b:2, c:true}");
    }

    @Test
    public void testNestedJSON() throws JSONException {
        Validator.validate("[1,true,\"hallo\", {a:1}, [1,2]]");
        Validator.validate("{a:\"you\", b:2, c:true, d: {d:1}, e: []}");
    }

    @Test(expected=JSONException.class)
    public void testTrailingCharsArray() throws JSONException {
        Validator.validate("[1,true,\"hallo\",]");
        //---------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTrailingCharsObject() throws JSONException {
        Validator.validate("{a:\"you\", b:2, c:true,}");
        //---------------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsArray1() throws JSONException {
        Validator.validate("[1,true,\"hallo\"]]");
        //----------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsArray2() throws JSONException {
        Validator.validate("[1,true,\"hallo\"]}");
        //----------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsArrayNested() throws JSONException {
        Validator.validate("{myobj:[1,true,\"hallo\"]],myobj2:5}");
        //-----------------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsObject1() throws JSONException {
        Validator.validate("{a:\"you\", b:2, c:true}}");
        //----------------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsObject2() throws JSONException {
        Validator.validate("{a:\"you\", b:2, c:true}]");
        //----------------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyClosingBracketsObjectNested() throws JSONException {
        Validator.validate("{myobj:{a:\"you\", b:2, c:true}},myobj2:5}");
        //-----------------------------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyOpeningBracketsArray() throws JSONException {
        Validator.validate("[[1,true,\"hallo\"]");
        //-----------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyOpeningBracketsArrayNested() throws JSONException {
        Validator.validate("{myobj:[[1,true,\"hallo\"],myobj2:5}");
        //------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyOpeningBracketsObject() throws JSONException {
        Validator.validate("{{a:\"you\", b:2, c:true}");
        //-----------invalid ^ 
    }

    @Test(expected=JSONException.class)
    public void testTooManyOpeningBracketsObjectNested() throws JSONException {
        Validator.validate("{myobj:{{a:\"you\", b:2, c:true},myobj2:5}");
        //------------------invalid ^ 
    }

    @Test(expected=JSONException.class)
    @Ignore
    public void testSLING_5276() throws JSONException {
        Validator.validate("[");
    }

}
