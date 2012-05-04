
function AssertException(message) { this.message = message; }
AssertException.prototype.toString = function () {
  return 'AssertException: ' + this.message;
}

function assert(exp, message) {
  if (!exp) {
    throw new AssertException(message);
  }
}


var resourceResolverFactory = sling.getService(Packages.org.apache.sling.api.resource.ResourceResolverFactory);
var authInfo = new java.util.HashMap()
authInfo.put("user.name","admin")
authInfo.put("user.password",new java.lang.String("admin").toCharArray())
authInfo.put("foo","bar")


assert(resourceResolverFactory != null, "Factory cannot be null");
var resourceResolver = resourceResolverFactory.getResourceResolver(authInfo)
assert(resourceResolver.getAttribute("foo") == "bar", 'Object is null');
out.println ("test")