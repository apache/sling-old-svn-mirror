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


/**
 * Sub package matcher - the topic must be in the same package or a sub package.
 */
public class SubPackagesTopicMatcher implements TopicMatcher {

    private final String packageName;

    public SubPackagesTopicMatcher(final String name) {
        // remove last char and maybe a trailing slash
        int lastPos = name.length() - 1;
        if ( lastPos > 0 && name.charAt(lastPos - 1) == '/' ) {
            this.packageName = name.substring(0, lastPos);
        } else {
            this.packageName = name.substring(0, lastPos) + '/';
        }
    }

    /**
     * @see org.apache.sling.event.impl.support.TopicMatcher#match(java.lang.String)
     */
    @Override
    public String match(final String topic) {
        final int pos = topic.lastIndexOf('/');
        return pos > -1 && topic.substring(0, pos + 1).startsWith(this.packageName) ? topic.substring(this.packageName.length()) : null;
    }

    @Override
    public String toString() {
        return "SubPackageMatcher [packageName=" + packageName + "]";
    }
}