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
package org.apache.sling.ide.eclipse.ui.actions;

import org.apache.sling.ide.eclipse.ui.internal.ExportWizard;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.PlatformUI;

/** Export content at a certain node level**/
public class JcrNodeExportContentHandler extends AbstractJcrNodeImportExportContentHandler {
    
    @Override
    protected Wizard createWizard(IResource resource) {

        ExportWizard wiz = new ExportWizard();
        wiz.init(PlatformUI.getWorkbench(), resource);
        return wiz;
    }
}