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
package org.apache.sling.ide.eclipse.internal.validation;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.xml.core.internal.validation.core.AbstractNestedValidator;
import org.eclipse.wst.xml.core.internal.validation.core.NestedValidatorContext;
import org.eclipse.wst.xml.core.internal.validation.eclipse.Validator;

@SuppressWarnings("restriction")
/** Almost the standard xml validator from WST with the following deviations - missing grammar is never marked - also .content.xml is
 * validated (the default XML validator skips all files starting with ".") */
public class IgnoreMissingGrammarXmlValidator extends Validator {

    private final String GET_FILE = "getFile"; //$NON-NLS-1$
    private final String GET_PROJECT_FILES = "getAllFiles"; //$NON-NLS-1$
    private final String GET_INPUTSTREAM = "inputStream"; //$NON-NLS-1$

    @Override
    protected void setupValidation(NestedValidatorContext context) {
        super.setupValidation(context);
        // always ignore missing grammar constraints being referenced in the XML
        indicateNoGrammar = 0;
    }

    /** Perform the validation using version 2 of the validation framework. Copied from {@link AbstractNestedValidator#validate(IResource,
     * int, ValidationState, IProgressMonitor). Cannot use original one as that skips resources starting with "." */
    @Override
    public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor) {
        ValidationResult result = new ValidationResult();
        IFile file = null;
        if (resource instanceof IFile)
            file = (IFile) resource;
        if (file != null && shouldValidate(file)) {
            IReporter reporter = result.getReporter(monitor);

            NestedValidatorContext nestedcontext = getNestedContext(state, false);
            boolean teardownRequired = false;
            if (nestedcontext == null) {
                // validationstart was not called, so manually setup and tear down
                nestedcontext = getNestedContext(state, true);
                nestedcontext.setProject(file.getProject());
                setupValidation(nestedcontext);
                teardownRequired = true;
            } else {
                nestedcontext.setProject(file.getProject());
            }
            doValidate(file, null, result, reporter, nestedcontext);

            if (teardownRequired)
                teardownValidation(nestedcontext);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.wst.validation.internal.provisional.core.IValidatorJob#validateInJob(org.eclipse.wst.validation.internal.provisional.core
     * .IValidationContext, org.eclipse.wst.validation.internal.provisional.core.IReporter)
     * 
     * Copied from {@link AbstractNestedValidator#validateInJob(IValidationContext context, IReporter reporter). Cannot use original one as
     * that skips resources starting with "."
     */
    @Override
    public IStatus validateInJob(IValidationContext context, IReporter reporter) throws ValidationException {
        NestedValidatorContext nestedcontext = new NestedValidatorContext();
        setupValidation(nestedcontext);
        String[] fileURIs = context.getURIs();
        if (fileURIs != null && fileURIs.length > 0) {
            int numFiles = fileURIs.length;
            for (int i = 0; i < numFiles && !reporter.isCancelled(); i++) {
                String fileName = fileURIs[i];
                if (fileName != null) {
                    Object[] parms = { fileName };

                    IFile file = (IFile) context.loadModel(GET_FILE, parms);
                    if (file != null && shouldValidate(file)) {
                        nestedcontext.setProject(file.getProject());
                        // The helper may not have a file stored in it but may have an InputStream if being
                        // called from a source other than the validation framework such as an editor.
                        if (context.loadModel(GET_INPUTSTREAM) instanceof InputStream) {
                            doValidate(file, (InputStream) context.loadModel(GET_INPUTSTREAM), null, reporter, nestedcontext); // do we need
                                                                                                                             // the
                                                                                                                             // fileName?
                                                                                                                             // what is int
                                                                                                                             // ruleGroup?
                        } else {
                            doValidate(file, null, null, reporter, nestedcontext);
                        }
                    }
                }
            }
        }
        // TODO: Is this needed? Shouldn't the framework pass the complete list?
        // Should I know that I'm validating a project as opposed to files?
        else {
            Object[] parms = { getValidatorID() };
            Collection files = (Collection) context.loadModel(GET_PROJECT_FILES, parms);
            // files can be null if they're outside of the workspace
            if (files != null) {
                Iterator iter = files.iterator();
                while (iter.hasNext() && !reporter.isCancelled()) {
                    IFile file = (IFile) iter.next();
                    if (shouldValidate(file)) {
                        doValidate(file, null, null, reporter, nestedcontext);
                    }
                }
            }
        }

        teardownValidation(nestedcontext);
        if (reporter.isCancelled())
            return Status.CANCEL_STATUS;
        return Status.OK_STATUS;
    }

    /** Call the original private method named validate with the same parameters from {@link AbstractNestedValidator} through reflection.
     * 
     * @param file
     * @param inputstream
     * @param result
     * @param reporter
     * @param context */
    private void doValidate(IFile file, InputStream inputstream, ValidationResult result, IReporter reporter,
            NestedValidatorContext context) {
        try {
            Method method = AbstractNestedValidator.class.getDeclaredMethod("validate", IFile.class, InputStream.class,
                    ValidationResult.class,
                    IReporter.class, NestedValidatorContext.class);
            method.setAccessible(true);
            method.invoke(this, file, inputstream, result, reporter, context);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            Logger logger = Activator.getDefault().getPluginLogger();
            logger.error("Failed calling validate method on AbstractNestedValidator, probably WTP version is incompatible.", e);
        }
    }

    /** Determine if a given file should be validated. Mostly copied from {@link AbstractNestedValidator#shouldValidate(...)} but will not
     * skip {@code .content.xml} files.
     * 
     * @param file The file that may be validated.
     * @return True if the file should be validated, false otherwise. */
    private static boolean shouldValidate(IFile file) {
        IResource resource = file;
        do {
            if (resource.isDerived() || resource.isTeamPrivateMember() ||
                    !resource.isAccessible() || (resource.getName().charAt(0) == '.' && !".content.xml".equals(resource.getName()))) {
                return false;
            }
            resource = resource.getParent();
        } while ((resource.getType() & IResource.PROJECT) == 0);

        return true;
    }

}
