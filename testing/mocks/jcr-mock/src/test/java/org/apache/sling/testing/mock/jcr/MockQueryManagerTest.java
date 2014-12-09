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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class MockQueryManagerTest {

    private Session session;
    private QueryManager queryManager;
    private List<Node> sampleNodes;
    
    @Before
    public void setUp() throws RepositoryException {
        session = MockJcr.newSession();
        queryManager = session.getWorkspace().getQueryManager();
        
        Node rootNode = session.getRootNode();
        
        sampleNodes = ImmutableList.of(
            rootNode.addNode("node1"),
            rootNode.addNode("node2"),
            rootNode.addNode("node3")
        );
        
        for (int i=0; i<sampleNodes.size(); i++) { 
            Node node = sampleNodes.get(i);
            node.setProperty("stringProp", "value" + (i + 1));
            node.setProperty("intProp", i + 1);
        }
        sampleNodes.get(0).setProperty("optionalStringProp", "optValue1");
    }
    
    @Test
    public void testNoQueryResults() throws RepositoryException {
        Query query = queryManager.createQuery("dummy", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertFalse(result.getNodes().hasNext());
    }
    
    @Test(expected=InvalidQueryException.class)
    public void testInvalidQueryLanguage() throws RepositoryException {
        queryManager.createQuery("dummy", "wurst");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResults_AllQuerys() throws RepositoryException {
        MockJcr.setQueryResult(queryManager, sampleNodes);
        
        Query query = queryManager.createQuery("query1", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));

        query = queryManager.createQuery("query2", Query.JCR_SQL2);
        result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResults_SpecificQuery() throws RepositoryException {
        MockJcr.setQueryResult(queryManager, "query1", sampleNodes);
        
        Query query = queryManager.createQuery("query1", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));

        query = queryManager.createQuery("query2", Query.JCR_SQL2);
        result = query.execute();
        assertFalse(result.getNodes().hasNext());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResults_ResultHandler() throws RepositoryException {
        MockJcr.addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                if (StringUtils.equals(query.getStatement(), "query1")) {
                    return new MockQueryResult(sampleNodes);
                }
                return null;
            }
        });
        
        Query query = queryManager.createQuery("query1", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));

        query = queryManager.createQuery("query2", Query.JCR_SQL2);
        result = query.execute();
        assertFalse(result.getNodes().hasNext());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResults_MultipleResultHandlers() throws RepositoryException {
        final List<Node> sampleNodes2 = ImmutableList.of(sampleNodes.get(0));
        MockJcr.addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                if (StringUtils.equals(query.getStatement(), "query2")) {
                    return new MockQueryResult(sampleNodes2);
                }
                return null;
            }
        });
        
        MockJcr.addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                if (StringUtils.equals(query.getStatement(), "query1")) {
                    return new MockQueryResult(sampleNodes);
                }
                return null;
            }
        });
        
        Query query = queryManager.createQuery("query1", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));

        query = queryManager.createQuery("query2", Query.JCR_SQL2);
        result = query.execute();
        assertEquals(sampleNodes2, ImmutableList.copyOf(result.getNodes()));

        query = queryManager.createQuery("query3", Query.JCR_SQL2);
        result = query.execute();
        assertFalse(result.getNodes().hasNext());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryResults_ResultHandler_Rows() throws RepositoryException {
        final List<String> columnNames = ImmutableList.of(
            "stringProp",
            "intProp",
            "optionalStringProp"
        );
         
        MockJcr.addQueryResultHandler(queryManager, new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                return new MockQueryResult(sampleNodes, columnNames);
            }
        });
        
        Query query = queryManager.createQuery("query1", Query.JCR_SQL2);
        QueryResult result = query.execute();
        assertEquals(sampleNodes, ImmutableList.copyOf(result.getNodes()));
        
        assertEquals(columnNames, ImmutableList.copyOf(result.getColumnNames()));
        
        List<Row> rows = ImmutableList.copyOf(result.getRows());
        assertEquals("value1", rows.get(0).getValue("stringProp").getString());
        assertEquals(1L, rows.get(0).getValue("intProp").getLong());
        assertEquals("optValue1", rows.get(0).getValue("optionalStringProp").getString());

        assertEquals("value2", rows.get(1).getValues()[0].getString());
        assertEquals(2L, rows.get(1).getValues()[1].getLong());
        assertNull(rows.get(1).getValues()[2]);

        assertEquals("value3", rows.get(2).getValues()[0].getString());
        assertEquals(3L, rows.get(2).getValues()[1].getLong());
        assertNull(rows.get(2).getValues()[2]);
    }
    
}
