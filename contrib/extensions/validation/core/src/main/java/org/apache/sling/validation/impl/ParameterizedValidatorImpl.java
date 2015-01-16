package org.apache.sling.validation.impl;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.ValidatorTypeUtil;

public class ParameterizedValidatorImpl implements ParameterizedValidator {
    private final Validator<?> validator;
    private final ValueMap parameters;
    private final Class<?> type;
    
    public ParameterizedValidatorImpl(Validator<?> validator, ValueMap parameters) {
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
    public ValueMap getParameters() {
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
