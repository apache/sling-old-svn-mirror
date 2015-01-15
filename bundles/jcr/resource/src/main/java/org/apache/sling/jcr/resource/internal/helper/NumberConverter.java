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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * A converter for Number
 */
public class NumberConverter implements Converter {

    private final Number value;

    public NumberConverter(final Number val) {
        this.value = val;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.value.toString();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toLong()
     */
    public Long toLong() {
        return this.value.longValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toByte()
     */
    public Byte toByte() {
        return this.value.byteValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toShort()
     */
    public Short toShort() {
        return this.value.shortValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toInteger()
     */
    public Integer toInteger() {
        return this.value.intValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toDouble()
     */
    public Double toDouble() {
        return this.value.doubleValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toFloat()
     */
    public Float toFloat() {
        return this.value.floatValue();
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toCalendar()
     */
    public Calendar toCalendar() {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(this.toLong());
        return c;
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toDate()
     */
    public Date toDate() {
        return new Date(this.toLong());
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toBoolean()
     */
    public Boolean toBoolean() {
        return false;
    }

    /**
     * @see org.apache.sling.jcr.resource.internal.helper.Converter#toBigDecimal()
     */
    public BigDecimal toBigDecimal() {
        if ( this.value instanceof BigDecimal ) {
            return (BigDecimal)this.value;
        }
        return new BigDecimal(this.value.toString());
    }
}
