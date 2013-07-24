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

import com.thoughtworks.qdox.model.AbstractBaseJavaEntity;
import org.apache.maven.plugin.logging.Log;

import com.thoughtworks.qdox.model.AbstractJavaEntity;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaClassParent;

/**
 * The <code>AbstractDescriptorElement</code> serves as the base class for the
 * descriptor objects read from the Doclet tags.
 */
abstract class AbstractDescriptorElement {

    private final Log log;

    protected final DocletTag tag;

    protected AbstractDescriptorElement(Log log, DocletTag tag) {
        this.log = log;
        this.tag = tag;
    }

    abstract boolean validate();

    abstract void generate(XMLWriter xmlWriter);

    JavaClass getJavaClass() {
        AbstractBaseJavaEntity aje = tag.getContext();
        if (aje instanceof JavaClass) {
            return (JavaClass) aje;
        }

        JavaClassParent parent = aje.getParent();
        if (parent instanceof JavaClass) {
            return (JavaClass) parent;
        }

        return null;
    }

    protected void log(String message) {
        log.error("@" + tag.getName() + ": " + message + " ("
            + tag.getContext().getName() + ", line "
            + tag.getLineNumber() + ")");
    }

    protected void warn(String message) {
        log.warn(message);
    }
}
