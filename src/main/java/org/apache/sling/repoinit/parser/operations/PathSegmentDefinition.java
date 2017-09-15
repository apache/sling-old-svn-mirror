/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.repoinit.parser.operations;

import java.util.List;

/** Defines a segment of a path to be created,
 *  with its name and an optional primary type and optional mixins
 */
public class PathSegmentDefinition {
    private final String segment;
    private final String primaryType;
    private final List<String> mixins;
    
    public PathSegmentDefinition(String segment, String primaryType) {
        this(segment, primaryType, null);
    }

    public PathSegmentDefinition(String segment, String primaryType, List<String> mixins) {
        this.segment = segment;
        this.primaryType = primaryType;
        this.mixins = mixins;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(segment);
        boolean hasPrimaryType = primaryType != null;
        boolean hasMixin = mixins != null && ! mixins.isEmpty();
        if (hasPrimaryType || hasMixin) {
            sb.append("(");
            if (hasPrimaryType) {
                sb.append(primaryType);
            }
            if (hasPrimaryType && hasMixin) {
                sb.append(" ");
            }
            if (hasMixin) {
                sb.append("mixin ");
                sb.append(mixins.toString());
            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    public String getSegment() {
        return segment;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public List<String> getMixins() {
        return mixins;
    }
}
