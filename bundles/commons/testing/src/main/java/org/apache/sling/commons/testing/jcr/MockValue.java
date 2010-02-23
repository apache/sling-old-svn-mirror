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
package org.apache.sling.commons.testing.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class MockValue implements Value {

    private String stringValue;
    private boolean booleanValue;
    private Calendar calendarValue;
    private double doubleValue;
    private long longValue;
    private InputStream stream;
    private int propertyType;
    private BigDecimal decimal;

    public MockValue() {
    }

    public MockValue(String str) {
        stringValue = str;
        propertyType = PropertyType.STRING;
    }

    public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException {
        return booleanValue;
    }

    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
        return calendarValue;
    }

    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
        return doubleValue;
    }

    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
        return longValue;
    }

    public InputStream getStream() throws IllegalStateException, RepositoryException {
        return stream;
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
        return stringValue;
    }

    public int getType() {
        return propertyType;
    }


    public void setValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public void setValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public void setValue(Calendar calendarValue) {
      this.calendarValue = calendarValue;
    }

    public void setValue(double doubleValue) {
      this.doubleValue = doubleValue;
    }

    public void setValue(long longValue) {
      this.longValue = longValue;
    }

    public void setValue(InputStream stream) {
      this.stream = stream;
    }

    public void setDecimal(BigDecimal value) {
        this.decimal = value;
    }

    public Binary getBinary() throws RepositoryException {
        return null;
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return decimal;
    }
}
