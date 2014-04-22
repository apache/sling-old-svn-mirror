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
package org.apache.sling.models.spi;

public class AbstractModelAnnotationProcessor implements
	ModelAnnotationProcessor {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.sling.models.spi.ModelAnnotationProcessor#getNameAnnotationValue
     * ()
     */
    public String getNameAnnotationValue() {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.sling.models.spi.ModelAnnotationProcessor#getViaAnnotationValue
     * ()
     */
    public String getViaAnnotationValue() {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.sling.models.spi.ModelAnnotationProcessor#hasDefaultValue()
     */
    public boolean hasDefaultValue() {
	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.models.spi.ModelAnnotationProcessor#
     * getDefaultAnnotationValue()
     */
    public Object[] getDefaultAnnotationValue() {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.models.spi.ModelAnnotationProcessor#isOptional()
     */
    public Boolean isOptional() {
	return null;
    }
}
