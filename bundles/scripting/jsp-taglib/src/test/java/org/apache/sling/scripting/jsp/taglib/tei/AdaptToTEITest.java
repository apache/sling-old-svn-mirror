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
package org.apache.sling.scripting.jsp.taglib.tei;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

import java.util.Hashtable;

import static junit.framework.TestCase.assertEquals;

public class AdaptToTEITest {

    private static final Logger LOG = LoggerFactory.getLogger(AdaptToTEITest.class);

    @Test
    public void typeInference() {
        final AdaptToTEI adaptToTEI = new AdaptToTEI();
        final String className = "org.foo.Bar";
        final VariableInfo[] variableInfo = adaptToTEI.getVariableInfo(tagData(className));
        assertEquals(1, variableInfo.length);
        assertEquals(className, variableInfo[0].getClassName());
    }

    @Test
    public void typeInferenceNotPossible() {
        final AdaptToTEI adaptToTEI = new AdaptToTEI();
        final Object className = TagData.REQUEST_TIME_VALUE;
        final VariableInfo[] variableInfo = adaptToTEI.getVariableInfo(tagData(className));
        assertEquals(1, variableInfo.length);
        assertEquals(Object.class.getName(), variableInfo[0].getClassName());
    }

    private TagData tagData(final Object className) {
        final Hashtable<String, Object> map = new Hashtable<String, Object>();
        map.put(AdaptToTEI.ATTR_VAR, "foo");
        map.put(AdaptToTEI.ATTR_ADAPT_TO, className);
        return new TagData(map);
    }
}
