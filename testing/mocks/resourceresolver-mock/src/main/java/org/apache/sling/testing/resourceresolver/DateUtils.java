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
package org.apache.sling.testing.resourceresolver;

import java.util.Calendar;
import java.util.Date;

import org.apache.jackrabbit.util.ISO8601;

/**
 * This is copied from org.apache.sling.api.wrappers.impl.DateUtils
 * to avoid dependency to latest Sling API.
 * This can be removed when Sling API 2.17.0 or higher is referenced.
 */
final class DateUtils {
    
    private DateUtils() {
        // static methods only
    }

    /**
     * @param date Date value
     * @return Calendar value or null
     */
    public static Calendar toCalendar(Date input) {
        if (input == null) {
            return null;
        }
        Calendar result = Calendar.getInstance();
        result.setTime(input);
        return result;
    }

    /**
     * @param calendar Calendar value
     * @return Date value or null
     */
    public static Date toDate(Calendar input) {
        if (input == null) {
            return null;
        }
        return input.getTime();
    }
    
    /**
     * @param input Date value
     * @return ISO8601 string representation or null
     */
    public static String dateToString(Date input) {
        return calendarToString(toCalendar(input));
    }

    /**
     * @param input Calendar value
     * @return ISO8601 string representation or null
     */
    public static String calendarToString(Calendar input) {
        if (input == null) {
            return null;
        }
        return ISO8601.format(input);
    }

    /**
     * @param input ISO8601 string representation
     * @return Date value or null
     */
    public static Date dateFromString(String input) {
        return toDate(calendarFromString(input));
    }

    /**
     * @param input ISO8601 string representation
     * @return Calendar value or null
     */
    public static Calendar calendarFromString(String input) {
        if (input == null) {
            return null;
        }
        return ISO8601.parse(input);
    }

}
