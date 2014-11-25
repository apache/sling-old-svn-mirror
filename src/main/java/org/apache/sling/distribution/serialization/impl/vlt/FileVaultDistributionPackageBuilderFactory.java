package org.apache.sling.distribution.serialization.impl.vlt;

import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuildingException;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;
import org.apache.sling.distribution.serialization.impl.ResourceSharedDistributionPackageBuilder;
import org.osgi.framework.BundleContext;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Map;

@Component(metatype = true,
        label = "Sling Distribution - File Vault Package Builder Factory",
        description = "OSGi configuration for file vault package builders",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionPackageBuilder.class)
public class FileVaultDistributionPackageBuilderFactory implements DistributionPackageBuilder {


    /**
     * name of this component.
     */
    @Property
    public static final String NAME = DistributionComponentUtils.NAME;

    /**
     * import mode property for file vault package builder
     */
    @Property
    public static final String PACKAGE_BUILDER_FILEVLT_IMPORT_MODE = "importMode";

    /**
     * ACL handling property for file vault package builder
     */
    @Property
    public static final String PACKAGE_BUILDER_FILEVLT_ACLHANDLING = "aclHandling";

    DistributionPackageBuilder packageBuilder;

    @Reference
    private DistributionEventFactory distributionEventFactory;

    @Reference
    private Packaging packaging;



    @Activate
    public void activate(BundleContext context, Map<String, Object> config) {

        String importMode = PropertiesUtil.toString(config.get(PACKAGE_BUILDER_FILEVLT_IMPORT_MODE), null);
        String aclHandling = PropertiesUtil.toString(config.get(PACKAGE_BUILDER_FILEVLT_ACLHANDLING), null);
        if (importMode != null && aclHandling != null) {
            packageBuilder = new ResourceSharedDistributionPackageBuilder(new FileVaultDistributionPackageBuilder(packaging, distributionEventFactory, importMode, aclHandling));
        } else {
            packageBuilder = new ResourceSharedDistributionPackageBuilder(new FileVaultDistributionPackageBuilder(packaging, distributionEventFactory));
        }
    }


    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionPackageBuildingException {
        return packageBuilder.createPackage(resourceResolver, request);
    }

    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageReadingException {
        return packageBuilder.readPackage(resourceResolver, stream);
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String id) {
        return packageBuilder.getPackage(resourceResolver, id);
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageReadingException {
        return packageBuilder.installPackage(resourceResolver, distributionPackage);
    }
}
