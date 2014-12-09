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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * Mock implementation of {@link QueryManager}.
 */
class MockQueryManager implements QueryManager {
    
    private List<MockQueryResultHandler> resultHandlers = new ArrayList<MockQueryResultHandler>();
    
    @SuppressWarnings("deprecation")
    private static final List<String> SUPPORTED_QUERY_LANGUAGES = ImmutableList.of(
      Query.JCR_SQL2,
      Query.JCR_JQOM,
      Query.XPATH,
      Query.SQL
    );
    
    @Override
    public Query createQuery(String statement, String language) throws InvalidQueryException {
        if (!SUPPORTED_QUERY_LANGUAGES.contains(StringUtils.defaultString(language))) {
            throw new InvalidQueryException("Unsupported query language: " + language);
        }
        return new MockQuery(this, statement, language);
    }

    @Override
    public String[] getSupportedQueryLanguages() {
        return SUPPORTED_QUERY_LANGUAGES.toArray(new String[SUPPORTED_QUERY_LANGUAGES.size()]);
    }
    
    void addResultHandler(MockQueryResultHandler resultHandler) {
        this.resultHandlers.add(resultHandler);
    }
    
    QueryResult executeQuery(MockQuery query) {
        for (MockQueryResultHandler resultHandler : resultHandlers) {
            MockQueryResult result = resultHandler.executeQuery(query);
            if (result != null) {
                return result;
            }
        }
        // fallback to empty result
        return new MockQueryResult(ImmutableList.<Node>of());
    }

    // --- unsupported operations ---
    
    @Override
    public QueryObjectModelFactory getQOMFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query getQuery(Node node) {
        throw new UnsupportedOperationException();
    }

}
