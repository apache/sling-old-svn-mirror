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
package org.apache.sling.launchpad.webapp.integrationtest.servlets.post;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.commons.testing.integration.HttpTestBase;
import org.apache.sling.commons.testing.integration.NameValuePairList;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  checks if the date parsing for non jcr-dates works.
 */

public class SlingDateValuesTest extends HttpTestBase {

    private static final String ECMA_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    public static final String TEST_BASE_PATH = "/sling-tests";
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SimpleDateFormat[] testFormats = new SimpleDateFormat[]{
        new SimpleDateFormat(ECMA_FORMAT, Locale.US),
        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    };

    private String postUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        postUrl = HTTP_BASE_URL + TEST_BASE_PATH + "/" + System.currentTimeMillis();
    }

    private void doDateTest(String expected, String input, String expected2, String input2)
            throws IOException {
        final NameValuePairList props = new NameValuePairList();
        props.add("someDate", input);
        props.add("someDate@TypeHint", "Date");

        props.add("manyDates", input);
        props.add("manyDates", input2);
        props.add("manyDates@TypeHint", "Date[]");
        
        final String createdNodeUrl = testClient.createNode(postUrl + SlingPostConstants.DEFAULT_CREATE_SUFFIX, props, null, false);
        String content = getContent(createdNodeUrl + ".json", CONTENT_TYPE_JSON);

        // default behaviour writes empty string
        log.info("Expecting [{}] -> [{}] (single value)", input, expected);
        assertJavascript(expected, content, "out.println(data.someDate)");
        
        assertJavascript(expected, content, "out.println(data.manyDates[0])");
        
        log.info("Expecting [{}] -> [{}] (multi-value)", input2, expected2);
        assertJavascript(expected2, content, "out.println(data.manyDates[1])");
    }

    public void testDateValues() throws IOException {
        SimpleDateFormat ecmaFmt = new SimpleDateFormat(ECMA_FORMAT, Locale.US);
        Date now = new Date();
        Date date2 = new Date(10000042L);
        String nowStr = ecmaFmt.format(now);
        String date2Str = ecmaFmt.format(date2);
        for (SimpleDateFormat fmt: testFormats) {
            String testStr = fmt.format(now);
            String test2Str = fmt.format(date2);
            doDateTest(nowStr, testStr, date2Str, test2Str);
        }
    }
    
    
    public void testDateTimezones() throws IOException{
        
        final int [] offsets = { -14400000, -4200000, 14400000, 4300000 };
        for(int offset : offsets) {
            for(String tzId : TimeZone.getAvailableIDs(offset)) {
                final TimeZone tz =TimeZone.getTimeZone(tzId);
                final Calendar cal = Calendar.getInstance(tz);
                DateFormat fmt = new SimpleDateFormat(ECMA_FORMAT, Locale.ENGLISH);
                fmt.setTimeZone(tz);
                final String ecmaNow = fmt.format(cal.getTime());
                final String isoNow=ISO8601.format(cal);
                
                final Calendar cal2= Calendar.getInstance(tz);
                final String ecmaNow2 = fmt.format(cal2.getTime());
                final String isoNow2=ISO8601.format(cal2);   
                    
                doDateTest(ecmaNow, isoNow, ecmaNow2, isoNow2);
            }
        }
        
        
    }
    
}