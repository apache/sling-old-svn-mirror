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
package org.apache.sling.nosql.mongodb.resourceprovider.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.nosql.mongodb.resourceprovider.impl.MongoDBNoSqlAdapter;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;

/**
 * Test creation of indexes in MongoDB
 */
public class IndexCreationIT {

	private MongoDBNoSqlAdapter underTest;
	
	private MongoClient mongoClient;
	private String database;
	private String collection;
		
	@Before
	public void setUp() throws Exception {
	    String connectionString = System.getProperty("connectionString", "localhost:27017");
        database =  System.getProperty("database", "sling") + "_indextest";
        collection = System.getProperty("collection", "resources");
		mongoClient = new MongoClient(connectionString);
		underTest = new MongoDBNoSqlAdapter(mongoClient, database, collection);
		underTest.checkConnection();
		underTest.createIndexDefinitions();
	}

	@After
	public void tearDown() {
		mongoClient.dropDatabase(database);
		mongoClient.close();
	}

	@Test
	public void testIndexesPresent() {
		assertNotNull(underTest);
		
		final List<String> expectedIndexNames= ImmutableList.<String>of("_id_", "parentPath_1");
		
		List<String> actualIndexNames = new ArrayList<String>();
		for (Document d : mongoClient.getDatabase(database).getCollection(collection).listIndexes()) {
		    actualIndexNames.add(d.get("name").toString());
		}
		
		assertEquals(expectedIndexNames, actualIndexNames);
	}

}
