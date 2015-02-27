package org.apache.sling.serviceusermapping;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ServiceUserMapping</code> service can be used to retrieve an already registered service user mapping.
 * A service reference targeting a service user mapping will be satisfied only when <code>ServiceUserMapper.getServiceUserID</code>
 * will return the registered user ID in that bundle.
 * For example setting the reference target to "(subServiceName=mySubService)" ensures that your component only starts when the subService is available.
 * The subServiceName will not be set for mappings that do not have one, and those can be referenced with a negating target "(!(subServiceName=*))".
 * Trying to reference a sub service from a bundle for which it was not registered for will not work.
 */
@ProviderType
public interface ServiceUserMapping {


    /**
     * The name of the osgi property holding the sub service name.
     */
    static String SUBSERVICENAME = "subServiceName";


    /**
     * Returns the sub service name for this mapping.
     *
     * @return The sub service name for this mapping. This can be {@code null} if no sub service name is configured for this mapping.
     */
    String getSubServiceName();
}
