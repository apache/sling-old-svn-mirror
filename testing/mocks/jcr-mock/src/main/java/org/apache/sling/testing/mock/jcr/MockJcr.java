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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang3.StringUtils;

import aQute.bnd.annotation.ConsumerType;

/**
 * Factory for mock JCR objects.
 */
@ConsumerType
public final class MockJcr {

    /**
     * Default workspace name
     */
    public static final String DEFAULT_WORKSPACE = "mockedWorkspace";

    /**
     * Default user id
     */
    public static final String DEFAULT_USER_ID = "admin";

    private MockJcr() {
        // static methods only
    }

    /**
     * Create a new mocked in-memory JCR repository. Beware: each session has
     * its own data store.
     * @return JCR repository
     */
    public static Repository newRepository() {
        return new MockRepository();
    }

    /**
     * Create a new mocked in-memory JCR session. It contains only the root
     * node. All data of the session is thrown away if it gets garbage
     * collected.
     * @return JCR session
     */
    public static Session newSession() {
        return newSession(null, null);
    }

    /**
     * Create a new mocked in-memory JCR session. It contains only the root
     * node. All data of the session is thrown away if it gets garbage
     * collected.
     * @param userId User id for the mock environment.
     * @param workspaceName Workspace name for the mock environment.
     * @return JCR session
     */
    public static Session newSession(String userId, String workspaceName) {
        try {
            return newRepository().login(
                    new SimpleCredentials(StringUtils.defaultString(userId, DEFAULT_USER_ID), new char[0]),
                    StringUtils.defaultString(workspaceName, DEFAULT_WORKSPACE)
            );
        } catch (RepositoryException ex) {
            throw new RuntimeException("Creating mocked JCR session failed.", ex);
        }
    }
    
    /**
     * Sets the expected result list for all queries executed with the given query manager.
     * @param session JCR session
     * @param resultList Result list
     */
    public static void setQueryResult(final Session session, final List<Node> resultList) {
        setQueryResult(getQueryManager(session), resultList);
    }
    
    /**
     * Sets the expected result list for all queries executed with the given query manager.
     * @param queryManager Mocked query manager
     * @param resultList Result list
     */
    public static void setQueryResult(final QueryManager queryManager, final List<Node> resultList) {
        addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                return new MockQueryResult(resultList);
            }
        });
    }

    /**
     * Sets the expected result list for all queries with the given statement executed with the given query manager.
     * @param session JCR session
     * @param statement Query statement
     * @param language Query language
     * @param resultList Result list
     */
    public static void setQueryResult(final Session session, final String statement, final String language, final List<Node> resultList) {
        setQueryResult(getQueryManager(session), statement, language, resultList);
    }
    
    /**
     * Sets the expected result list for all queries with the given statement executed with the given query manager.
     * @param queryManager Mocked query manager
     * @param statement Query statement
     * @param language Query language
     * @param resultList Result list
     */
    public static void setQueryResult(final QueryManager queryManager, final String statement, final String language, final List<Node> resultList) {
        addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                if (StringUtils.equals(query.getStatement(), statement)
                        && StringUtils.equals(query.getLanguage(), language)) {
                    return new MockQueryResult(resultList);
                }
                else {
                    return null;
                }
            }
        });
    }

    /**
     * Adds a query result handler for the given query manager which may return query results for certain queries that are executed.
     * @param session JCR session
     * @param resultHandler Mock query result handler
     */
    public static void addQueryResultHandler(final Session session, final MockQueryResultHandler resultHandler) {
        addQueryResultHandler(getQueryManager(session), resultHandler);
    }
    
    /**
     * Adds a query result handler for the given query manager which may return query results for certain queries that are executed.
     * @param queryManager Mocked query manager
     * @param resultHandler Mock query result handler
     */
    public static void addQueryResultHandler(final QueryManager queryManager, final MockQueryResultHandler resultHandler) {
        ((MockQueryManager)queryManager).addResultHandler(resultHandler);
    }
    
    private static QueryManager getQueryManager(Session session) {
        try {
            return session.getWorkspace().getQueryManager();
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("Unable to access query manager.", ex);
        }
    }

}
