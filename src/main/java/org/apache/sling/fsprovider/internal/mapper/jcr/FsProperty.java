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
package org.apache.sling.fsprovider.internal.mapper.jcr;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.fsprovider.internal.mapper.ContentFile;

/**
 * Simplified implementation of read-only content access via the JCR API.
 */
class FsProperty extends FsItem implements Property {
    
    private final String propertyName;
    private final Node node;
    
    public FsProperty(ContentFile contentFile, ResourceResolver resolver, String propertyName, Node node) {
        super(contentFile, resolver);
        this.propertyName = propertyName;
        this.node = node;
    }
    
    @Override
    public String getName() throws RepositoryException {
        return propertyName;
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return getNode();
    }

    @Override
    public Node getNode() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return node;
    }
    
    @Override
    public String getPath() throws RepositoryException {
        return super.getPath() + "/" + propertyName;
    }

    @Override
    public Value getValue() throws ValueFormatException, RepositoryException {
        return new FsValue(props, propertyName);
    }

    @Override
    public String getString() throws ValueFormatException, RepositoryException {
        return getValue().getString();
    }

    @SuppressWarnings("deprecation")
    @Override
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return getValue().getStream();
    }

    @Override
    public Binary getBinary() throws ValueFormatException, RepositoryException {
        return getValue().getBinary();
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        return getValue().getLong();
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        return getValue().getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return getValue().getDecimal();
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return getValue().getDate();
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return getValue().getBoolean();
    }

    @Override
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        if (!isMultiple()) {
            throw new ValueFormatException();
        }
        Object value = props.get(propertyName);
        int size = Array.getLength(value);
        Value[] result = new Value[size];
        for (int i=0; i<size; i++) {
            result[i] = new FsValue(props, propertyName, i);
        }
        return result;
    }

    @Override
    public boolean isMultiple() throws RepositoryException {
        Object value = props.get(propertyName);
        return value != null && value.getClass().isArray();
    }

    @Override
    public int getType() throws RepositoryException {
        return getValue().getType();
    }
    
    @Override
    public PropertyDefinition getDefinition() throws RepositoryException {
        return new FsPropertyDefinition(propertyName);
    }

    
    // --- unsupported methods ---
    
    @Override
    public void setValue(Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(String value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(double value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLength() throws ValueFormatException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        throw new UnsupportedOperationException();
    }

}
