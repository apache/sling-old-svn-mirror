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
package org.apache.sling.ide.eclipse.sightly.model;

public class Attribute implements ProposalDescription {

    private final String attributeName;
    private final String additionalInfo;

    public Attribute(String attributeName) {
        this.attributeName = attributeName;
        this.additionalInfo = ModelElements.props.getProperty("dataSly" + Character.toUpperCase(attributeName.charAt(0))
                + attributeName.substring(1) + "_description");
    }

    public String getLabel() {
        return "data-sly-" + attributeName;
    }

    @Override
    public String getInsertionText() {
        return getLabel() + "=\"\"";
    }

    @Override
    public String getAdditionalInfo() {
        return additionalInfo;
    }
}