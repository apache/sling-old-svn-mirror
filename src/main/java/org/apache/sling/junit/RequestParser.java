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
package org.apache.sling.junit;


/** Parse information from a request, to define which
 *  tests to run and which renderer to select.
 *  
 *  We do not use the Sling API to to that, in order to 
 *  keep the junit core module reusable in other OSGi 
 *  environments.
 */
public class RequestParser implements TestSelector {
    private final String testNameSelector;
    private final String selectedMethodName;
    private final String extension;
    private static final String EMPTY_STRING = "";

    /** Parse subpath, which is in the form 
     *  TEST_SELECTOR/TEST_METHOD.EXTENSION
     *  or
     *  TEST_SELECTOR.EXTENSION
     */
    public RequestParser(String subpath) {
        
        if (subpath == null) {
            testNameSelector = EMPTY_STRING;
            selectedMethodName = EMPTY_STRING;
            extension = EMPTY_STRING;
        } else {
            if (subpath.startsWith("/")) {
                subpath = subpath.substring(1);
            }
            
            // Split at last dot to find extension
            String beforeExtension = null;
            {
                final int pos = subpath.lastIndexOf('.');
                if (pos >= 0) {
                    beforeExtension = subpath.substring(0, pos);
                    extension = subpath.substring(pos+1);
                } else {
                    beforeExtension = subpath;
                    extension = EMPTY_STRING;
                }
            }
            
            // And split at last / between test selector and test method name
            {
                final int pos = beforeExtension.lastIndexOf('/');
                if(pos >= 0) {
                    testNameSelector = beforeExtension.substring(0, pos);
                    selectedMethodName = beforeExtension.substring(pos+1);
                } else {
                    testNameSelector = beforeExtension;
                    selectedMethodName = EMPTY_STRING;
                }
            }
        }
    }

    public String toString() {
        return getClass().getSimpleName() 
                + ", testSelector [" + testNameSelector + "]"
                + ", methodName [" + selectedMethodName + "]"
                + ", extension [" + extension + "]"
                ;
    }

    public String getTestSelectorString() {
        return testNameSelector;
    }

    public String getExtension() {
        return extension;
    }
    
    public String getMethodName() {
        return selectedMethodName;
    }
    
    /** @inheritDoc */
    public boolean acceptTestName(String testName) {
        if(testNameSelector.length() == 0) {
            return true;
        } else {
            return testName.startsWith(testNameSelector);
        }
    }

    /** @inheritDoc */
    public String getSelectedTestMethodName() {
        return selectedMethodName;
    }
}