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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import aQute.bnd.annotation.ConsumerType;

import com.google.common.collect.ImmutableMap;

/**
 * Mock implementation of {@link Query}.
 */
@ConsumerType
public final class MockQuery implements Query {
    
    private final MockQueryManager queryManager;
    private final String statement;
    private final String language;
    
    private long limit;
    private long offset;
    private Map<String,Value> variables = new HashMap<String, Value>();
    
    MockQuery(MockQueryManager queryManager, String statement, String language) {
        this.queryManager = queryManager;
        this.statement = statement;
        this.language = language;
    }

    @Override
    public QueryResult execute() {
        return queryManager.executeQuery(this);
    }

    @Override
    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    @Override
    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String getStatement() {
        return this.statement;
    }

    @Override
    public String getLanguage() {
        return this.language;
    }

    @Override
    public void bindValue(String varName, Value value) {
        variables.put(varName,  value);
    }

    @Override
    public String[] getBindVariableNames() {
        Set<String> variableNames = variables.keySet();
        return variableNames.toArray(new String[variableNames.size()]);
    }
    
    public Map<String, Value> getBindVariables() {
        return ImmutableMap.copyOf(variables);
    }
    

    // --- unsupported operations ---

    @Override
    public String getStoredQueryPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node storeAsNode(String absPath) {
        throw new UnsupportedOperationException();
    }

}
