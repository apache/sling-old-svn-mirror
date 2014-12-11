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

    public MockProperty(final ItemData itemData, final Session session) {
        super(itemData, session);
        if (this.itemData.getValues() == null) {
            try {
                this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue("") });
            }
            catch (RepositoryException ex) {
                throw new RuntimeException("Initializing property failed.", ex);
            }
        }
    }

    private Value internalGetValue() throws RepositoryException {
        if (this.itemData.getValues().length > 1) {
            throw new ValueFormatException(this
                    + " is a multi-valued property, so it's values can only be retrieved as an array");
        } else {
            return this.itemData.getValues()[0];
        }
    }

    @Override
    public Value getValue() throws RepositoryException {
        return internalGetValue();
    }

    @Override
    public Value[] getValues() throws RepositoryException {
        Value[] valuesCopy = new Value[this.itemData.getValues().length];
        for (int i = 0; i < this.itemData.getValues().length; i++) {
            valuesCopy[i] = this.itemData.getValues()[i];
        }
        return valuesCopy;
    }

    @Override
    public void setValue(final Value newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { newValue });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final Value[] newValues) throws RepositoryException {
        Value[] values = new Value[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            values[i] = newValues[i];
        }
        this.itemData.setValues(values);
        this.itemData.setMultiple(true);
    }

    @Override
    public void setValue(final String newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final String[] newValues) throws RepositoryException {
        Value[] values = new Value[newValues.length];
        for (int i = 0; i < newValues.length; i++) {
            values[i] = getSession().getValueFactory().createValue(newValues[i]);
        }
        this.itemData.setValues(values);
        this.itemData.setMultiple(true);
    }

    @Override
    public void setValue(final InputStream newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { new BinaryValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final long newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final double newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final Calendar newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final boolean newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final Node newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final Binary newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { new BinaryValue(newValue) });
        this.itemData.setMultiple(false);
    }

    @Override
    public void setValue(final BigDecimal newValue) throws RepositoryException {
        this.itemData.setValues(new Value[] { getSession().getValueFactory().createValue(newValue) });
        this.itemData.setMultiple(false);
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
        return this.itemData.getValues()[0].getType();
    }

    @Override
    public long getLength() throws RepositoryException {
        return getValue().getString().length();
    }

    @Override
    public long[] getLengths() throws RepositoryException {
        long[] lengths = new long[this.itemData.getValues().length];
        for (int i = 0; i < this.itemData.getValues().length; i++) {
            lengths[i] = this.itemData.getValues()[i].getString().length();
        }
        return lengths;
    }

    @Override
    public boolean isNode() {
        return false;
    }

    @Override
    public boolean isMultiple() throws RepositoryException {
        return this.itemData.isMultiple();
    }

    @Override
    public PropertyDefinition getDefinition() throws RepositoryException {
        return new MockPropertyDefinition();
    }

    @Override
    public int hashCode() {
        return itemData.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockProperty) {
            return itemData.equals(((MockProperty)obj).itemData);
        }
        return false;
    }
    
    // --- unsupported operations ---
    @Override
    public Node getNode() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    private final class MockPropertyDefinition implements PropertyDefinition {

        @Override
        public boolean isMultiple() {
            return MockProperty.this.itemData.isMultiple();
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
