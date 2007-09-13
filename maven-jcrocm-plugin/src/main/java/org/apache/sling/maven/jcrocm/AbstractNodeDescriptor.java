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

/**
 * The <code>AbstractNodeDescriptor</code> is the base class for mostly node
 * based descriptors (bean, collection).
 */
public class AbstractNodeDescriptor extends AbstractItemDescriptor {

    public static final String PROXY = "proxy";

    public static final String AUTO_RETRIEVE = "autoRetrieve";

    public static final String AUTO_UPDATE = "autoUpdate";

    public static final String AUTO_INSERT = "autoInsert";

    public static final String JCR_TYPE = "jcrType";

    public static final String JCR_SAME_NAME_SIBBLINGS = "jcrSameNameSiblings";

    private boolean isProxy = true;

    private boolean isAutoRetrieve = true;

    private boolean isAutoUpdate = true;

    private boolean isAutoInsert = true;

    private String jcrType;

    private boolean isJcrSameNameSibblings;

    /**
     * @param log
     * @param tag
     */
    public AbstractNodeDescriptor(Log log, DocletTag tag, String fieldName) {
        super(log, tag, fieldName);

        jcrType = tag.getNamedParameter(JCR_TYPE);
        isJcrSameNameSibblings = Boolean.valueOf(
            tag.getNamedParameter(JCR_SAME_NAME_SIBBLINGS)).booleanValue();

        if (tag.getNamedParameter(PROXY) != null) {
            isProxy = Boolean.valueOf(tag.getNamedParameter(PROXY)).booleanValue();
        }
        if (tag.getNamedParameter(AUTO_RETRIEVE) != null) {
            isAutoRetrieve = Boolean.valueOf(
                tag.getNamedParameter(AUTO_RETRIEVE)).booleanValue();
        }
        if (tag.getNamedParameter(AUTO_UPDATE) != null) {
            isAutoUpdate = Boolean.valueOf(tag.getNamedParameter(AUTO_UPDATE)).booleanValue();
        }
        if (tag.getNamedParameter(AUTO_INSERT) != null) {
            isAutoInsert = Boolean.valueOf(tag.getNamedParameter(AUTO_INSERT)).booleanValue();
        }
    }

    void generate(XMLWriter xmlWriter) {
        super.generate(xmlWriter);

        xmlWriter.printAttribute(JCR_TYPE, jcrType);
        xmlWriter.printAttribute(JCR_SAME_NAME_SIBBLINGS,
            isJcrSameNameSibblings);

        if (!isProxy) {
            xmlWriter.printAttribute(PROXY, "false");
        }
        if (!isAutoRetrieve) {
            xmlWriter.printAttribute(AUTO_RETRIEVE, "false");
        }
        if (!isAutoUpdate) {
            xmlWriter.printAttribute(AUTO_UPDATE, "false");
        }
        if (!isAutoInsert) {
            xmlWriter.printAttribute(AUTO_INSERT, "false");
        }
    }

    boolean validate() {
        boolean valid = super.validate();

        // do additional validation

        return valid;
    }
}
