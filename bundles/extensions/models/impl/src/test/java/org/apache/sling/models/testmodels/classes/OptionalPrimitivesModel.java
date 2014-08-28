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
package org.apache.sling.models.testmodels.classes;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables = Resource.class)
public class OptionalPrimitivesModel {

    @Inject @Optional
    private byte byteValue;

    @Inject @Optional
    private short shortValue;

    @Inject @Optional
    private int intValue;

    @Inject @Optional
    private long longValue;

    @Inject @Optional
    private float floatValue;

    @Inject @Optional
    private double doubleValue;

    @Inject @Optional
    private char charValue;

    @Inject @Optional
    private boolean booleanValue;

    @Inject @Optional
    private Byte byteObjectValue;

    @Inject @Optional
    private Short shortObjectValue;

    @Inject @Optional
    private Integer intObjectValue;

    @Inject @Optional
    private Long longObjectValue;

    @Inject @Optional
    private Float floatObjectValue;

    @Inject @Optional
    private Double doubleObjectValue;

    @Inject @Optional
    private Character charObjectValue;

    @Inject @Optional
    private Boolean booleanObjectValue;

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
