package org.apache.sling.validation.impl.util.examplevalidators;

import org.apache.sling.validation.api.Validator;

public abstract class AbstractValidatorWithAdditionalType<A,T,B> implements Validator<T>{
    public abstract A getA();
    public abstract B getB();
}
