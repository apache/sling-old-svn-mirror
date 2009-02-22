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
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class MockValue implements Value {

    private final String value;
    
    public MockValue(String str) {
        value = str;
    }
    
    public boolean getBoolean() throws ValueFormatException, IllegalStateException, RepositoryException {
        return false;
    }

    public Calendar getDate() throws ValueFormatException, IllegalStateException, RepositoryException {
        return null;
    }

    public double getDouble() throws ValueFormatException, IllegalStateException, RepositoryException {
        return 0;
    }

    public long getLong() throws ValueFormatException, IllegalStateException, RepositoryException {
        return 0;
    }

    public InputStream getStream() throws IllegalStateException, RepositoryException {
        return null;
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
        return value;
    }

    public int getType() {
        return PropertyType.STRING;
    }

}
