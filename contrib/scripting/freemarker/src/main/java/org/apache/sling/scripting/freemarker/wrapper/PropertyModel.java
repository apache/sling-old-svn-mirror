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
package org.apache.sling.scripting.freemarker.wrapper;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * A wrapper for JCR properties to support freemarker scripting.
 */
public class PropertyModel implements TemplateScalarModel {

    private Property property;

    public PropertyModel(Property property) {
        this.property = property;
    }

    /**
     * Returns the string representation of this model. In general, avoid
     * returning null. In compatibility mode the engine will convert
     * null into empty string, however in normal mode it will
     * throw an exception if you return null from this method.
     */
    public String getAsString() throws TemplateModelException {
        try {
            return property.getString();
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }
}
