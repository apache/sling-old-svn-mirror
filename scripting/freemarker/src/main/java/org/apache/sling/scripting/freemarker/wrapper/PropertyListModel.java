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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.TemplateModelException;
import freemarker.template.SimpleScalar;

import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.RepositoryException;

/**
 * A wrapper for JCR multi value properties to support freemarker scripting.
 */
public class PropertyListModel implements TemplateSequenceModel {

    private Value[] values;

    public PropertyListModel(Property property) throws RepositoryException {
        this.values = property.getValues();
    }

    /**
     * Retrieves the i-th template model in this sequence.
     *
     * @return the item at the specified index, or <code>null</code> if
     *         the index is out of bounds. Note that a <code>null</code> value is
     *         interpreted by FreeMarker as "variable does not exist", and accessing
     *         a missing variables is usually considered as an error in the FreeMarker
     *         Template Language, so the usage of a bad index will not remain hidden.
     */
    public TemplateModel get(int index) throws TemplateModelException {
        try {
            return new SimpleScalar(values[index].getString());
        } catch (RepositoryException e) {
            throw new TemplateModelException(e);
        }
    }

    /**
     * @return the number of items in the list.
     */
    public int size() throws TemplateModelException {
        return values.length;
    }
}
