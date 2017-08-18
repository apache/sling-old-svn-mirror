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

public class DisableServiceUser extends ServiceUserOperation {
    private final String reason;
    
    public DisableServiceUser(String username, String reason) {
        super(username);
        this.reason = cleanupQuotedString(reason);
        if(this.reason == null || this.reason.length() == 0) {
            throw new IllegalArgumentException("A non-empty reason is required");
        }
    }

    @Override
    public String getParametersDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.getParametersDescription());
        if(reason!=null) {
            sb.append(" : ");
            sb.append(reason);
        }
        return sb.toString();
    }
    
    @Override
    public void accept(OperationVisitor v) {
        v.visitDisableServiceUser(this);
    }
}
