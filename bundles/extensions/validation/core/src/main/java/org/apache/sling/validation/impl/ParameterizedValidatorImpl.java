package org.apache.sling.validation.impl;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.ValidatorTypeUtil;

public class ParameterizedValidatorImpl implements ParameterizedValidator {
    private final @Nonnull Validator<?> validator;
    private final @Nonnull ValueMap parameters;
    private final @Nonnull Class<?> type;
    
    public ParameterizedValidatorImpl(@Nonnull Validator<?> validator, @Nonnull ValueMap parameters) {
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
    public @Nonnull Validator<?> getValidator() {
        return validator;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getParameters()
     */
    @Override
    public @Nonnull ValueMap getParameters() {
        return parameters;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.validation.impl.ParameterizedValidator#getType()
     */
    @Override
    public @Nonnull Class<?> getType() {
        return type;
    }
}
