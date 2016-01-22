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

package org.apache.sling.acldef.parser.operations;

import java.util.Collections;
import java.util.List;

/** Set ACL statement that groups a set of AclLines
 *  that all refer to the same set of paths.
 */
public class SetAclPaths extends AclGroupBase {
    
    private final List<String> paths;
    
    public SetAclPaths(List<String> paths, List<AclLine> lines) {
        super(lines);
        this.paths = Collections.unmodifiableList(paths);
    }
    
    protected String getParametersDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append(paths);
        sb.append(super.getParametersDescription());
        return sb.toString(); 
    }
    
    public List<String> getPaths() {
        return paths;
    }

    @Override
    public void accept(OperationVisitor v) {
        v.visitSetAclPaths(this);
    }
}