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
package org.apache.sling.api.wrappers.impl;

import org.junit.Ignore;
import org.junit.Test;

/**
 * This test suite is DISABLED.
 * Primitive types as "type argument" for ValueMap conversions are currently not supported
 * (and are also not supported by jcr.resource ValueMap implementation). 
 */
@Ignore
public class ObjectConverterPrimitiveTest {
    
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
    
    @Test
    public void testToBooleanPrimitive() {
        Convert.fromPrimitive(BOOLEAN_1, BOOLEAN_2).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        Convert.from(BOOLEAN_1, BOOLEAN_2).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        Convert.from(Boolean.toString(BOOLEAN_1), Boolean.toString(BOOLEAN_2)).toPrimitive(BOOLEAN_1, BOOLEAN_2).nullValue(false).test();
        
        // test other types that should not be converted
        Convert.from(INT_1, INT_2).toPrimitive(false,  false).nullValue(false).test();
    }
    
    @Test
    public void testToBytePrimitive() {
        Convert.fromPrimitive(BYTE_1, BYTE_2).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();
        Convert.from(BYTE_1, BYTE_2).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();
        Convert.from(Byte.toString(BYTE_1), Byte.toString(BYTE_2)).toPrimitive(BYTE_1, BYTE_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(INT_1, INT_2).toPrimitive((byte)INT_1, (byte)INT_2).test();
        Convert.fromPrimitive(INT_1, INT_2).toPrimitive((byte)INT_1, (byte)INT_2).test();

        // test other types that should not be converted
        Convert.from(INT_1, INT_2).toPrimitive((byte)0, (byte)0).nullValue(false).test();
    }
    
    @Test
    public void testToShortPrimitive() {
        Convert.fromPrimitive(SHORT_1, SHORT_2).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();
        Convert.from(SHORT_1, SHORT_2).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();
        Convert.from(Short.toString(SHORT_1), Short.toString(SHORT_2)).toPrimitive(SHORT_1, SHORT_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(INT_1, INT_2).toPrimitive((short)INT_1, (short)INT_2).test();
        Convert.fromPrimitive(INT_1, INT_2).toPrimitive((short)INT_1, (short)INT_2).test();

        // test other types that should not be converted
        Convert.from(INT_1, INT_2).toPrimitive((short)0, (short)0).nullValue(false).test();
    }
    
    @Test
    public void testToIntegerPrimitive() {
        Convert.fromPrimitive(INT_1, INT_2).toPrimitive(INT_1, INT_2).nullValue(0).test();
        Convert.from(INT_1, INT_2).toPrimitive(INT_1, INT_2).nullValue(0).test();
        Convert.from(Integer.toString(INT_1), Integer.toString(INT_2)).toPrimitive(INT_1, INT_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(SHORT_1, SHORT_2).toPrimitive((int)SHORT_1, (int)SHORT_2).test();
        Convert.fromPrimitive(SHORT_1, SHORT_2).toPrimitive((int)SHORT_1, (int)SHORT_2).test();

        // test other types that should not be converted
        Convert.from(INT_1, INT_2).toPrimitive((int)0, (int)0).nullValue(false).test();
    }
    
    @Test
    public void testToLongPrimitive() {
        Convert.fromPrimitive(LONG_1, LONG_2).toPrimitive(LONG_1, LONG_2).nullValue(0).test();
        Convert.from(LONG_1, LONG_2).toPrimitive(LONG_1, LONG_2).nullValue(0).test();
        Convert.from(Long.toString(LONG_1), Long.toString(LONG_2)).toPrimitive(LONG_1, LONG_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(SHORT_1, SHORT_2).toPrimitive((long)SHORT_1, (long)SHORT_2).test();
        Convert.fromPrimitive(SHORT_1, SHORT_2).toPrimitive((long)SHORT_1, (long)SHORT_2).test();

        // test other types that should not be converted
        Convert.from(LONG_1, LONG_2).toPrimitive((long)0, (long)0).nullValue(false).test();
    }
    
    @Test
    public void testToFloatPrimitive() {
        Convert.fromPrimitive(FLOAT_1, FLOAT_2).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();
        Convert.from(FLOAT_1, FLOAT_2).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();
        Convert.from(Float.toString(FLOAT_1), Float.toString(FLOAT_2)).toPrimitive(FLOAT_1, FLOAT_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(SHORT_1, SHORT_2).toPrimitive((float)SHORT_1, (float)SHORT_2).test();
        Convert.fromPrimitive(SHORT_1, SHORT_2).toPrimitive((float)SHORT_1, (float)SHORT_2).test();

        // test other types that should not be converted
        Convert.from(FLOAT_1, FLOAT_2).toPrimitive((float)0, (float)0).nullValue(false).test();
    }
 
    @Test
    public void testToDoublePrimitive() {
        Convert.fromPrimitive(DOUBLE_1, DOUBLE_2).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();
        Convert.from(DOUBLE_1, DOUBLE_2).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();
        Convert.from(Double.toString(DOUBLE_1), Double.toString(DOUBLE_2)).toPrimitive(DOUBLE_1, DOUBLE_2).nullValue(0).test();

        // test conversion from other number types
        Convert.from(SHORT_1, SHORT_2).toPrimitive((double)SHORT_1, (double)SHORT_2).test();
        Convert.fromPrimitive(SHORT_1, SHORT_2).toPrimitive((double)SHORT_1, (double)SHORT_2).test();

        // test other types that should not be converted
        Convert.from(DOUBLE_1, DOUBLE_2).toPrimitive((double)0, (double)0).nullValue(false).test();
    }
    
}
