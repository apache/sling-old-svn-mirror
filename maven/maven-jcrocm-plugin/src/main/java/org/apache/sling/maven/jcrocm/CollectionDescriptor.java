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
 * The <code>CollectionDescriptor</code> class provides support for the
 * collection-descriptor element of a class mapping, which has the following
 * attribute list definition:
 * 
 * <pre>
 *  &lt;!ATTLIST collection-descriptor
 *      fieldName CDATA #REQUIRED
 *      jcrName CDATA #IMPLIED 
 *      proxy (true | false) &quot;false&quot;
 *      autoRetrieve (true|false) &quot;true&quot;
 *      autoUpdate (true|false) &quot;true&quot;
 *      autoInsert (true|false) &quot;true&quot;  
 *      elementClassName CDATA #IMPLIED
 *      collectionClassName CDATA #IMPLIED
 *      collectionConverter CDATA #IMPLIED
 *      jcrType CDATA #IMPLIED
 *      jcrAutoCreated (true | false) &quot;false&quot;   
 *      jcrMandatory (true | false) &quot;false&quot;
 *      jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) &quot;COPY&quot;
 *      jcrProtected (true | false) &quot;false&quot;
 *      jcrSameNameSiblings (true | false) &quot;false&quot;  
 *  &gt;
 * </pre>
 */
public class CollectionDescriptor extends AbstractNodeDescriptor {

    public static final String TAG_COLLECTION_DESCRIPTOR = "ocm.collection";

    public static final String ELEMENT_COLLECTION_DESCRIPTOR = "collection-descriptor";

    private static final String ELEMENT_CLASS_NAME = "elementClassName";

    private static final String COLLECTION_CLASS_NAME = "collectionClassName";

    private static final String COLLECTION_CONVERTER = "collectionConverter";

    private String elementClassName;

    private String collectionClassName;

    private String collectionConverter;

    static CollectionDescriptor fromField(Log log, JavaField field) {

        DocletTag tag = field.getTagByName(TAG_COLLECTION_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        return new CollectionDescriptor(log, tag, field.getName());
    }

    static CollectionDescriptor fromMethod(Log log, JavaMethod method) {

        DocletTag tag = method.getTagByName(TAG_COLLECTION_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        // field name is the method name, unless overwritten with the fieldName
        // tag
        String fieldName = tag.getNamedParameter(FIELD_NAME);
        if (fieldName == null) {
            fieldName = getFieldFromMethod(method);
        }

        return new CollectionDescriptor(log, tag, fieldName);
    }

    /**
     * @param log
     * @param tag
     */
    public CollectionDescriptor(Log log, DocletTag tag, String fieldName) {
        super(log, tag, fieldName);

        elementClassName = tag.getNamedParameter(ELEMENT_CLASS_NAME);
        collectionClassName = tag.getNamedParameter(COLLECTION_CLASS_NAME);
        collectionConverter = tag.getNamedParameter(COLLECTION_CONVERTER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#generate(org.apache.sling.maven.jcrocm.XMLWriter)
     */
    void generate(XMLWriter xmlWriter) {
        xmlWriter.printElementStart(ELEMENT_COLLECTION_DESCRIPTOR, true);
        super.generate(xmlWriter);

        xmlWriter.printAttribute(ELEMENT_CLASS_NAME, elementClassName);
        xmlWriter.printAttribute(COLLECTION_CLASS_NAME, collectionClassName);
        xmlWriter.printAttribute(COLLECTION_CONVERTER, collectionConverter);

        xmlWriter.printElementStartClose(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#validate()
     */
    boolean validate() {
        boolean valid = super.validate();

        // ensure element class name
        if (elementClassName == null) {
            elementClassName = Object.class.getName();
        }

        return valid;
    }

}
