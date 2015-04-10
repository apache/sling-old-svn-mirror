package org.apache.sling.validation.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.validation.api.ChildResource;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.util.JCRBuilder;

/**
 * Implements a {@link ChildResource}
 */
public class ChildResourceImpl implements ChildResource {

    private final String name;
    private final Pattern namePattern;
    private final Set<ResourceProperty> properties;
    private final List<ChildResource> children;
    private final boolean isRequired;

    public ChildResourceImpl(Resource modelResource, Resource childResource, Map<String, Validator<?>> validatorsMap, List<ChildResource> children) {
        String root = modelResource.getPath();
        if (!childResource.getPath().startsWith(root)) {
            throw new IllegalArgumentException("Expected resource " + childResource.getPath() + " to be under root path " + root);
        }
        // if pattern is set, always use that
        ValueMap childrenProperties = childResource.adaptTo(ValueMap.class);
        if (childrenProperties == null) {
            throw new IllegalStateException("Could not adapt resource " + childResource.getPath() + " to ValueMap");
        }
        if (childrenProperties.containsKey(Constants.NAME_REGEX)) {
            name = null;
            String regex = childrenProperties.get(Constants.NAME_REGEX, String.class);
            try {
                namePattern = Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Given regular expression is invalid: " + regex);
            }
        } else {
            // otherwise fall back to the name
            name = childResource.getName();
            namePattern = null;
        }
        isRequired = !PropertiesUtil.toBoolean(childrenProperties.get(Constants.OPTIONAL), false);
        properties = JCRBuilder.buildProperties(validatorsMap, childResource.getChild(Constants.PROPERTIES));
        this.children = children;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<ResourceProperty> getProperties() {
        return properties;
    }

    @Override
    public Pattern getNamePattern() {
        return namePattern;
    }
    
    public List<ChildResource> getChildren() {
        return children;
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }
}
