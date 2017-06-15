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
package org.apache.sling.models.jacksonexporter.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import org.apache.commons.collections.iterators.EnumerationIterator;

import java.io.IOException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Trivial serializer for Enumeration types (needed for Servlet APIs) which leverages
 * the existing Jackson support for Iterators.
 */
public class EnumerationSerializer extends JsonSerializer<Enumeration> implements ResolvableSerializer {

    private JsonSerializer<Object> iteratorSerializer;

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        this.iteratorSerializer = provider.findValueSerializer(Iterator.class, null);
    }

    @Override
    public void serialize(Enumeration value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        iteratorSerializer.serialize(new EnumerationIterator(value), jgen, provider);
    }
}
