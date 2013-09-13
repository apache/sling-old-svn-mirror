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
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

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

        // TODO - review for completeness and filevault compatibility
        for (Map.Entry<String, Object> entry : serializationData.entrySet()) {

            if (node.hasProperty(entry.getKey())) {

                Property prop = node.getProperty(entry.getKey());
                if (prop.getDefinition().isProtected()) {
                    continue;
                }
            }

            if (entry.getValue() instanceof String) {
                node.setProperty(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof String[]) {
                node.setProperty(entry.getKey(), (String[]) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                node.setProperty(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Boolean[]) {
                node.setProperty(entry.getKey(), toValueArray((Boolean[]) entry.getValue(), session));
            } else if (entry.getValue() instanceof Calendar) {
                node.setProperty(entry.getKey(), (Calendar) entry.getValue());
            } else if (entry.getValue() instanceof Calendar[]) {
                node.setProperty(entry.getKey(), toValueArray((Calendar[]) entry.getValue(), session));
            } else if (entry.getValue() instanceof Double) {
                node.setProperty(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof Double[]) {
                node.setProperty(entry.getKey(), toValueArray((Double[]) entry.getValue(), session));
            } else if (entry.getValue() instanceof BigDecimal) {
                node.setProperty(entry.getKey(), (BigDecimal) entry.getValue());
            } else if (entry.getValue() instanceof BigDecimal[]) {
                node.setProperty(entry.getKey(), toValueArray((BigDecimal[]) entry.getValue(), session));
            } else if (entry.getValue() instanceof Long) {
                node.setProperty(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof Long[]) {
                node.setProperty(entry.getKey(), toValueArray((Long[]) entry.getValue(), session));
            } else {
                throw new IllegalArgumentException("Unable to handle value of type '"
                        + entry.getValue().getClass().getName() + "' for property '" + entry.getKey() + "'");
            }
        }

        return null;

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

}
