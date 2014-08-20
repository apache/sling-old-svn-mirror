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
package org.apache.sling.models.testmodels.classes.constructorinjection;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables = Resource.class)
public class OptionalPrimitivesModel {

    private final byte byteValue;
    private final short shortValue;
    private final int intValue;
    private final long longValue;
    private final float floatValue;
    private final double doubleValue;
    private final char charValue;
    private final boolean booleanValue;
    private final Byte byteObjectValue;
    private final Short shortObjectValue;
    private final Integer intObjectValue;
    private final Long longObjectValue;
    private final Float floatObjectValue;
    private final Double doubleObjectValue;
    private final Character charObjectValue;
    private final Boolean booleanObjectValue;

    @Inject
    public OptionalPrimitivesModel(
            @Optional byte byteValue,
            @Optional short shortValue,
            @Optional int intValue,
            @Optional long longValue,
            @Optional float floatValue,
            @Optional double doubleValue,
            @Optional char charValue,
            @Optional boolean booleanValue,
            @Optional Byte byteObjectValue,
            @Optional Short shortObjectValue,
            @Optional Integer intObjectValue,
            @Optional Long longObjectValue,
            @Optional Float floatObjectValue,
            @Optional Double doubleObjectValue,
            @Optional Character charObjectValue,
            @Optional Boolean booleanObjectValue) {
        this.byteValue = byteValue;
        this.shortValue = shortValue;
        this.intValue = intValue;
        this.longValue = longValue;
        this.floatValue = floatValue;
        this.doubleValue = doubleValue;
        this.charValue = charValue;
        this.booleanValue = booleanValue;
        this.byteObjectValue = byteObjectValue;
        this.shortObjectValue = shortObjectValue;
        this.intObjectValue = intObjectValue;
        this.longObjectValue = longObjectValue;
        this.floatObjectValue = floatObjectValue;
        this.doubleObjectValue = doubleObjectValue;
        this.charObjectValue = charObjectValue;
        this.booleanObjectValue = booleanObjectValue;
    }

    public byte getByteValue() {
        return this.byteValue;
    }

    public short getShortValue() {
        return this.shortValue;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public long getLongValue() {
        return this.longValue;
    }

    public float getFloatValue() {
        return this.floatValue;
    }

    public double getDoubleValue() {
        return this.doubleValue;
    }

    public char getCharValue() {
        return this.charValue;
    }

    public boolean getBooleanValue() {
        return this.booleanValue;
    }
    
    public Byte getByteObjectValue() {
        return this.byteObjectValue;
    }

    public Short getShortObjectValue() {
        return this.shortObjectValue;
    }

    public Integer getIntObjectValue() {
        return this.intObjectValue;
    }

    public Long getLongObjectValue() {
        return this.longObjectValue;
    }

    public Float getFloatObjectValue() {
        return this.floatObjectValue;
    }

    public Double getDoubleObjectValue() {
        return this.doubleObjectValue;
    }

    public Character getCharObjectValue() {
        return this.charObjectValue;
    }

    public Boolean getBooleanObjectValue() {
        return this.booleanObjectValue;
    }
    
}
