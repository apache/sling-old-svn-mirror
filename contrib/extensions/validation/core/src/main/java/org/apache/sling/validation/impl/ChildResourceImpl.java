package org.apache.sling.validation.impl;

import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidatorLookupService;
import org.apache.sling.validation.impl.util.JCRBuilder;

/**
 * Implements a {@link ChildResource}
 */
public class ChildResourceImpl implements ChildResource {

    private String name;
    private Set<ResourceProperty> properties;

    public ChildResourceImpl(Resource modelResource, Resource childResource, ValidatorLookupService validatorLookupService) {
        String root = modelResource.getPath();
        if (!childResource.getPath().startsWith(root)) {
            throw new IllegalArgumentException("Expected resource " + childResource.getPath() + " to be under root path " + root);
        }
        name = childResource.getPath().replaceFirst(root + "/", "").replaceAll(Constants.CHILDREN + "/", "");
        properties = JCRBuilder.buildProperties(validatorLookupService, childResource.getChild(Constants.PROPERTIES));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<ResourceProperty> getProperties() {
        return properties;
    }
}
