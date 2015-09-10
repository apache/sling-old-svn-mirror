package org.apache.sling.resourceresolver.impl;

import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class BasicResolveContext implements ResolveContext<Object> {

    private final ResourceResolver resourceResolver;

    private final Map<String, String> resolveParameters;

    private final Object providerState;

    private final ResolveContext<?> parentResolveContext;

    private final ResourceProvider<?> parentResourceProvider;

    public BasicResolveContext(ResourceResolver resourceResolver, Map<String, String> resolveParameters,
            Object providerState, ResolveContext<?> parentResolveContext, ResourceProvider<?> parentResourceProvider) {
        this.resourceResolver = resourceResolver;
        this.resolveParameters = resolveParameters;
        this.providerState = providerState;
        this.parentResolveContext = parentResolveContext;
        this.parentResourceProvider = parentResourceProvider;
    }

    public BasicResolveContext(ResourceResolver resourceResolver, Map<String, String> resolveParameters, Object providerState) {
        this(resourceResolver, resolveParameters, providerState, null, null);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public Map<String, String> getResolveParameters() {
        return resolveParameters;
    }

    @Override
    public Object getProviderState() {
        return providerState;
    }

    @Override
    public ResolveContext<?> getParentResolveContext() {
        return parentResolveContext;
    }

    @Override
    public ResourceProvider<?> getParentResourceProvider() {
        return parentResourceProvider;
    }

}
