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
package org.apache.sling.ide.impl.vlt;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.sling.ide.transport.FileInfo;

public class UpdateNodePropertiesCommand extends JcrCommand<Void> {

    private final Map<String, Object> serializationData;

    public UpdateNodePropertiesCommand(Repository jcrRepo, Credentials credentials, FileInfo fileInfo,
            Map<String, Object> serializationData) {

        // TODO - won't support serialization of full coverage nodes
        super(jcrRepo, credentials, PlatformNameFormat.getRepositoryPath(PathUtil.makePath(
                fileInfo.getRelativeLocation(), "")));

        this.serializationData = serializationData;
    }

    @Override
    protected Void execute0(Session session) throws RepositoryException, IOException {

        Node node = session.getNode(getPath());
        
        Set<String> propertiesToRemove = new HashSet<String>();
        PropertyIterator properties = node.getProperties();
        while ( properties.hasNext()) {
            propertiesToRemove.add(properties.nextProperty().getName());
        }
        
        propertiesToRemove.removeAll(serializationData.keySet());

        // TODO - review for completeness and filevault compatibility
        for (Map.Entry<String, Object> entry : serializationData.entrySet()) {

            String propertyName = entry.getKey();
            Object propertyValue = entry.getValue();
            Property property = null;

            if (node.hasProperty(propertyName)) {
                property = node.getProperty(propertyName);
            }

            if (property != null && property.getDefinition().isProtected()) {
                continue;
            }
            
            // TODO - we don't handle the case where the input no longer matches the property definition, e.g. type
            // change or multiplicity change
            
            boolean isMultiple = property != null && property.getDefinition().isMultiple();

            ValueFactory valueFactory = session.getValueFactory();
            Value value = null;
            Value[] values = null;

            if (propertyValue instanceof String) {
                if (isMultiple) {
                    values = toValueArray(new String[] { (String) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((String) propertyValue);
                }
            } else if (propertyValue instanceof String[]) {
                values = toValueArray((String[]) propertyValue, session);
            } else if (propertyValue instanceof Boolean) {
                if (isMultiple) {
                    values = toValueArray(new Boolean[] { (Boolean) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Boolean) propertyValue);
                }
            } else if (propertyValue instanceof Boolean[]) {
                values = toValueArray((Boolean[]) propertyValue, session);
            } else if (propertyValue instanceof Calendar) {
                if (isMultiple) {
                    values = toValueArray(new Calendar[] { (Calendar) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Calendar) propertyValue);
                }
            } else if (propertyValue instanceof Calendar[]) {
                values = toValueArray((Calendar[]) propertyValue, session);
            } else if (propertyValue instanceof Double) {
                if (isMultiple) {
                    values = toValueArray(new Double[] { (Double) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Double) propertyValue);
                }
            } else if (propertyValue instanceof Double[]) {
                values = toValueArray((Double[]) propertyValue, session);
            } else if (propertyValue instanceof BigDecimal) {
                if (isMultiple) {
                    values = toValueArray(new BigDecimal[] { (BigDecimal) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((BigDecimal) propertyValue);
                }
            } else if (propertyValue instanceof BigDecimal[]) {
                values = toValueArray((BigDecimal[]) propertyValue, session);
            } else if (propertyValue instanceof Long) {
                if (isMultiple) {
                    values = toValueArray(new Long[] { (Long) propertyValue }, session);
                } else {
                    value = valueFactory.createValue((Long) propertyValue);
                }
            } else if (propertyValue instanceof Long[]) {
                values = toValueArray((Long[]) propertyValue, session);
                // TODO - distinguish between weak vs strong references
            } else if (propertyValue instanceof UUID) {
                Node reference = session.getNodeByIdentifier(((UUID) propertyValue).toString());
                if (isMultiple) {
                    values = toValueArray(new UUID[] { (UUID) propertyValue }, session);
                } else {
                    value = valueFactory.createValue(reference);
                }

            } else if (propertyValue instanceof UUID[]) {
                values = toValueArray((UUID[]) propertyValue, session);
            } else {
                throw new IllegalArgumentException("Unable to handle value '" + propertyValue + "' for property '"
                        + propertyName + "'");
            }

            if (value != null) {
                node.setProperty(propertyName, value);
            } else if (values != null) {
                node.setProperty(propertyName, values);
            } else {
                throw new IllegalArgumentException("Unable to extract a value or a value array for property '"
                        + propertyName + "' with value '" + propertyValue + "'");
            }
        }
        
        for ( String propertyToRemove : propertiesToRemove ) {
            Property prop = node.getProperty(propertyToRemove);
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            prop.remove();
        }

        return null;

    }

    private Value[] toValueArray(String[] strings, Session session) throws RepositoryException {

        Value[] values = new Value[strings.length];

        for (int i = 0; i < strings.length; i++) {
            values[i] = session.getValueFactory().createValue(strings[i]);
        }

        return values;
    }

    private Value[] toValueArray(Boolean[] booleans, Session session) throws RepositoryException {

        Value[] values = new Value[booleans.length];

        for (int i = 0; i < booleans.length; i++) {
            values[i] = session.getValueFactory().createValue(booleans[i]);
        }

        return values;
    }

    private Value[] toValueArray(Calendar[] calendars, Session session) throws RepositoryException {

        Value[] values = new Value[calendars.length];

        for (int i = 0; i < calendars.length; i++) {
            values[i] = session.getValueFactory().createValue(calendars[i]);
        }

        return values;
    }

    private Value[] toValueArray(Double[] doubles, Session session) throws RepositoryException {

        Value[] values = new Value[doubles.length];

        for (int i = 0; i < doubles.length; i++) {
            values[i] = session.getValueFactory().createValue(doubles[i]);
        }

        return values;
    }

    private Value[] toValueArray(BigDecimal[] bigDecimals, Session session) throws RepositoryException {

        Value[] values = new Value[bigDecimals.length];

        for (int i = 0; i < bigDecimals.length; i++) {
            values[i] = session.getValueFactory().createValue(bigDecimals[i]);
        }

        return values;
    }

    private Value[] toValueArray(Long[] longs, Session session) throws RepositoryException {

        Value[] values = new Value[longs.length];

        for (int i = 0; i < longs.length; i++) {
            values[i] = session.getValueFactory().createValue(longs[i]);
        }

        return values;
    }

    private Value[] toValueArray(UUID[] uuids, Session session) throws RepositoryException {

        Value[] values = new Value[uuids.length];

        for (int i = 0; i < uuids.length; i++) {

            Node reference = session.getNodeByIdentifier(uuids[i].toString());

            values[i] = session.getValueFactory().createValue(reference);
        }

        return values;
    }

}
