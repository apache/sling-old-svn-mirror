package org.apache.sling.serviceusermapping;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ServiceUserMapped</code> is a marker service that can be used to ensure that there is an already registered mapping for a certain service/subService.
 * A service reference targeting a <code>ServiceUserMapped</code> will be satisfied only if <code>ServiceUserMapper.getServiceUserID</code>
 * will resolve the subService to an userID.
 * For example setting the reference target to "(subServiceName=mySubService)" ensures that your component only starts when the subService is available.
 * The subServiceName will not be set for mappings that do not have one, and those can be referenced with a negating target "(!(subServiceName=*))".
 * Trying to reference a sub service from a bundle for which it was not registered for will not work.
 */
@ProviderType
public interface ServiceUserMapped {


    /**
     * The name of the osgi property holding the sub service name.
     */
    static String SUBSERVICENAME = "subServiceName";

}
