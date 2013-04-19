/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.muppet.util;

import org.apache.sling.muppet.api.Evaluator;
import org.apache.sling.muppet.api.EvaluationResult;
import org.apache.sling.muppet.api.SystemAttribute;
import org.apache.sling.muppet.api.EvaluationResult.Status;

public class DefaultEvaluator implements Evaluator {
    @Override
    public Status evaluate(SystemAttribute a, String expression) {
        final String [] parts = expression.split(" ");
        boolean matches = false;
        
        if(expression.startsWith(">") && parts.length == 2) {
            matches = Integer.valueOf(a.getValue().toString()).intValue() > Integer.valueOf(parts[1]);
            
        } else if(expression.startsWith("<") && parts.length == 2) {
            matches = Integer.valueOf(a.getValue().toString()).intValue() < Integer.valueOf(parts[1]);
            
        } else if(parts.length == 4 && "between".equalsIgnoreCase(parts[0]) && "and".equalsIgnoreCase(parts[2]) ) {
            final int aValue = Integer.valueOf(a.getValue().toString()).intValue();
            final int lowerBound = Integer.valueOf(parts[1]);
            final int upperBound = Integer.valueOf(parts[3]);
            matches = aValue > lowerBound && aValue < upperBound;
            
        } else {
            matches = expression.equals(a.getValue().toString()); 
        }
        
        return matches ? EvaluationResult.Status.OK : EvaluationResult.Status.ERROR;
    }
}
