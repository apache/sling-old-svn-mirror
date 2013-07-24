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
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * The <code>AbstractItemDescriptor</code> is the base class for the field,
 * bean and collection descriptors providing support for the common
 * configuration elements.
 */
abstract class AbstractItemDescriptor extends AbstractDescriptorElement {

    /**
     * The name of the Doclet Tag attribute naming the field to which the
     * descriptor applies (value is "fieldName").
     */
    public static final String FIELD_NAME = "fieldName";

    /**
     * The name of the Doclet Tag attribute naming the JCR item to which the
     * descriptor applies (value is "jcrName").
     */
    public static final String JCR_NAME = "jcrName";

    /**
     * The name of the Doclet Tag attribute naming the field to which the
     * descriptor applies (value is "fieldName").
     */
    public static final String JCR_AUTO_CREATED = "jcrAutoCreated";

    public static final String JCR_MANDATORY = "jcrMandatory";

    public static final String JCR_ON_PARENT_VERSION = "jcrOnParentVersion";

    public static final String JCR_PROTECTED = "jcrProtected";

    private String fieldName;

    private String jcrName;

    private boolean isJcrAutoCreated;

    private boolean isJcrMandatory;

    private String jcrOnParentVersion;

    private boolean isJcrProtected;

    /**
     * @param log
     * @param tag
     */
    public AbstractItemDescriptor(Log log, DocletTag tag, String fieldName) {
        super(log, tag);

        this.fieldName = fieldName;
        jcrName = tag.getNamedParameter(JCR_NAME);
        isJcrAutoCreated = Boolean.valueOf(
            tag.getNamedParameter(JCR_AUTO_CREATED)).booleanValue();
        isJcrMandatory = Boolean.valueOf(tag.getNamedParameter(JCR_MANDATORY)).booleanValue();
        jcrOnParentVersion = tag.getNamedParameter(JCR_ON_PARENT_VERSION);
        isJcrProtected = Boolean.valueOf(tag.getNamedParameter(JCR_PROTECTED)).booleanValue();
    }

    void generate(XMLWriter xmlWriter) {
        xmlWriter.printAttribute(FIELD_NAME, fieldName);
        xmlWriter.printAttribute(JCR_NAME, jcrName);
        xmlWriter.printAttribute(JCR_AUTO_CREATED, isJcrAutoCreated);
        xmlWriter.printAttribute(JCR_MANDATORY, isJcrMandatory);
        xmlWriter.printAttribute(JCR_ON_PARENT_VERSION, jcrOnParentVersion);
        xmlWriter.printAttribute(JCR_PROTECTED, isJcrProtected);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#validate()
     */
    boolean validate() {
        boolean valid = true;

        if (jcrOnParentVersion != null) {
            if (!jcrOnParentVersion.equals("COPY")
                && !jcrOnParentVersion.equals("VERSION")
                && !jcrOnParentVersion.equals("INITIALIZE")
                && !jcrOnParentVersion.equals("COMPUTE")
                && !jcrOnParentVersion.equals("IGNORE")
                && !jcrOnParentVersion.equals("ABORT")) {
                log("Invalid JCR Field OnParentVersion: " + jcrOnParentVersion);
                valid = false;
            }
        }

        return valid;
    }

    static String getFieldFromMethod(JavaMethod method) {
        String fieldName = method.getName();
        if ((fieldName.startsWith("get") || fieldName.startsWith("set"))
            && fieldName.length() >= 4) {
            fieldName = Character.toLowerCase(fieldName.charAt(3))
                + fieldName.substring(4);
        }
        return fieldName;
    }
}
