package org.apache.sling.validation.impl.util.examplevalidators;

import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.api.exceptions.SlingValidationException;

public class IntegerValidator implements Validator<Integer> {

    @Override
    public String validate(Integer data, ValueMap valueMap, Map<String, String> arguments)
            throws SlingValidationException {
        return null;
    }

}
