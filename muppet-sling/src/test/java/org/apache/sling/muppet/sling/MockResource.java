package org.apache.sling.muppet.sling;

import java.util.HashMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import static org.apache.sling.muppet.sling.api.RulesResourceParser.NAMESPACE;
import static org.apache.sling.muppet.sling.api.RulesResourceParser.RULE_NAME;
import static org.apache.sling.muppet.sling.api.RulesResourceParser.QUALIFIER;
import static org.apache.sling.muppet.sling.api.RulesResourceParser.EXPRESSION;

class MockResource implements Resource {
    private final ResourceResolver resolver;
    private final ValueMap valueMap;
    private final String path;
    
    @SuppressWarnings("serial")
    static class PropertiesMap extends HashMap<String, Object> implements ValueMap {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String key, Class<T> type) {
            // we only need Strings in our tests
            final Object value = get(key);
            return value == null ? null : (T)value.toString();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String key, T defaultValue) {
            final Object value = get(key);
            if(value == null) {
                return defaultValue;
            }
            return (T)value;
        }
        
    };
    
    MockResource(MockResolver resolver, String path, String namespace, String ruleName, String qualifier, String expression) {
        this.resolver = resolver;
        this.path = path;
        this.valueMap = new PropertiesMap();
        valueMap.put(NAMESPACE, namespace);
        valueMap.put(RULE_NAME, ruleName);
        valueMap.put(QUALIFIER, qualifier);
        valueMap.put(EXPRESSION, expression);
        resolver.addResource(this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> target) {
        if(target == ValueMap.class) {
            return (AdapterType)valueMap;
        }
        return null;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return null;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public String getResourceType() {
        return null;
    }
}
