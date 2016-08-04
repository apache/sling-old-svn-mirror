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
package org.apache.sling.sample.slingshot;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SlingshotUtilTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void loadData() {
        context.load().json("/slingshot.json", SlingshotConstants.APP_ROOT_PATH);
    }

    @Test
    public void getUserId_deepPath() {

        Resource resource = context.resourceResolver().getResource("/slingshot/users/admin/hobby");

        assertThat(SlingshotUtil.getUserId(resource), equalTo("admin"));
    }

    @Test
    public void getUserId_exactPath() {

        Resource resource = context.resourceResolver().getResource("/slingshot/users/admin");

        assertThat(SlingshotUtil.getUserId(resource), equalTo("admin"));
    }

    @Test
    public void getUserId_noMatch() {

        Resource resource = context.resourceResolver().getResource("/slingshot/users");

        assertThat(SlingshotUtil.getUserId(resource), nullValue());
    }

    @Test
    public void getContentPath_match() {

        Resource resource = context.resourceResolver().getResource("/slingshot/users/admin/hobby");

        assertThat(SlingshotUtil.getContentPath(resource), equalTo("/hobby"));
    }

    @Test
    public void getContentPath_noMatch() {

        Resource resource = context.resourceResolver().getResource("/slingshot/users/admin");

        assertThat(SlingshotUtil.getContentPath(resource), nullValue());
    }
}
