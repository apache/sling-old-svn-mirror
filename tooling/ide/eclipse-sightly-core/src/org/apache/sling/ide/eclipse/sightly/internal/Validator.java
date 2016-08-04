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
package org.apache.sling.ide.eclipse.sightly.internal;

import org.apache.sling.ide.eclipse.sightly.SightlyFacetHelper;
import org.apache.sling.ide.eclipse.sightly.validation.ValidatorReporter;
import org.eclipse.core.resources.IFile;
import org.eclipse.wst.html.core.internal.validation.HTMLValidationReporter;
import org.eclipse.wst.html.core.internal.validation.HTMLValidator;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

@SuppressWarnings("restriction")
public class Validator extends HTMLValidator {

    @Override
    protected HTMLValidationReporter getReporter(IReporter reporter, IFile file, IDOMModel model) {
        
        if ( SightlyFacetHelper.hasSightlyFacet(file.getProject() )) {
            return new ValidatorReporter(this, reporter, file, model);
        }
        
        return super.getReporter(reporter, file, model);
    }
}
