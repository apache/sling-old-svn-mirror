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
package org.apache.sling.testing.mock.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.value.BinaryValue;

/**
 * Mock {@link Property} implementation
 */
class MockProperty extends AbstractItem implements Property {

    private Value[] values;
    private boolean isMultiple;

    public MockProperty(final String path, final Session session) throws RepositoryException {
        super(path, session);
        this.values = new Value[] { getSession().getValueFactory().createValue("") };
    }

    private Value internalGetValue() throws ValueFormatException {
        if (this.values.length > 1) {
            throw new ValueFormatException(this
                    + " is a multi-valued property, so it's values can only be retrieved as an array");
        } else {
            return this.values[0];
        }
    }

    @Override
    public Value getValue() throws ValueFormatException {
        return internalGetValue();
    }

    @Override
    public Value[] getValues() {
        Value[] valuesCopy = new Value[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            valuesCopy[i] = this.values[i];
        }
        return valuesCopy;
    }

    @Override
    public void setValue(final Value newValue) {
        this.values = new Value[] { newValue };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final Value[] newValues) {
        this.values = new Value[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            this.values[i] = newValues[i];
        }
        this.isMultiple = true;
    }

    @Override
    public void setValue(final String newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final String[] newValues) throws RepositoryException {
        this.values = new Value[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            this.values[i] = getSession().getValueFactory().createValue(newValues[i]);
        }
        this.isMultiple = true;
    }

    @Override
    public void setValue(final InputStream newValue) throws RepositoryException {
        this.values = new Value[] { new BinaryValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final long newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final double newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final Calendar newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final boolean newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final Node newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final Binary newValue) throws RepositoryException {
        this.values = new Value[] { new BinaryValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public void setValue(final BigDecimal newValue) throws RepositoryException {
        this.values = new Value[] { getSession().getValueFactory().createValue(newValue) };
        this.isMultiple = false;
    }

    @Override
    public boolean getBoolean() throws RepositoryException {
        return internalGetValue().getBoolean();
    }

    @Override
    public Calendar getDate() throws RepositoryException {
        return internalGetValue().getDate();
    }

    @Override
    public double getDouble() throws RepositoryException {
        return internalGetValue().getDouble();
    }

    @Override
    public long getLong() throws RepositoryException {
        return internalGetValue().getLong();
    }

    @Override
    public String getString() throws RepositoryException {
        return internalGetValue().getString();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InputStream getStream() throws RepositoryException {
        return internalGetValue().getStream();
    }

    @Override
    public Binary getBinary() throws RepositoryException {
        return internalGetValue().getBinary();
    }

    @Override
    public BigDecimal getDecimal() throws RepositoryException {
        return internalGetValue().getDecimal();
    }

    @Override
    public int getType() throws RepositoryException {
        return this.values[0].getType();
    }

    @Override
    public long getLength() throws RepositoryException {
        return getValue().getString().length();
    }

    @Override
    public long[] getLengths() throws RepositoryException {
        long[] lengths = new long[this.values.length];
        for (int i = 0; i < this.values.length; i++) {
            lengths[i] = this.values[i].getString().length();
        }
        return lengths;
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isMultiple() {
        return this.isMultiple;
    }

    @Override
    public PropertyDefinition getDefinition() {
        return new MockPropertyDefinition();
    }

    // --- unsupported operations ---
    @Override
    public Node getNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty() {
        throw new UnsupportedOperationException();
    }

    private final class MockPropertyDefinition implements PropertyDefinition {

        @Override
        public boolean isMultiple() {
            return MockProperty.this.isMultiple();
        }

        // --- unsupported operations ---
        @Override
        public Value[] getDefaultValues() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getRequiredType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getValueConstraints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeType getDeclaringNodeType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getOnParentVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAutoCreated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isMandatory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isProtected() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getAvailableQueryOperators() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFullTextSearchable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isQueryOrderable() {
            throw new UnsupportedOperationException();
        }
    }

}
