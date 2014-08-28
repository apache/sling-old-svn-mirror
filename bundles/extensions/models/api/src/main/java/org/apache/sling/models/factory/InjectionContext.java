package org.apache.sling.models.factory;

import java.lang.reflect.Type;

public class InjectionContext {
    private final String injectorName;
    private final String name;
    private final Type type;
    private final String annotatedElementName;
    
    public InjectionContext(String injectorName, String name, Type type, String annotatedElementName) {
        super();
        this.injectorName = injectorName;
        this.name = name;
        this.type = type;
        this.annotatedElementName = annotatedElementName;
    }

    public String getInjectorName() {
        return (injectorName == null) ? "any" : injectorName;
    }
    
    @Override
    public String toString() {
        return "injector '" + getInjectorName() + "' into element '" + annotatedElementName + "' with name '" + name + "' and type '" + type + "'";
    }
    
}
