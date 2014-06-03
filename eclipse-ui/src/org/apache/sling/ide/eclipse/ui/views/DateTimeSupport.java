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
package org.apache.sling.ide.eclipse.ui.views;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

public class DateTimeSupport {

    public static Date parseAsDate(String vaultDateTime) {
        return parseAsCalendar(vaultDateTime).getTime();
    }
    
    public static Calendar parseAsCalendar(String vaultDateTime) {
        final Calendar result = DatatypeConverter.parseDateTime(vaultDateTime);
        return result;
    }
    
    public static String print(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return print(c);
    }
    
    public static String print(Calendar c) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        String result = sdf.format(c.getTime());
        SimpleDateFormat timezone = new SimpleDateFormat("Z");
        String timezoneStr = timezone.format(c.getTime());
        //make it ISO_8601 conform
        timezoneStr = timezoneStr.substring(0, timezoneStr.length()-2) + ":" + timezoneStr.substring(timezoneStr.length()-2);
        return result+timezoneStr;
    }
}
