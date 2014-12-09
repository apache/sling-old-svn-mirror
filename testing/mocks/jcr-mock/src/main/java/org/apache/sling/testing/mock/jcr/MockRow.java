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

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;

/**
 * Mock implementation of {@link Row}.
 */
class MockRow implements Row {
    
    private final List<String> columnNames;
    private final Node node;
    
    public MockRow(List<String> columnNames, Node node) {
        this.columnNames = columnNames;
        this.node = node;
    }

    @Override
    public Value[] getValues() throws RepositoryException {
        Value[] values = new Value[columnNames.size()];
        for (int i = 0; i < values.length; i++) {
            try {
                values[i] = getValue(columnNames.get(i));
            }
            catch (PathNotFoundException ex) {
                values[i] = null;
            }
        }
        return values;
    }

    @Override
    public Value getValue(String columnName) throws ItemNotFoundException, RepositoryException {
        return node.getProperty(columnName).getValue();
    }

    @Override
    public Node getNode() throws RepositoryException {
        return node;
    }

    @Override
    public Node getNode(String selectorName) throws RepositoryException {
        return null;
    }

    @Override
    public String getPath() throws RepositoryException {
        return node.getPath();
    }

    @Override
    public String getPath(String selectorName) throws RepositoryException {
        return null;
    }

    @Override
    public double getScore() throws RepositoryException {
        return 0;
    }

    @Override
    public double getScore(String selectorName) throws RepositoryException {
        return 0;
    }

}
