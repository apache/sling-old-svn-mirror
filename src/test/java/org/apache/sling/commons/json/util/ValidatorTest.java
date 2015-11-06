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
    }

    @Test(expected=JSONException.class)
    public void testTrailingCharsObject() throws JSONException {
        Validator.validate("{a:\"you\", b:2, c:true,}");
    }

}
