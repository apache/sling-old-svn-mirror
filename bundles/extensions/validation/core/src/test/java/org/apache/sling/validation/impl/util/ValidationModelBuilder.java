package org.apache.sling.validation.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.impl.model.ValidationModelImpl;

public class ValidationModelBuilder {

    private final List<ResourceProperty> resourceProperties;
    private final List<ChildResource> children;
    private final Collection<String> applicablePaths;
    
    public ValidationModelBuilder() {
        resourceProperties = new ArrayList<ResourceProperty>();
        children = new ArrayList<ChildResource>();
        applicablePaths = new ArrayList<String>();
    }
    
    public ValidationModelBuilder resourceProperty(ResourceProperty resourceProperty) {
        resourceProperties.add(resourceProperty);
        return this;
    }
    
    public ValidationModelBuilder childResource(ChildResource childResource) {
        children.add(childResource);
        return this;
    }
    
    public ValidationModelBuilder applicablePath(String applicablePath) {
        applicablePaths.add(applicablePath);
        return this;
    }
    
    public ValidationModel build(String path, String validatedResourceType) {
        return new ValidationModelImpl(path, resourceProperties, validatedResourceType, applicablePaths.toArray(new String[0]), children);
    }
}
