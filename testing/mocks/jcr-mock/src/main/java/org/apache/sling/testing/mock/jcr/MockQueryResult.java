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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;

import aQute.bnd.annotation.ProviderType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Mock implementation of {@link QueryResult}.
 * Allows to manually set the expected result, optional with column names 
 * (which are interpreted as property names of the nodes of the result list).
 */
@ProviderType
public final class MockQueryResult implements QueryResult {
    
    private final List<Node> nodes;
    private final List<String> columnNames;
    
    public MockQueryResult(List<Node> nodes) {
        this(nodes, ImmutableList.<String>of());
    }

    public MockQueryResult(List<Node> nodes, List<String> columnNames) {
        this.columnNames = columnNames;
        this.nodes = nodes;
    }

    @Override
    public String[] getColumnNames() throws RepositoryException {
        return columnNames.toArray(new String[columnNames.size()]);
    }

    @Override
    public RowIterator getRows() throws RepositoryException {
        return new RowIteratorAdapter(Lists.transform(nodes, new Function<Node, Row>() {
            @Override
            public Row apply(Node node) {
                return new MockRow(columnNames, node);
            }
        }));
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIteratorAdapter(nodes);
    }

    @Override
    public String[] getSelectorNames() throws RepositoryException {
        return new String[0];
    }

}
