/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.core.objects;

import org.apache.sling.jcr.resource.AbstractMappedObject;

/**
 * The <code>AbstractSelectableMappedObject</code> class extends the
 * <code>BaseContent</code> class by implementing the
 * {@link SelectableObject} interface hence supporting selection as defined by
 * the Sling core bundle.
 *
 * @ocm.mapped jcrType="sling:SelectableObject" discriminator="false"
 */
public abstract class AbstractSelectableMappedObject extends AbstractMappedObject implements
        SelectableObject {

    /** @ocm.bean jcrName="sling:selector" */
    private Selector selector;

    public Selector getSelector() {
        return this.selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }
}