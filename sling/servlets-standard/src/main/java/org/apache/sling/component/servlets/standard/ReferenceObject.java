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
package org.apache.sling.component.servlets.standard;

import org.apache.sling.jcr.resource.AbstractMappedObject;

/**
 * The <code>ReferenceObject</code> TODO
 *
 * @ocm.mapped jcrType="sling:Reference" discriminator="false"
 */
public class ReferenceObject extends AbstractMappedObject {

    /** @ocm.field jcrName="sling:path" */
    private String reference;

    // ---------- Mapped Content -----------------------------------------------

    /**
     * @return the reference
     */
    public String getReference() {
        return this.reference;
    }

    /**
     * @param value the reference to set
     */
    public void setReference(String value) {
        this.reference = value;
    }
}
