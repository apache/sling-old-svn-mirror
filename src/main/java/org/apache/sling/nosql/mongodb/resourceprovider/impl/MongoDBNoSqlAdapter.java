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
package org.apache.sling.nosql.mongodb.resourceprovider.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.sling.nosql.generic.adapter.AbstractNoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.MultiValueMode;
import org.apache.sling.nosql.generic.adapter.NoSqlData;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * {@link org.apache.sling.nosql.generic.adapter.NoSqlAdapter} implementation for MongoDB.
 */
public final class MongoDBNoSqlAdapter extends AbstractNoSqlAdapter {
    
    private static final String ID_PROPERTY = "_id";
    private static final String DATA_PROPERTY = "_data";
    
    private final MongoCollection<Document> collection;
    
    /**
     * @param mongoClient MongoDB client
     * @param database MongoDB database
     * @param collection MongoDB collection
     */
    public MongoDBNoSqlAdapter(MongoClient mongoClient, String database, String collection) {
        MongoDatabase db = mongoClient.getDatabase(database);
        this.collection = db.getCollection(collection);
    }

    @Override
    public NoSqlData get(String path) {
        Document wrapper = collection.find(Filters.eq(ID_PROPERTY, path)).first();
        if (wrapper == null) {
            return null;
        }
        else {
            return new NoSqlData(path, wrapper.get(DATA_PROPERTY, Document.class), MultiValueMode.LISTS);
        }
    }

    @Override
    public Iterator<NoSqlData> getChildren(String parentPath) {
        List<NoSqlData> children = new ArrayList<>();
        Pattern directChildren = Pattern.compile("^" + Pattern.quote(parentPath) + "/[^/]+$");
        FindIterable<Document> result = collection.find(Filters.regex(ID_PROPERTY, directChildren));
        try (MongoCursor<Document> wrappers = result.iterator()) {
            while (wrappers.hasNext()) {
                Document wrapper = wrappers.next();
                String path = wrapper.get(ID_PROPERTY, String.class);
                Document data = wrapper.get(DATA_PROPERTY, Document.class);
                children.add(new NoSqlData(path, data, MultiValueMode.LISTS));
            }
        }
        return children.iterator();
    }

    @Override
    public boolean store(NoSqlData data) {
        Document wrapper = new Document();
        wrapper.put(ID_PROPERTY, data.getPath());
        wrapper.put(DATA_PROPERTY, new Document(data.getProperties(MultiValueMode.LISTS)));
        
        UpdateResult result = collection.replaceOne(Filters.eq(ID_PROPERTY, data.getPath()), wrapper, new UpdateOptions().upsert(true));
        
        // return true if a new entry was inserted, false if an existing was replaced
        return (result.getMatchedCount() == 0);
    }

    @Override
    public boolean deleteRecursive(String path) {        
        Pattern descendantsAndSelf = Pattern.compile("^" + Pattern.quote(path) + "(/.+)?$");
        DeleteResult result = collection.deleteMany(Filters.regex(ID_PROPERTY, descendantsAndSelf));
        
        // return true if any document was deleted
        return result.getDeletedCount() > 0;
    }

}
