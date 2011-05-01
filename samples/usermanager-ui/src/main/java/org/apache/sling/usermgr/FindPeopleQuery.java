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
package org.apache.sling.usermgr;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.QueryBuilder.Direction;

/**
 * Simple Query to find Users/Groups
 */
public class FindPeopleQuery implements Query {

    private int searchType;
    private String searchQuery;
    private boolean isPrefixQuery;
    private long offset;
    private long maxResults;
    
    /**
     * Constructor. The parameters specify what to query for. 
     * 
     * @param searchType the type to search for.  
     *         One of: {@link UserManager#SEARCH_TYPE_USER}, {@link UserManager#SEARCH_TYPE_GROUP}
     *           or {@link UserManager#SEARCH_TYPE_AUTHORIZABLE}
     * @param searchQuery the term to search for
     * @param isPrefixQuery true if this is a simple prefix query
     * @param offset the offset to start the results from
     * @param maxResults the maximum number of rows to retrieve
     */
    public FindPeopleQuery(int searchType, String searchQuery,
            boolean isPrefixQuery, long offset, long maxResults) {
        this.searchType = searchType;
        this.searchQuery = searchQuery;
        this.isPrefixQuery = isPrefixQuery;
        this.offset = offset;
        this.maxResults = maxResults;
    }

    /* (non-Javadoc)
     * @see org.apache.jackrabbit.api.security.user.Query#build(org.apache.jackrabbit.api.security.user.QueryBuilder)
     */
    public <T> void build(QueryBuilder<T> builder) {
        if (UserManager.SEARCH_TYPE_USER == searchType) {
            builder.setSelector(User.class);
        } else if (UserManager.SEARCH_TYPE_GROUP == searchType) {
            builder.setSelector(Group.class);
        }

        //JCR-952: case-insensive sort needs jackrabbit 2.3+
        // builder.setSortOrder("fn:lower-case(@rep:principalName)", Direction.ASCENDING);
        builder.setSortOrder("@rep:principalName", Direction.ASCENDING);
        builder.setLimit(offset, maxResults + 1); //max + 1 so we can tell if there are more on the next page
        
        String encodedTerm = encodeForLikeClause(searchQuery);
        if (isPrefixQuery) {
            String lcEncodedTerm = encodedTerm.toLowerCase();
            //check for a case insensitive match in principalName
            builder.setCondition(builder.or(builder.nameMatches(encodedTerm + "%"),
                                            builder.nameMatches(lcEncodedTerm + "%"))
                                );
        } else {
            //check for a match in the name or displayName
            builder.setCondition(builder.or(builder.nameMatches("%" + encodedTerm + "%"),
                                            builder.like("@displayName", "%" + encodedTerm + "%")));
        }
    }

    /**
     * Encodes the special characters of a term to provide a valid like clause
     * @param rawValue the value to encode
     * @return encoded value safe to use in a query like clause
     */
    static String encodeForLikeClause(String rawValue) {
        if (rawValue == null || rawValue.length() == 0) {
            return rawValue;
        }
        
        char wrappingQuoteChar = '\'';
        // encode
        StringBuffer encoded = new StringBuffer();
        for (int i = 0; i < rawValue.length(); i++) {
               char c = rawValue.charAt(i);
               if (c == '%' || c == '_' || c == '\\') {
                   encoded.append('\\'); //escape the special character
               } else if (c == wrappingQuoteChar) {
                   encoded.append(c); //double up the character to match the literal quote character
               }
               if (c == '*') {
                   //change the '*' wildcard to '%'
                   encoded.append('%');
               } else if (c == '?') {
                   //change the '?' wildcard to '_'
                   encoded.append('_');
               } else {
                   encoded.append(c);
               }
        }
        return encoded.toString();
    }
}
