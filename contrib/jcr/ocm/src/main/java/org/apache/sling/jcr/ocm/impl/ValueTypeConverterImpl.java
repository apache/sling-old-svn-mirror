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

import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter;

/**
 * JCR Value Type Converter
 */
public class ValueTypeConverterImpl implements AtomicTypeConverter
{
    /*
     * (non-Javadoc)
     * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getValue(javax.jcr.ValueFactory, java.lang.Object)
     */
    public Value getValue(ValueFactory valueFactory, Object propValue)
    {
        if (propValue == null)
        {
            return null;
        }
        return (Value) propValue;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getObject(javax.jcr.Value)
     */
    public Object getObject(Value value)
    {
        return value;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverter#getXPathQueryValue(javax.jcr.ValueFactory, java.lang.Object)
     */
    public String getXPathQueryValue(ValueFactory valueFactory, Object object) {
        return "'" + object.toString() + "'";
    }
}
