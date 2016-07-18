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

/** An embedded block of text */
public class RegisterNodetypes extends Operation {
    private final String cndStatements;
    
    /** Optional prefix used at the beginning of CND lines,
     *  to avoid conflicts with Sling provisioning
     *  model parser. If present at the beginning of CND lines,
     *  this string is removed.
     */
    public static final String CND_OPTIONAL_PREFIX = "<< ";
    
    public RegisterNodetypes(String cndStatements) {
        this.cndStatements = new LinePrefixCleaner().removePrefix(CND_OPTIONAL_PREFIX, cndStatements);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ":\n" + getParametersDescription();
    }
    
    @Override
    protected String getParametersDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getCndStatements());
        return sb.toString();
    }
    
    @Override
    public void accept(OperationVisitor v) {
        v.visitRegisterNodetypes(this);
    }
    
    public String getCndStatements() {
        return cndStatements;
    }
}
