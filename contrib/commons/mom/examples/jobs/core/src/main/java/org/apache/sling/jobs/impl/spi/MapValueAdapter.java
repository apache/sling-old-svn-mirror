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
package org.apache.sling.jobs.impl.spi;

/**
 * Objects that can be converted to and from a Map are expected to extend this base class.
 * The values contained in the Map are expected to be Maps or values that can be serialised into most
 * common formats. It would be safe to use json or yaml as an example of a common format.
 * Created by ieb on 28/03/2016.
 *
 */
public interface MapValueAdapter {


    /**
     * Populate the object from a map value.
     * @param mapValue
     */
    void fromMapValue(Object mapValue);

    /**
     * Adapt the object into a value suitable for use in a map to be serialised by standard map -> json,yaml writers.
     * @return a value, which may be a primitive, an array or a map of primitives.
     */
    Object toMapValue();




}
