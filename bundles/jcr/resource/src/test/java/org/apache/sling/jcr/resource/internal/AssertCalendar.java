/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.resource.internal;

import org.junit.Assert;

import java.util.Calendar;

public final class AssertCalendar {

    /**
     * Asserts that two calendars are equal.
     * <p>
     * This differs from {@link Assert#assertEquals(Object, Object)} in the way how the time zone is compared. While
     * assertEquals expects the exact same {@link java.util.TimeZone} object, this simply compares the getTimeInMillis()
     * values - if those are equal both Calendar point to the same time.
     */
    public static void assertEqualsCalendar(Calendar expected, Calendar actual) {
        if (expected == null) {
            Assert.assertNull(actual);
        } else {
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        }
    }

}
