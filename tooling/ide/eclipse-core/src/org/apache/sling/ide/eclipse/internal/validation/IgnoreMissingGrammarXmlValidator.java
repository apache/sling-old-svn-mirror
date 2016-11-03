package org.apache.sling.ide.eclipse.internal.validation;

import org.eclipse.wst.xml.core.internal.validation.core.NestedValidatorContext;
import org.eclipse.wst.xml.core.internal.validation.eclipse.Validator;

@SuppressWarnings("restriction")
public class IgnoreMissingGrammarXmlValidator extends Validator {

    @Override
    protected void setupValidation(NestedValidatorContext context) {
        super.setupValidation(context);
        // always ignore missing grammar constraints being referenced in the XML
        indicateNoGrammar = 0;
    }


}
