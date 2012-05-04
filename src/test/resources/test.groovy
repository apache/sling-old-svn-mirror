import org.apache.sling.api.resource.ResourceResolverFactory

def resourceResolverFactory = sling.getService(ResourceResolverFactory.class);
def authInfo = [
        (ResourceResolverFactory.USER):"admin",
        (ResourceResolverFactory.PASSWORD):"admin",
        foo:"bar",
]
def resourceResolver = resourceResolverFactory.getResourceResolver(authInfo)
assert resourceResolver.getAttribute("foo") == "bar"