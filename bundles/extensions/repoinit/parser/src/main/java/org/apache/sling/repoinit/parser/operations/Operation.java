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

public abstract class Operation {
    public abstract void accept(OperationVisitor v);
    public static final String DQUOTE = "\"";
    
    protected abstract String getParametersDescription();
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + getParametersDescription();
    }
    
    public static String cleanupQuotedString(String s) {
        if(s == null) {
            return null;
        }
        if(s.startsWith(DQUOTE)) {
            s = s.substring(1);
        }
        if(s.endsWith(DQUOTE)) {
            s = s.substring(0, s.length() - 1);
        }
        s = s.trim();
        if(s.length() == 0) {
            return null;
        }
        return s;
    }
}
