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
package org.apache.sling.event.impl.support;

public abstract class TopicMatcherHelper {

    public static final TopicMatcher[] MATCH_ALL = new TopicMatcher[] {
        new TopicMatcher() {

           @Override
           public String match(String topic) {
               return topic;
           }
       }
    };

    /**
     * Create matchers based on the topic parameters.
     * If the topic parameters do not contain any definition
     * <code>null</code> is returned.
     */
    public static TopicMatcher[] buildMatchers(final String[] topicsParam) {
        final TopicMatcher[] matchers;
        if ( topicsParam == null
                || topicsParam.length == 0
                || (topicsParam.length == 1 && (topicsParam[0] == null || topicsParam[0].length() == 0))) {
               matchers = null;
       } else {
           final TopicMatcher[] newMatchers = new TopicMatcher[topicsParam.length];
           for(int i=0; i < topicsParam.length; i++) {
               String value = topicsParam[i];
               if ( value != null ) {
                   value = value.trim();
               }
               if ( value != null && value.length() > 0 ) {
                   if ( value.equals("*") ) {
                       return MATCH_ALL;
                   }
                   if ( value.endsWith(".") ) {
                       newMatchers[i] = new PackageTopicMatcher(value);
                   } else if ( value.endsWith("*") ) {
                       newMatchers[i] = new SubPackagesTopicMatcher(value);
                   } else {
                       newMatchers[i] = new ExactTopicMatcher(value);
                   }
               }
           }
           matchers = newMatchers;
       }
        return matchers;
    }

}
