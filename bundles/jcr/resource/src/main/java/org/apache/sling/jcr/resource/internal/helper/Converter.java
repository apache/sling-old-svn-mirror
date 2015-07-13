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
 * A converter converts a value to a specific target type.
 */
public interface Converter {

    /**
     * Convert to Long.
     * @return Long representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Long toLong();

    /**
     * Convert to Byte.
     * @return Byte representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Byte toByte();

    /**
     * Convert to Short.
     * @return Short representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Short toShort();

    /**
     * Convert to Integer.
     * @return Integer representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Integer toInteger();

    /**
     * Convert to Double.
     * @return Double representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Double toDouble();

    /**
     * Convert to Float.
     * @return Float representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    Float toFloat();

    /**
     * Convert to Calendar.
     * @return Calendar representation of the converted value
     * @throws IllegalArgumentException  if the value cannot be parsed into a calendar
     */
    Calendar toCalendar();

    /**
     * Convert to Date.
     * @return Date representation of the converted value
     * @throws IllegalArgumentException  if the value cannot be parsed into a date
     */
    Date toDate();

    /**
     * Convert to boolean.
     * @return  Boolean representation of the converted value
     */
    Boolean toBoolean();

    /**
     * Convert to BigDecimal.
     * @return BigDecimal representation of the converted value
     * @throws NumberFormatException if the conversion fails
     */
    BigDecimal toBigDecimal();

}
