/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.hapi;

import org.apache.sling.hapi.impl.HApiPropertyImpl;
import org.apache.sling.hapi.impl.HApiTypeImpl;
import org.apache.sling.hapi.impl.MicrodataAttributeHelperImpl;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MicrodataAttributeHelperTest {

    public static final String SERVER_URL = "http://localhost:8080";
    public static final String TEST_PROP = "testProp";

    static HApiType propType = new HApiTypeImpl("propType", "propTypeDesc", SERVER_URL, "/a/b/propType", "a.b.propType", null,
            Collections.<String, HApiProperty>emptyMap(), null, false);
    static HApiProperty prop = new HApiPropertyImpl(TEST_PROP, "test property", propType, false);
    static Map<String, HApiProperty> props;
    static {
        Map<String, HApiProperty> map = new HashMap<String, HApiProperty>();
        map.put(TEST_PROP, prop);
        props = Collections.unmodifiableMap(map);
    }

    HApiType type = new HApiTypeImpl("testType", "testDescription", SERVER_URL, "/a/b/c/testType", "a.b.c.testType",
            null, props, null, false);

    MicrodataAttributeHelper helper = new MicrodataAttributeHelperImpl(null, type);

    @Test
    public void testItemType() throws Exception {
        final String itemtype = helper.itemtype();
        Assert.assertThat("itemtype attr is wrong or not present", itemtype,
                StringContains.containsString("itemtype=\"http://localhost:8080/a/b/c/testType.html\""));
        Assert.assertThat("itemscope attr is wrong or not present", itemtype,
                StringContains.containsString("itemscope=\"itemscope\""));
    }

    @Test
    public void testItemProp() throws Exception {
        final String itemprop = helper.itemprop(TEST_PROP);
        Assert.assertThat("itemprop attr is wrong or not present", itemprop,
                StringContains.containsString("itemprop=\"" + TEST_PROP + "\""));
        Assert.assertThat("itemscopee attr is wrong or not present", itemprop,
                StringContains.containsString("itemscope=\"itemscope\""));
        Assert.assertThat("itemscopee attr is wrong or not present", itemprop,
                StringContains.containsString("itemtype=\"http://localhost:8080/a/b/propType.html\""));
    }
}
