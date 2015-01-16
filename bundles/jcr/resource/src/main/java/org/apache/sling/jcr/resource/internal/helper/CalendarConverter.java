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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Calendar;
import java.util.Date;

import org.apache.jackrabbit.util.ISO8601;

/**
 * A converter for Calendar
 */
public class CalendarConverter extends NumberConverter implements Converter {

    private final Calendar value;

    public CalendarConverter(final Calendar val) {
        super(val.getTimeInMillis());
        this.value = val;
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toCalendar()
     */
    @Override
    public Calendar toCalendar() {
        return this.value;
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toDate()
     */
    @Override
    public Date toDate() {
        return this.value.getTime();
    }

    @Override
    public String toString() {
        return ISO8601.format(this.value);
    }
}
