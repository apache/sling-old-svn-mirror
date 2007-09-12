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
 * The <code>FieldDescriptor</code> class provides support for the
 * field-descriptor element of a class mapping, which has the following
 * attribute list definition:
 * 
 * <pre>
 *  &lt;!ATTLIST field-descriptor
 *      fieldName CDATA #REQUIRED
 *      fieldType CDATA #IMPLIED
 *      jcrName CDATA #IMPLIED 
 *      id (true | false) &quot;false&quot;
 *      path (true | false) &quot;false&quot;
 *      uuid (true | false) &quot;false&quot;
 *      converter CDATA #IMPLIED
 *      jcrDefaultValue CDATA #IMPLIED
 *      jcrValueConstraints CDATA #IMPLIED 
 *      jcrType (String | Date | Long | Double | Boolean | Binary) #IMPLIED
 *      jcrAutoCreated (true | false) &quot;false&quot;
 *      jcrMandatory (true | false) &quot;false&quot;
 *      jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) &quot;COPY&quot;
 *      jcrProtected (true | false) &quot;false&quot;
 *      jcrMultiple (true | false) &quot;false&quot;
 *  &gt;
 * </pre>
 */
public class FieldDescriptor extends AbstractItemDescriptor {

    public static final String TAG_FIELD_DESCRIPTOR = "ocm.field";

    public static final String ELEMENT_FIELD_DESCRIPTOR = "field-descriptor";

    public static final String FIELD_TYPE = "fieldType";

    public static final String ID = "id";

    public static final String PATH = "path";

    public static final String UUID = "uuid";

    public static final String CONVERTER = "converter";

    public static final String JCR_DEFAULT_VALUE = "jcrDefaultValue";

    public static final String JCR_VALUE_CONSTRAINTS = "jcrValueConstraints";

    public static final String JCR_TYPE = "jcrType";

    public static final String JCR_MULTIPLE = "jcrMultiple";

    private String fieldType;

    private boolean isId;

    private boolean isPath;

    private boolean isUuid;

    private String converter;

    private String jcrDefaultValue;

    private String jcrValueConstraints;

    private String jcrType;

    private boolean isJcrMultiple;

    static FieldDescriptor fromField(Log log, JavaField field) {

        DocletTag tag = field.getTagByName(TAG_FIELD_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        // field type is explicitly declared or Java field type
        String fieldType = tag.getNamedParameter(FIELD_TYPE);
        if (fieldType == null) {
            fieldType = field.getType().getJavaClass().getFullyQualifiedName();
        }
        
        return new FieldDescriptor(log, tag, field.getName(),
            fieldType);
    }

    static FieldDescriptor fromMethod(Log log, JavaMethod method) {

        DocletTag tag = method.getTagByName(TAG_FIELD_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        // field name is the method name, unless overwritten with the fieldName
        // tag
        String fieldName = tag.getNamedParameter(FIELD_NAME);
        if (fieldName == null) {
            fieldName = getFieldFromMethod(method);
        }

        // field type is explicitly declared or parameter or return value
        String fieldType = tag.getNamedParameter(FIELD_TYPE);
        if (fieldType == null) {
            if (method.getParameters() != null
                && method.getParameters().length == 1) {
                fieldType = method.getParameters()[0].getType().getJavaClass().getFullyQualifiedName();
            } else if (method.getReturns() != null) {
                fieldType = method.getReturns().getJavaClass().getFullyQualifiedName();
            }
        }

        return new FieldDescriptor(log, tag, fieldName, fieldType);
    }

    private FieldDescriptor(Log log, DocletTag tag, String fieldName,
            String fieldType) {
        super(log, tag, fieldName);

        this.fieldType = fieldType;

        isId = Boolean.valueOf(tag.getNamedParameter(ID)).booleanValue();
        isPath = Boolean.valueOf(tag.getNamedParameter(PATH)).booleanValue();
        isUuid = Boolean.valueOf(tag.getNamedParameter(UUID)).booleanValue();

        converter = tag.getNamedParameter(CONVERTER);
        jcrDefaultValue = tag.getNamedParameter(JCR_DEFAULT_VALUE);
        jcrValueConstraints = tag.getNamedParameter(JCR_VALUE_CONSTRAINTS);
        jcrType = tag.getNamedParameter(JCR_TYPE);
        isJcrMultiple = Boolean.valueOf(tag.getNamedParameter(JCR_MULTIPLE)).booleanValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#generate(org.apache.sling.maven.jcrocm.XMLWriter)
     */
    void generate(XMLWriter xmlWriter) {
        xmlWriter.printElementStart(ELEMENT_FIELD_DESCRIPTOR, true);

        super.generate(xmlWriter);

        xmlWriter.printAttribute(FIELD_TYPE, fieldType);
        xmlWriter.printAttribute(ID, isId);
        xmlWriter.printAttribute(PATH, isPath);
        xmlWriter.printAttribute(UUID, isUuid);
        xmlWriter.printAttribute(CONVERTER, converter);
        xmlWriter.printAttribute(JCR_DEFAULT_VALUE, jcrDefaultValue);
        xmlWriter.printAttribute(JCR_VALUE_CONSTRAINTS, jcrValueConstraints);
        xmlWriter.printAttribute(JCR_TYPE, jcrType);
        xmlWriter.printAttribute(JCR_MULTIPLE, isJcrMultiple);

        xmlWriter.printElementStartClose(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#validate()
     */
    boolean validate() {
        boolean valid = true;

        if (jcrType != null) {
            if (!jcrType.equals("String") && !jcrType.equals("Date")
                && !jcrType.equals("Long") && !jcrType.equals("Double")
                && !jcrType.equals("Boolean") && !jcrType.equals("Binary")) {
                log("Invalid JCR Field Type: " + jcrType);
                valid = false;
            }
        }

        valid &= super.validate();

        return valid;
    }
}
