package org.apache.sling.nosql.mongodb.resourceprovider.integration;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.sling.nosql.mongodb.resourceprovider.impl.MongoDBNoSqlAdapter;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoClient;

public class MongoDBNoSqlAdapterIT {

	MongoDBNoSqlAdapter obj;
	
	MongoClient mongoClient;
	String database;
	String collection;
	
	
	@Before
	public void setUp() throws Exception {
		database = "slingdb";
		collection = "indexedcollection";
		//default connection requiring MongoDB instance to be running
		mongoClient = new MongoClient();
		obj = new MongoDBNoSqlAdapter(mongoClient, database, collection);
	}

	@After
	public void tearDown() throws Exception {
		mongoClient.dropDatabase(database);
		mongoClient.close();
	}

	@Test
	public void testMongoDBNoSqlAdapter() {
		assertNotNull(obj);
		//expecting at least 3 indexes (_id, _path, _parentPath)
		int expected = 3;
		int actual=0;
		
		final String[] expectedIndexesNames=  {"_id_", "_path_1", "_parentPath_1"};
		
		for( Document d : mongoClient.getDatabase(database).getCollection(collection).listIndexes()){
			assert Arrays.asList(expectedIndexesNames).contains( d.get("name") );
			actual++;
		}
		assert expected == actual;
	}

}
