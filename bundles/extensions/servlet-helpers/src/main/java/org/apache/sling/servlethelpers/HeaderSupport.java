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
package org.apache.sling.servlethelpers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Manage HTTP headers for request and response.
 */
class HeaderSupport {

    private static final String RFC_1123_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final DateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat(RFC_1123_DATE_PATTERN, Locale.US);
    private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");
    static {
        RFC1123_DATE_FORMAT.setTimeZone(TIMEZONE_GMT);
    }

    private List<HeaderValue> headers = new ArrayList<HeaderValue>();

    private static class HeaderValue {

        private String key;
        private String value;

        public HeaderValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }

    }

    public void addHeader(String name, String value) {
        headers.add(new HeaderValue(name, value));
    }

    public void addIntHeader(String name, int value) {
        headers.add(new HeaderValue(name, Integer.toString(value)));
    }

    public void addDateHeader(String name, long date) {
        Calendar calendar = Calendar.getInstance(TIMEZONE_GMT, Locale.US);
        calendar.setTimeInMillis(date);
        headers.add(new HeaderValue(name, formatDate(calendar)));
    }

    public void setHeader(String name, String value) {
        removeHeaders(name);
        addHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        removeHeaders(name);
        addIntHeader(name, value);
    }

    public void setDateHeader(String name, long date) {
        removeHeaders(name);
        addDateHeader(name, date);
    }

    private void removeHeaders(String name) {
        for (int i = this.headers.size() - 1; i >= 0; i--) {
            if (StringUtils.equals(this.headers.get(i).getKey(), name)) {
                headers.remove(i);
            }
        }
    }

    public boolean containsHeader(String name) {
        return !getHeaders(name).isEmpty();
    }

    public String getHeader(String name) {
        Collection<String> values = getHeaders(name);
        if (!values.isEmpty()) {
            return values.iterator().next();
        } else {
            return null;
        }
    }

    public int getIntHeader(String name) {
        String value = getHeader(name);
        return NumberUtils.toInt(value);
    }

    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (StringUtils.isEmpty(value)) {
            return 0L;
        } else {
            try {
                return parseDate(value).getTimeInMillis();
            } catch (ParseException ex) {
                return 0L;
            }
        }
    }

    public Collection<String> getHeaders(String name) {
        List<String> values = new ArrayList<String>();
        for (HeaderValue entry : headers) {
            if (StringUtils.equals(entry.getKey(), name)) {
                values.add(entry.getValue());
            }
        }
        return values;
    }

    public Collection<String> getHeaderNames() {
        Set<String> values = new HashSet<String>();
        for (HeaderValue entry : headers) {
            values.add(entry.getKey());
        }
        return values;
    }

    public void reset() {
        headers.clear();
    }

    public static Enumeration<String> toEnumeration(Collection<String> collection) {
        return new Vector<String>(collection).elements();
    }

    private static synchronized String formatDate(Calendar date) {
        return RFC1123_DATE_FORMAT.format(date.getTime());
    }

    private static synchronized Calendar parseDate(String dateString) throws ParseException {
        RFC1123_DATE_FORMAT.parse(dateString);
        return RFC1123_DATE_FORMAT.getCalendar();
    }

}
