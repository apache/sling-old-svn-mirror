package org.apache.sling.validation.impl;

import java.util.Map;

import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.ValidatorTypeUtil;

public class ParameterizedValidatorImpl implements ParameterizedValidator {
    private final Validator<?> validator;
    private final Map<String, String> parameters;
    private final Class<?> type;
    
    public ParameterizedValidatorImpl(Validator<?> validator, Map<String, String> parameters) {
        super();
        this.validator = validator;
        this.parameters = parameters;
        // cache type information as this is using reflection
        this.type = ValidatorTypeUtil.getValidatorType(validator);
    }

    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getValidator()
     */
    @Override
    public Validator<?> getValidator() {
        return validator;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getParameters()
     */
    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getType()
     */
    @Override
    public Class<?> getType() {
        return type;
    }
}
