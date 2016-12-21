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
package org.apache.sling.api.wrappers;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Ignore;
import org.junit.Test;

public class ValueMapDecoratorConversionTest {
    
    private static final String STRING_1 = "item1";
    private static final String STRING_2 = "item2";
    private static final boolean BOOLEAN_1 = true;
    private static final boolean BOOLEAN_2 = false;
    private static final byte BYTE_1 = (byte)0x01;
    private static final byte BYTE_2 = (byte)0x02;
    private static final short SHORT_1 = (short)12;
    private static final short SHORT_2 = (short)34;
    private static final int INT_1 = 55;
    private static final int INT_2 = -123;
    private static final long LONG_1 = 1234L;
    private static final long LONG_2 = -4567L;
    private static final float FLOAT_1 = 1.23f;
    private static final float FLOAT_2 = -4.56f;
    private static final double DOUBLE_1 = 12.34d;
    private static final double DOUBLE_2 = -45.67d;
    private static final BigDecimal BIGDECIMAL_1 = new BigDecimal("12345.67");
    private static final BigDecimal BIGDECIMAL_2 = new BigDecimal("-23456.78");
    private static final Calendar CALENDAR_1 = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
    private static final Calendar CALENDAR_2 = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
    {
        CALENDAR_1.set(2016, 10, 15, 8, 20, 30);
        CALENDAR_2.set(2015, 6, 31, 19, 10, 20);
    }
    private static final Date DATE_1 = CALENDAR_1.getTime();
    private static final Date DATE_2 = CALENDAR_2.getTime();

    @Test
    public void testToString() {
        TestUtils.conv(STRING_1, STRING_2).to(STRING_1, STRING_2).test();
        TestUtils.convPrimitive(BOOLEAN_1, BOOLEAN_2).to(Boolean.toString(BOOLEAN_1), Boolean.toString(BOOLEAN_2)).test();
        TestUtils.conv(BOOLEAN_1, BOOLEAN_2).to(Boolean.toString(BOOLEAN_1), Boolean.toString(BOOLEAN_2)).test();
        TestUtils.convPrimitive(BYTE_1, BYTE_2).to(Byte.toString(BYTE_1), Byte.toString(BYTE_2)).test();
        TestUtils.conv(BYTE_1, BYTE_2).to(Byte.toString(BYTE_1), Byte.toString(BYTE_2)).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to(Short.toString(SHORT_1), Short.toString(SHORT_2)).test();
        TestUtils.conv(SHORT_1, SHORT_2).to(Short.toString(SHORT_1), Short.toString(SHORT_2)).test();
        TestUtils.convPrimitive(INT_1, INT_2).to(Integer.toString(INT_1), Integer.toString(INT_2)).test();
        TestUtils.conv(INT_1, INT_2).to(Integer.toString(INT_1), Integer.toString(INT_2)).test();
        TestUtils.convPrimitive(LONG_1, LONG_2).to(Long.toString(LONG_1), Long.toString(LONG_2)).test();
        TestUtils.conv(LONG_1, LONG_2).to(Long.toString(LONG_1), Long.toString(LONG_2)).test();
        TestUtils.convPrimitive(FLOAT_1, FLOAT_2).to(Float.toString(FLOAT_1), Float.toString(FLOAT_2)).test();
        TestUtils.conv(FLOAT_1, FLOAT_2).to(Float.toString(FLOAT_1), Float.toString(FLOAT_2)).test();
        TestUtils.convPrimitive(DOUBLE_1, DOUBLE_2).to(Double.toString(DOUBLE_1), Double.toString(DOUBLE_2)).test();
        TestUtils.conv(DOUBLE_1, DOUBLE_2).to(Double.toString(DOUBLE_1), Double.toString(DOUBLE_2)).test();
        TestUtils.conv(BIGDECIMAL_1, BIGDECIMAL_2).to(BIGDECIMAL_1.toString(), BIGDECIMAL_2.toString()).test();
        TestUtils.conv(CALENDAR_1, CALENDAR_2).to(CALENDAR_1.toString(), CALENDAR_2.toString()).test();
        TestUtils.conv(DATE_1, DATE_2).to(DATE_1.toString(), DATE_2.toString()).test();
    }
    
    @Test
    public void testToBoolean() {
        TestUtils.convPrimitive(BOOLEAN_1, BOOLEAN_2).to(BOOLEAN_1, BOOLEAN_2).test();
        TestUtils.conv(BOOLEAN_1, BOOLEAN_2).to(BOOLEAN_1, BOOLEAN_2).test();
        TestUtils.conv(Boolean.toString(BOOLEAN_1), Boolean.toString(BOOLEAN_2)).to(BOOLEAN_1, BOOLEAN_2).test();
        
        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Integer,Boolean>conv(INT_1, INT_2).toNull(Boolean.class).test();
        TestUtils.<Date,Boolean>conv(DATE_1, DATE_2).toNull(Boolean.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToBooleanPrimitive() {
        TestUtils.convPrimitive(BOOLEAN_1, BOOLEAN_2).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        TestUtils.conv(BOOLEAN_1, BOOLEAN_2).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        TestUtils.conv(Boolean.toString(BOOLEAN_1), Boolean.toString(BOOLEAN_2)).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        
        // test other types that should not be converted
        TestUtils.conv(INT_1, INT_2).toPrimitive(false,  false).nullValue(false).test();
        TestUtils.conv(DATE_1, DATE_2).toPrimitive(false,  false).nullValue(false).test();
    }
    
    @Test
    public void testToByte() {
        TestUtils.convPrimitive(BYTE_1, BYTE_2).to(BYTE_1, BYTE_2).test();
        TestUtils.conv(BYTE_1, BYTE_2).to(BYTE_1, BYTE_2).test();
        TestUtils.conv(Byte.toString(BYTE_1), Byte.toString(BYTE_2)).to(BYTE_1, BYTE_2).test();
        
        // test conversion from other number types
        TestUtils.conv(INT_1, INT_2).to((byte)INT_1, (byte)INT_2).test();
        TestUtils.convPrimitive(INT_1, INT_2).to((byte)INT_1, (byte)INT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Byte>conv(DATE_1, DATE_2).toNull(Byte.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToBytePrimitive() {
        TestUtils.convPrimitive(BYTE_1, BYTE_2).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();
        TestUtils.conv(BYTE_1, BYTE_2).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();
        TestUtils.conv(Byte.toString(BYTE_1), Byte.toString(BYTE_2)).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(INT_1, INT_2).toPrimitive((byte)INT_1, (byte)INT_2).test();
        TestUtils.convPrimitive(INT_1, INT_2).toPrimitive((byte)INT_1, (byte)INT_2).test();

        // test other types that should not be converted
        TestUtils.conv(INT_1, INT_2).toPrimitive((byte)0, (byte)0).nullValue(false).test();
    }
    
    @Test
    public void testToShort() {
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to(SHORT_1, SHORT_2).test();
        TestUtils.conv(SHORT_1, SHORT_2).to(SHORT_1, SHORT_2).test();
        TestUtils.conv(Short.toString(SHORT_1), Short.toString(SHORT_2)).to(SHORT_1, SHORT_2).test();
        
        // test conversion from other number types
        TestUtils.conv(INT_1, INT_2).to((short)INT_1, (short)INT_2).test();
        TestUtils.convPrimitive(INT_1, INT_2).to((short)INT_1, (short)INT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Short>conv(DATE_1, DATE_2).toNull(Short.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToShortPrimitive() {
        TestUtils.convPrimitive(SHORT_1, SHORT_2).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();
        TestUtils.conv(SHORT_1, SHORT_2).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();
        TestUtils.conv(Short.toString(SHORT_1), Short.toString(SHORT_2)).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(INT_1, INT_2).toPrimitive((short)INT_1, (short)INT_2).test();
        TestUtils.convPrimitive(INT_1, INT_2).toPrimitive((short)INT_1, (short)INT_2).test();

        // test other types that should not be converted
        TestUtils.conv(INT_1, INT_2).toPrimitive((short)0, (short)0).nullValue(false).test();
    }
    
    @Test
    public void testToInteger() {
        TestUtils.convPrimitive(INT_1, INT_2).to(INT_1, INT_2).test();
        TestUtils.conv(INT_1, INT_2).to(INT_1, INT_2).test();
        TestUtils.conv(Integer.toString(INT_1), Integer.toString(INT_2)).to(INT_1, INT_2).test();
        
        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).to((int)SHORT_1, (int)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to((int)SHORT_1, (int)SHORT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Integer>conv(DATE_1, DATE_2).toNull(Integer.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToIntegerPrimitive() {
        TestUtils.convPrimitive(INT_1, INT_2).toPrimitive(INT_1, INT_2).nullValue(0).test();
        TestUtils.conv(INT_1, INT_2).toPrimitive(INT_1, INT_2).nullValue(0).test();
        TestUtils.conv(Integer.toString(INT_1), Integer.toString(INT_2)).toPrimitive(INT_1, INT_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).toPrimitive((int)SHORT_1, (int)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).toPrimitive((int)SHORT_1, (int)SHORT_2).test();

        // test other types that should not be converted
        TestUtils.conv(INT_1, INT_2).toPrimitive((int)0, (int)0).nullValue(false).test();
    }
    
    @Test
    public void testToLong() {
        TestUtils.convPrimitive(LONG_1, LONG_2).to(LONG_1, LONG_2).test();
        TestUtils.conv(LONG_1, LONG_2).to(LONG_1, LONG_2).test();
        TestUtils.conv(Long.toString(LONG_1), Long.toString(LONG_2)).to(LONG_1, LONG_2).test();
        
        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).to((long)SHORT_1, (long)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to((long)SHORT_1, (long)SHORT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Long>conv(DATE_1, DATE_2).toNull(Long.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToLongPrimitive() {
        TestUtils.convPrimitive(LONG_1, LONG_2).toPrimitive(LONG_1, LONG_2).nullValue(0).test();
        TestUtils.conv(LONG_1, LONG_2).toPrimitive(LONG_1, LONG_2).nullValue(0).test();
        TestUtils.conv(Long.toString(LONG_1), Long.toString(LONG_2)).toPrimitive(LONG_1, LONG_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).toPrimitive((long)SHORT_1, (long)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).toPrimitive((long)SHORT_1, (long)SHORT_2).test();

        // test other types that should not be converted
        TestUtils.conv(LONG_1, LONG_2).toPrimitive((long)0, (long)0).nullValue(false).test();
    }
    
    @Test
    public void testToFloat() {
        TestUtils.convPrimitive(FLOAT_1, FLOAT_2).to(FLOAT_1, FLOAT_2).test();
        TestUtils.conv(FLOAT_1, FLOAT_2).to(FLOAT_1, FLOAT_2).test();
        TestUtils.conv(Float.toString(FLOAT_1), Float.toString(FLOAT_2)).to(FLOAT_1, FLOAT_2).test();
        
        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).to((float)SHORT_1, (float)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to((float)SHORT_1, (float)SHORT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Float>conv(DATE_1, DATE_2).toNull(Float.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToFloatPrimitive() {
        TestUtils.convPrimitive(FLOAT_1, FLOAT_2).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();
        TestUtils.conv(FLOAT_1, FLOAT_2).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();
        TestUtils.conv(Float.toString(FLOAT_1), Float.toString(FLOAT_2)).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).toPrimitive((float)SHORT_1, (float)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).toPrimitive((float)SHORT_1, (float)SHORT_2).test();

        // test other types that should not be converted
        TestUtils.conv(FLOAT_1, FLOAT_2).toPrimitive((float)0, (float)0).nullValue(false).test();
    }
 
    @Test
    public void testToDouble() {
        TestUtils.convPrimitive(DOUBLE_1, DOUBLE_2).to(DOUBLE_1, DOUBLE_2).test();
        TestUtils.conv(DOUBLE_1, DOUBLE_2).to(DOUBLE_1, DOUBLE_2).test();
        TestUtils.conv(Double.toString(DOUBLE_1), Double.toString(DOUBLE_2)).to(DOUBLE_1, DOUBLE_2).test();
        
        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).to((double)SHORT_1, (double)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).to((double)SHORT_1, (double)SHORT_2).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,Double>conv(DATE_1, DATE_2).toNull(Double.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support primitive
    public void testToDoublePrimitive() {
        TestUtils.convPrimitive(DOUBLE_1, DOUBLE_2).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();
        TestUtils.conv(DOUBLE_1, DOUBLE_2).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();
        TestUtils.conv(Double.toString(DOUBLE_1), Double.toString(DOUBLE_2)).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();

        // test conversion from other number types
        TestUtils.conv(SHORT_1, SHORT_2).toPrimitive((double)SHORT_1, (double)SHORT_2).test();
        TestUtils.convPrimitive(SHORT_1, SHORT_2).toPrimitive((double)SHORT_1, (double)SHORT_2).test();

        // test other types that should not be converted
        TestUtils.conv(DOUBLE_1, DOUBLE_2).toPrimitive((double)0, (double)0).nullValue(false).test();
    }
    
    @Test
    public void testToBigDecimal() {
        TestUtils.conv(BIGDECIMAL_1, BIGDECIMAL_2).to(BIGDECIMAL_1, BIGDECIMAL_2).test();
        TestUtils.conv(BIGDECIMAL_1.toString(), BIGDECIMAL_2.toString()).to(BIGDECIMAL_1, BIGDECIMAL_2).test();
        
        // test conversion from other number types
        TestUtils.conv(LONG_1, LONG_2).to(BigDecimal.valueOf(LONG_1), BigDecimal.valueOf(LONG_2)).test();
        TestUtils.convPrimitive(LONG_1, LONG_2).to(BigDecimal.valueOf(LONG_1), BigDecimal.valueOf(LONG_2)).test();
        TestUtils.conv(DOUBLE_1, DOUBLE_2).to(BigDecimal.valueOf(DOUBLE_1), BigDecimal.valueOf(DOUBLE_2)).test();
        TestUtils.convPrimitive(DOUBLE_1, DOUBLE_2).to(BigDecimal.valueOf(DOUBLE_1), BigDecimal.valueOf(DOUBLE_2)).test();

        // test other types that should not be converted
        // TODO: test is not successful yet
        /*
        TestUtils.<Date,BigDecimal>conv(DATE_1, DATE_2).toNull(BigDecimal.class).test();
        */
    }
    
    @Test
    @Ignore // TODO: support calendar
    public void testToCalendar() {
        TestUtils.conv(CALENDAR_1, CALENDAR_2).to(CALENDAR_1, CALENDAR_2).test();
        TestUtils.conv(CALENDAR_1.toString(), CALENDAR_2.toString()).to(CALENDAR_1, CALENDAR_2).test();
        
        // test conversion from other date types
        TestUtils.conv(DATE_1, DATE_2).to(DateUtils.toCalendar(DATE_1), DateUtils.toCalendar(DATE_2)).test();
        TestUtils.convPrimitive(DATE_1, DATE_2).to(DateUtils.toCalendar(DATE_1), DateUtils.toCalendar(DATE_2)).test();

        // test other types that should not be converted
        TestUtils.<String,Calendar>conv(STRING_1, STRING_2).toNull(Calendar.class).test();
        TestUtils.<Boolean,Calendar>conv(BOOLEAN_1, BOOLEAN_2).toNull(Calendar.class).test();
    }
    
    @Test
    @Ignore // TODO: support date
    public void testToDate() {
        TestUtils.conv(DATE_1, DATE_2).to(DATE_1, DATE_2).test();
        TestUtils.conv(DATE_1.toString(), DATE_2.toString()).to(DATE_1, DATE_2).test();
        
        // test conversion from other date types
        TestUtils.conv(CALENDAR_1, CALENDAR_2).to(CALENDAR_1.getTime(), CALENDAR_2.getTime()).test();
        TestUtils.convPrimitive(CALENDAR_1, CALENDAR_2).to(CALENDAR_1.getTime(), CALENDAR_2.getTime()).test();

        // test other types that should not be converted
        TestUtils.<String,Date>conv(STRING_1, STRING_2).toNull(Date.class).test();
        TestUtils.<Boolean,Date>conv(BOOLEAN_1, BOOLEAN_2).toNull(Date.class).test();
    }
    
}
