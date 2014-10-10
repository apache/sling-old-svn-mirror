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
package org.apache.sling.ide.eclipse.core;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

public abstract class ResourceUtil {

    /**
     * This property is set by code which imports content from the repository into the workspace
     * 
     * <p>
     * It serves to distinguish between changes which are triggered by the user directly and changes which are triggered
     * by an import run.
     * </p>
     * 
     * <p>
     * If an exporter finds this property and the property of the {#link {@link IResource#getModificationStamp()} is
     * older than or equal to the value of this property, the change should be ignored.
     * </p>
     */
    public static final QualifiedName QN_IMPORT_MODIFICATION_TIMESTAMP = new QualifiedName(Activator.PLUGIN_ID,
            "importModificationTimestamp");

    private ResourceUtil() {

    }
}
