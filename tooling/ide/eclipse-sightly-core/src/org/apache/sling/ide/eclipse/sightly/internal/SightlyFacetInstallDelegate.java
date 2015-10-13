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

import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.validation.Validator;
import org.eclipse.wst.validation.internal.ValManager;
import org.eclipse.wst.validation.internal.ValPrefManagerProject;
import org.eclipse.wst.validation.internal.ValidatorMutable;
import org.eclipse.wst.validation.internal.model.ProjectPreferences;

/**
 * The <tt>SightlyFacetInstallDelegate</tt> ensures that the HTML validator is not active
 * 
 * <p>Since the Sightly validators delegate to the HTML counterparts and also ensure that 
 * Sightly-specific constructs do not trigger validation problems it is completely safe
 * to disable the HTML validator</p> 
 */
@SuppressWarnings("restriction")
public class SightlyFacetInstallDelegate implements IDelegate {

    private static final String HTML_VALIDATOR_ID = "org.eclipse.wst.html.core.HTMLValidator";

    @Override
    public void execute(IProject project, IProjectFacetVersion version, Object config, IProgressMonitor monitor)
            throws CoreException {
        
        Logger logger = Activator.getDefault().getLogger();

        Validator[] validators = ValManager.getDefault().getValidators(project);
        ValidatorMutable[] mutis = new ValidatorMutable[validators.length];
        for ( int i = 0 ; i < validators.length; i++) {
            mutis[i] = new ValidatorMutable(validators[i]);
        }

        boolean changed = false;
        
        for ( ValidatorMutable validator : mutis ) {
            if ( HTML_VALIDATOR_ID.equals(validator.getId()) ) {
                if ( validator.isManualValidation() || validator.isBuildValidation() ) {
                    
                    validator.setBuildValidation(false);
                    validator.setManualValidation(false);
                    changed = true;
                    
                    logger.trace("Disabled {0} for project {1}", validator, project.getName());
                    
                    break;
                }
            }
        }

        ProjectPreferences projectPreferences = ValManager.getDefault().getProjectPreferences(project);
        if ( !projectPreferences.getOverride() ) {
            projectPreferences = new ProjectPreferences(project, true, projectPreferences.getSuspend(), null);
        }

        if ( changed || !projectPreferences.getOverride() ) {
            ValPrefManagerProject prefManager = new ValPrefManagerProject(project);
            prefManager.savePreferences(projectPreferences, mutis);
        }
               
    }
}
