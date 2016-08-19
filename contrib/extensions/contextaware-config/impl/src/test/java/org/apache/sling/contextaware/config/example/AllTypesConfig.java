/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.contextaware.config.example;

import static org.apache.sling.contextaware.config.example.AllTypesDefaults.BOOL_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.BOOL_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.DOUBLE_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.DOUBLE_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.INT_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.INT_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.LONG_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.LONG_DEFAULT_2;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.STRING_DEFAULT;
import static org.apache.sling.contextaware.config.example.AllTypesDefaults.STRING_DEFAULT_2;

public @interface AllTypesConfig {

    String stringParam();
    
    String stringParamWithDefault() default STRING_DEFAULT;

    int intParam();
    
    int intParamWithDefault() default INT_DEFAULT;
    
    long longParam();
    
    long longParamWithDefault() default LONG_DEFAULT;
    
    double doubleParam();
    
    double doubleParamWithDefault() default DOUBLE_DEFAULT;
    
    boolean boolParam();
    
    boolean boolParamWithDefault() default BOOL_DEFAULT;
    
    String[] stringArrayParam();
    
    String[] stringArrayParamWithDefault() default { STRING_DEFAULT, STRING_DEFAULT_2 };

    int[] intArrayParam();
    
    int[] intArrayParamWithDefault() default { INT_DEFAULT, INT_DEFAULT_2 };
    
    long[] longArrayParam();
    
    long[] longArrayParamWithDefault() default { LONG_DEFAULT, LONG_DEFAULT_2 };
    
    double[] doubleArrayParam();
    
    double[] doubleArrayParamWithDefault() default { DOUBLE_DEFAULT, DOUBLE_DEFAULT_2 };
    
    boolean[] boolArrayParam();
    
    boolean[] boolArrayParamWithDefault() default { BOOL_DEFAULT, BOOL_DEFAULT_2 };
    
}
