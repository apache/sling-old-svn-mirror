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
package org.apache.sling.hc.util;

import org.apache.sling.hc.api.Evaluator;
import org.apache.sling.hc.api.SystemAttribute;
import org.slf4j.Logger;

public class DefaultEvaluator implements Evaluator {
    @Override
    public void evaluate(SystemAttribute a, String expression, Logger logger) {
        
        boolean matches = false;
        final Object oValue = a.getValue(logger);
        final String stringValue = oValue == null ? "" : oValue.toString();
        
        if(expression == null || expression.trim().length() == 0) {
            // No expression, result will be based on a.getValue() logging only
            return;
        }
        
        final String [] parts = expression.split(" ");
        
        try {
            if(expression.startsWith(">") && parts.length == 2) {
                final int value = Integer.valueOf(stringValue).intValue();
                matches = value > Integer.valueOf(parts[1]);
                
            } else if(expression.startsWith("<") && parts.length == 2) {
                final int value = Integer.valueOf(stringValue).intValue();
                matches = value < Integer.valueOf(parts[1]);
                
            } else if(parts.length == 4 && "between".equalsIgnoreCase(parts[0]) && "and".equalsIgnoreCase(parts[2]) ) {
                final int value = Integer.valueOf(stringValue).intValue();
                final int lowerBound = Integer.valueOf(parts[1]);
                final int upperBound = Integer.valueOf(parts[3]);
                matches = value > lowerBound && value < upperBound;
                
            } else {
                matches = expression.equals(stringValue); 
            }
        } catch(NumberFormatException nfe) {
            logger.warn("Invalid numeric value [{}] while evaluating {}", oValue, expression);
        }
        
        if(!matches) {
            logger.warn("Value [{}] does not match expression [{}]", stringValue, expression);
        }
    }
}
