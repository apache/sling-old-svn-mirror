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
package org.apache.sling.maven.jcrocm;

import org.apache.maven.plugin.logging.Log;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * The <code>BeanDescriptor</code> class provides support for the
 * bean-descriptor element of a class mapping, which has the following attribute
 * list definition:
 * 
 * <pre>
 *   &lt;!ATTLIST bean-descriptor
 *       fieldName CDATA #REQUIRED
 *       jcrName CDATA #IMPLIED 
 *       proxy (true | false) &quot;false&quot; 
 *       autoRetrieve (true|false) &quot;true&quot;
 *       autoUpdate (true|false) &quot;true&quot;
 *       autoInsert (true|false) &quot;true&quot;  
 *       converter CDATA #IMPLIED
 *       jcrType CDATA #IMPLIED
 *       jcrAutoCreated (true | false) &quot;false&quot;   
 *       jcrMandatory (true | false) &quot;false&quot;
 *       jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) &quot;COPY&quot;
 *       jcrProtected (true | false) &quot;false&quot;
 *       jcrSameNameSiblings (true | false) &quot;false&quot;         
 *   &gt;
 * </pre>
 */
public class BeanDescriptor extends AbstractNodeDescriptor {

    public static final String TAG_BEAN_DESCRIPTOR = "ocm.bean";

    public static final String CONVERTER = "converter";

    public static final String ELEMENT_BEAN_DESCRIPTOR = "bean-descriptor";

    private String converter;

    static BeanDescriptor fromField(Log log, JavaField field) {

        DocletTag tag = field.getTagByName(TAG_BEAN_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        return new BeanDescriptor(log, tag, field.getName());
    }

    static BeanDescriptor fromMethod(Log log, JavaMethod method) {

        DocletTag tag = method.getTagByName(TAG_BEAN_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        // field name is the method name, unless overwritten with the fieldName
        // tag
        String fieldName = tag.getNamedParameter(FIELD_NAME);
        if (fieldName == null) {
            fieldName = getFieldFromMethod(method);
        }

        return new BeanDescriptor(log, tag, fieldName);
    }

    /**
     * @param log
     * @param tag
     */
    private BeanDescriptor(Log log, DocletTag tag, String fieldName) {
        super(log, tag, fieldName);

        converter = tag.getNamedParameter(CONVERTER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#generate(org.apache.sling.maven.jcrocm.XMLWriter)
     */
    void generate(XMLWriter xmlWriter) {
        xmlWriter.printElementStart(ELEMENT_BEAN_DESCRIPTOR, true);
        super.generate(xmlWriter);

        xmlWriter.printAttribute(CONVERTER, converter);

        xmlWriter.printElementStartClose(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#validate()
     */
    boolean validate() {
        boolean valid = super.validate();

        // do additional validation

        return valid;
    }

}
