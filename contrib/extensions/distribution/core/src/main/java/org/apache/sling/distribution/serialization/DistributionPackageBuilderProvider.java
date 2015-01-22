package org.apache.sling.distribution.serialization;


/**
 * A helper interface to allow finding registered {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder}s
 */
public interface DistributionPackageBuilderProvider {

    /**
     * Finds a package builder that has the specified package type.
     * @param type the package type
     * @return a {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder} if one is already registered for that type
     * or null otherwise
     */
    DistributionPackageBuilder getPackageBuilder(String type);
}
