/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.jcr.webdav.impl;

import org.apache.sling.commons.contentdetection.ContentAwareMimeTypeService;
import org.apache.sling.commons.contentdetection.internal.ContentAwareMimeTypeServiceImpl;
import org.apache.sling.jcr.webdav.impl.servlets.SlingWebDavServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SlingWebDavServletTest {

    SlingWebDavServlet classUnderTest;
    ContentAwareMimeTypeService mockContentAwareMimeTypeService = new ContentAwareMimeTypeServiceImpl();



    @Before
    public void setup() {
        classUnderTest = new SlingWebDavServlet();
    }

    @Test
    public void testSetContentAwareMimeTypeService()
            throws NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {

        //Case 1: BASIC
        //Initializations
        this.setField(classUnderTest, "detectionMode",
                SlingWebDavServlet.MIME_DETECTION_MODE_BASIC);

        //The test run
        this.invoke(classUnderTest, "setContentAwareMimeTypeService");

        //Verification of expectations
        Assert.assertEquals(null, getField(classUnderTest, "contentAwareMimeTypeService"));

    }

    @Test
    public void testSetContentAwareMimeTypeServiceWithMandatoryService()
            throws NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        //Initializations
        this.setField(classUnderTest, "detectionMode",
                SlingWebDavServlet.MIME_DETECTION_MODE_CONTENT_AWARE_MANDATORY);
        this.setField(classUnderTest, "contentAwareMimeTypeServiceRef",
                mockContentAwareMimeTypeService);

        //The test run
        this.invoke(classUnderTest, "setContentAwareMimeTypeService");

        //Verification of expectations
        Assert.assertEquals(mockContentAwareMimeTypeService,
                getField(classUnderTest, "contentAwareMimeTypeService"));
    }

    @Test(expected = Exception.class)
    public void testSetContentAwareMimeTypeServiceWithMandatoryMissingService()
            throws NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        //Initializations
        this.setField(classUnderTest, "detectionMode",
                SlingWebDavServlet.MIME_DETECTION_MODE_CONTENT_AWARE_MANDATORY);
        this.setField(classUnderTest, "contentAwareMimeTypeServiceRef", null);

        //The test run
        this.invoke(classUnderTest, "setContentAwareMimeTypeService");

    }

    @Test
    public void testSetContentAwareMimeTypeServiceWithOptionalService()
            throws NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        //Case 1: Optional Service present
        //Initializations
        this.setField(classUnderTest, "detectionMode",
                SlingWebDavServlet.MIME_DETECTION_MODE_CONTENT_AWARE_OPTIONAL);
        this.setField(classUnderTest, "contentAwareMimeTypeServiceRef",
                mockContentAwareMimeTypeService);

        //The test run
        this.invoke(classUnderTest, "setContentAwareMimeTypeService");

        //Verification of expectations
        Assert.assertEquals(mockContentAwareMimeTypeService,
                getField(classUnderTest, "contentAwareMimeTypeService"));

        //Case 2: Optional Service missing
        //Initializations
        this.setField(classUnderTest, "detectionMode",
                SlingWebDavServlet.MIME_DETECTION_MODE_CONTENT_AWARE_OPTIONAL);
        this.setField(classUnderTest, "contentAwareMimeTypeServiceRef", null);

        //The test run
        this.invoke(classUnderTest, "setContentAwareMimeTypeService");

        //Verification of expectations
        Assert.assertEquals(null,
                getField(classUnderTest, "contentAwareMimeTypeService"));

    }


    private void setField(Object classUnderTest, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = classUnderTest.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(classUnderTest, value);
    }

    private Object getField(Object classUnderTest, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = classUnderTest.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(classUnderTest);
    }

    private Object invoke(Object classUnderTest, String privateMethod, Object... params)
            throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {

        Class<?>[] classArray = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            classArray[i] = params[i].getClass();
        }
        Method method = classUnderTest.getClass().getDeclaredMethod(privateMethod, classArray);
        method.setAccessible(true);

        return method.invoke(classUnderTest, params);
    }
}
