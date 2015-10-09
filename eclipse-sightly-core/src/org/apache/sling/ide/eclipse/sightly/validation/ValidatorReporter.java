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
package org.apache.sling.ide.eclipse.sightly.validation;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.html.core.internal.validation.HTMLValidationReporter;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.validate.ErrorInfo;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.w3c.dom.Node;

@SuppressWarnings("restriction")
public class ValidatorReporter extends HTMLValidationReporter {
    public ValidatorReporter(IValidator owner, IReporter reporter, IFile file, IStructuredModel model) {
        super(owner, reporter, file, model);
    }

    @Override
    public void report(ErrorInfo info) {
        int targetType = info.getTargetType();
        if (targetType == Node.ELEMENT_NODE && info.getState() == 11) {
            String name = info.getHint();
            if ("sly".equals(name) ) {
                return;
            }
        }
        
        super.report(info);
    }
}