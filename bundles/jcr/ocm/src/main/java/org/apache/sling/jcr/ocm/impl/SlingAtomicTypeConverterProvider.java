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
package org.apache.sling.jcr.ocm.impl;

import java.util.Map;

import javax.jcr.Value;

import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.UndefinedTypeConverterImpl;

/**
 * The <code>SlingAtomicTypeConverterProvider</code> TODO
 */
public class SlingAtomicTypeConverterProvider extends
        DefaultAtomicTypeConverterProvider {

    @SuppressWarnings("unchecked")
    protected Map registerDefaultAtomicTypeConverters() {
        Map converters = super.registerDefaultAtomicTypeConverters();

        // add undefined type converter for Object class (not entirely correct but ok)
        converters.put(Object.class, UndefinedTypeConverterImpl.class);

        // Value type converter for JCR Values
        converters.put(Value.class, ValueTypeConverterImpl.class);

        return converters;
    }
}
