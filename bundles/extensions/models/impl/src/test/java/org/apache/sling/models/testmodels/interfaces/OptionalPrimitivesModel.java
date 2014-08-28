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
package org.apache.sling.models.testmodels.interfaces;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

@Model(adaptables = Resource.class)
public interface OptionalPrimitivesModel {

    @Inject @Optional
    public byte getByteValue();

    @Inject @Optional
    public short getShortValue();

    @Inject @Optional
    public int getIntValue();

    @Inject @Optional
    public long getLongValue();

    @Inject @Optional
    public float getFloatValue();

    @Inject @Optional
    public double getDoubleValue();

    @Inject @Optional
    public char getCharValue();

    @Inject @Optional
    public boolean getBooleanValue();

    @Inject @Optional
    public Byte getByteObjectValue();

    @Inject @Optional
    public Short getShortObjectValue();

    @Inject @Optional
    public Integer getIntObjectValue();

    @Inject @Optional
    public Long getLongObjectValue();

    @Inject @Optional
    public Float getFloatObjectValue();

    @Inject @Optional
    public Double getDoubleObjectValue();

    @Inject @Optional
    public Character getCharObjectValue();

    @Inject @Optional
    public Boolean getBooleanObjectValue();

}
