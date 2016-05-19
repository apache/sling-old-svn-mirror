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
import java.util.Collections;
import java.util.List;

public class CreatePath extends Operation {
    private List<PathSegmentDefinition> pathDef;
    private final String defaultPrimaryType;
    
    public CreatePath(String defaultPrimaryType) {
        this.pathDef = new ArrayList<PathSegmentDefinition>();
        this.defaultPrimaryType = defaultPrimaryType;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + pathDef;
    }
    
    @Override
    protected String getParametersDescription() {
        return pathDef.toString();
    }

    @Override
    public void accept(OperationVisitor v) {
        v.visitCreatePath(this);
    }
    
    public void addSegment(String path, String primaryType) {
        // We might get a path like /var/discovery, in which case
        // the specified primary type applies to the last
        // segment only
        final String [] segments = path.split("/");
        for(int i=0; i < segments.length; i++) {
            if(segments[i].length() == 0) {
                continue;
            }
            String pt = defaultPrimaryType;
            if(i == segments.length -1 && primaryType != null) {
                pt = primaryType;
            }
            pathDef.add(new PathSegmentDefinition(segments[i], pt));
        }
    }
    
    public List<PathSegmentDefinition> getDefinitions() {
        return Collections.unmodifiableList(pathDef);
    }
}
