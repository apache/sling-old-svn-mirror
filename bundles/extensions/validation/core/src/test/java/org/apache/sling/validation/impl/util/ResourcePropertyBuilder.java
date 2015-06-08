package org.apache.sling.validation.impl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.model.ParameterizedValidatorImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyImpl;

public class ResourcePropertyBuilder {

    public boolean optional;
    public boolean multiple;
    String nameRegex;
    final List<ParameterizedValidator> validators;

    public ResourcePropertyBuilder() {
        validators = new ArrayList<ParameterizedValidator>();
        this.nameRegex = null;
        this.optional = false;
        this.multiple = false;
    }

    public ResourcePropertyBuilder nameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
        return this;
    }
    
    public ResourcePropertyBuilder validator(@Nonnull Validator<?> validator) {
        validators.add(new ParameterizedValidatorImpl(validator, new HashMap<String, Object>()));
        return this;
    }

    public ResourcePropertyBuilder validator(@Nonnull Validator<?> validator, @Nonnull Map<String, Object> parameters) {
        validators.add(new ParameterizedValidatorImpl(validator, parameters));
        return this;
    }
    
    public ResourcePropertyBuilder optional() {
        this.optional = true;
        return this;
    }
    
    public ResourcePropertyBuilder multiple() {
        this.multiple = true;
        return this;
    }
    
    public ResourceProperty build(String name) {
        return new ResourcePropertyImpl(name, nameRegex, multiple, !optional, validators);
    }
}
