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
package org.apache.sling.sample.slingshot.ratings.impl;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.sample.slingshot.SlingshotConstants;
import org.apache.sling.sample.slingshot.ratings.RatingsService;
import org.apache.sling.sample.slingshot.ratings.RatingsUtil;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class RatingPostServletTest {
    
    @Rule
    public final SlingContext context = new SlingContext();
    
    @Test
    public void successfulSave() throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(RatingsUtil.PROPERTY_RATING, "5");
        
        context.registerService(RatingsService.class, Mockito.mock(RatingsService.class));
        
        RatingPostServlet servlet = context.registerInjectActivateService(new RatingPostServlet());

        MockSlingHttpServletRequest request = context.request();
        request.setRemoteUser("admin");
        request.setParameterMap(params);
        request.setResource(context.create().resource(SlingshotConstants.APP_ROOT_PATH+"/content/admin/travel"));

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        
        servlet.doPost(request, response);
        
        assertThat(response.getStatus(), Matchers.equalTo(SC_OK));
        String output = response.getOutputAsString();
        
        assertThat(output, equalTo("{  \"rating\" : 0}"));
        
    }

}
