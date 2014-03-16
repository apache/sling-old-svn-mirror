package org.apache.sling.resourceaccesssecurity.it.impl.providers.unsecured;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.resourceaccesssecurity.it.impl.providers.SimpleResourceProvider;

@Component(metatype = true, label = "Unsecured ResourceProvider")
@Service(value = ResourceProvider.class)
@Properties({
        @Property(name = ResourceProvider.ROOTS, value = "/providers/unsecured/read" )

})
public class UnsecuredReadProvider extends SimpleResourceProvider implements ResourceProvider {
}
