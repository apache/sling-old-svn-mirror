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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

public class MockProperty implements Property {

    private Value [] values = {};
    private final String name;
    
    public MockProperty(String name) {
        this.name = name;
    }
    
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return false;
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return null;
    }

    public PropertyDefinition getDefinition() throws RepositoryException {
        return new MockPropertyDefinition(values.length > 1);
    }

    public double getDouble() throws ValueFormatException, RepositoryException {
        return 0;
    }

    public long getLength() throws ValueFormatException, RepositoryException {
        return 0;
    }

    public long[] getLengths() throws ValueFormatException, RepositoryException {
        return null;
    }

    public long getLong() throws ValueFormatException, RepositoryException {
        return 0;
    }

    public Node getNode() throws ValueFormatException, RepositoryException {
        return null;
    }

    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return null;
    }

    public String getString() throws ValueFormatException, RepositoryException {
        if(values.length > 0) return values[0].getString();
        return null;
    }

    public int getType() throws RepositoryException {
        return PropertyType.STRING;
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        return new MockValue(getString());
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        return values;
    }

    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(double value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(String value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        values =new Value[1];
        values[0] = new MockValue(value);
    }

    public void setValue(String[] inputValues) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        this.values = new Value[inputValues.length];
        int i = 0;
        for(String str : inputValues) {
            values[i++] = new MockValue(str);
        }
    }

    public void setValue(Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
    }

    public void accept(ItemVisitor visitor) throws RepositoryException {
    }

    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return null;
    }

    public int getDepth() throws RepositoryException {
        return 0;
    }

    public String getName() throws RepositoryException {
        return name;
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return null;
    }

    public String getPath() throws RepositoryException {
        return null;
    }

    public Session getSession() throws RepositoryException {
        return null;
    }

    public boolean isModified() {
        return false;
    }

    public boolean isNew() {
        return false;
    }

    public boolean isNode() {
        return false;
    }

    public boolean isSame(Item otherItem) throws RepositoryException {
        return false;
    }

    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
    }

    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
    }

    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
            InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException,
            NoSuchNodeTypeException, RepositoryException {
    }

}
