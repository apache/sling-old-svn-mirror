package org.apache.sling.models.impl;

import org.apache.sling.models.factory.InjectionContext;

public class InjectionResult {
   
    private final boolean isError;
    private final InjectionContext context;
    
    
    public InjectionResult(boolean isError, InjectionContext context) {
        this.isError = isError;
        this.context = context;
    }
    
    public boolean isOk() {
        return !isError;
    }
    
    public InjectionContext getContext() {
        return context;
    };
}
