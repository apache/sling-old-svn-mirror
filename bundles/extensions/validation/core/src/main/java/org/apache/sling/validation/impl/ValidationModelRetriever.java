package org.apache.sling.validation.impl;

import org.apache.sling.validation.api.ValidationModel;

public interface ValidationModelRetriever {
    public ValidationModel getModel(String resourceType, String resourcePath);
}
