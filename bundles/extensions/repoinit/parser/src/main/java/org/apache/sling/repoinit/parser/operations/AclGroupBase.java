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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Base class for operations that group AclLines */
 abstract class AclGroupBase extends Operation {
    /**
     * Supported ACL options
     */
    public static final String ACL_OPTION_MERGE = "merge";
    public static final String ACL_OPTION_MERGE_PRESERVE = "mergePreserve";

    private final List<AclLine> lines;
    private final List<String> aclOptions;
    
    protected AclGroupBase(List<AclLine> lines) {
        this(lines,new ArrayList<String>());
    }
    protected AclGroupBase(List<AclLine> lines, List<String> aclOptions) {
        this.lines = Collections.unmodifiableList(lines);
        this.aclOptions = Collections.unmodifiableList(aclOptions);
    }
    
    protected String getParametersDescription() {
        final StringBuilder sb = new StringBuilder();
        for(AclLine line : lines) {
            sb.append("\n  ").append(line.toString());
        }
        return sb.toString(); 
    }
    
    public Collection<AclLine> getLines() {
        return lines;
    }

    public List<String> getOptions() {
        return aclOptions;
    }
}
