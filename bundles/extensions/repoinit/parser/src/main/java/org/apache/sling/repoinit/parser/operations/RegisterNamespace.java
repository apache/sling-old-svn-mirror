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

public class RegisterNamespace extends Operation {
    private final String prefix;
    private final String uri;
    
    public RegisterNamespace(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + getParametersDescription();
    }
    
    @Override
    protected String getParametersDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(").append(prefix == null ? "" : prefix).append(") ");
        sb.append(uri);
        return sb.toString();
    }

    @Override
    public void accept(OperationVisitor v) {
        v.visitRegisterNamespace(this);
    }
}
