package org.apache.sling.distribution.serialization.impl.vlt;


import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuildingException;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;
import org.apache.sling.distribution.serialization.impl.AbstractDistributionPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.InputStream;
import java.util.UUID;
/**
 * a {@link org.apache.sling.distribution.serialization.DistributionPackageBuilder} based on Apache Jackrabbit FileVault.
 * <p/>
 * Each {@link org.apache.sling.distribution.packaging.DistributionPackage} created by {@link JcrVaultDistributionPackageBuilder} is
 * backed by a {@link org.apache.jackrabbit.vault.packaging.JcrPackage}. 
 */
public class JcrVaultDistributionPackageBuilder  extends AbstractDistributionPackageBuilder implements
        DistributionPackageBuilder {
    private final Logger log = LoggerFactory.getLogger(getClass());


    private static final String VERSION = "0.0.1";
    private static final String PACKAGE_GROUP = "sling/distribution";

    public static final String PACKAGING_TYPE = "jcrvlt";
    private final Packaging packaging;
    private ImportMode importMode;
    private AccessControlHandling aclHandling;

    public JcrVaultDistributionPackageBuilder(Packaging packaging, ImportMode importMode, AccessControlHandling aclHandling) {
        super(PACKAGING_TYPE);

        this.packaging = packaging;

        this.importMode = importMode;
        this.aclHandling = aclHandling;
    }

    @Override
    protected DistributionPackage createPackageForAdd(ResourceResolver resourceResolver, DistributionRequest request) throws DistributionPackageBuildingException {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            JcrPackageManager packageManager = packaging.getPackageManager(session);

            final String[] paths = request.getPaths();

            String packageGroup = PACKAGE_GROUP;
            String packageName = PACKAGING_TYPE + "_" + System.currentTimeMillis() + "_" +  UUID.randomUUID();


            WorkspaceFilter filter = VltUtils.createFilter(request);

            final JcrPackage jcrPackage = packageManager.create(packageGroup, packageName, VERSION);
            final JcrPackageDefinition jcrPackageDefinition = jcrPackage.getDefinition();


            jcrPackageDefinition.setFilter(filter, true);

            log.debug("assembling package {}", packageGroup + '/' + packageName + "-" + VERSION);
            packageManager.assemble(jcrPackage, null);
            return new JcrVaultDistributionPackage(PACKAGING_TYPE, jcrPackage, session);
        } catch (Exception e) {
            throw new DistributionPackageBuildingException(e);
        } finally {
            ungetSession(session);
        }
    }

    @Override
    protected DistributionPackage readPackageInternal(ResourceResolver resourceResolver, InputStream stream) throws DistributionPackageReadingException {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            JcrPackageManager packageManager = packaging.getPackageManager(session);

            JcrPackage jcrPackage = packageManager.upload(stream, true);

            return new JcrVaultDistributionPackage(PACKAGING_TYPE, jcrPackage, session);
        } catch (Exception e) {
            throw new DistributionPackageReadingException(e);
        } finally {
            ungetSession(session);
        }
    }

    @Override
    protected boolean installPackageInternal(ResourceResolver resourceResolver, DistributionPackage distributionPackage) throws DistributionPackageReadingException {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            JcrPackageManager packageManager = packaging.getPackageManager(session);



            String packageName = distributionPackage.getId();
            JcrPackage jcrPackage = packageManager.open(new PackageId(PACKAGE_GROUP, packageName, VERSION));

            ImportOptions importOptions = VltUtils.getImportOptions(aclHandling, importMode);
            jcrPackage.extract(importOptions);

            return true;
        } catch (Exception e) {
            throw new DistributionPackageReadingException(e);
        } finally {
            ungetSession(session);
        }
    }

    @Override
    protected DistributionPackage getPackageInternal(ResourceResolver resourceResolver, String id) {
        Session session = null;
        try {
            session = getSession(resourceResolver);
            JcrPackageManager packageManager = packaging.getPackageManager(session);

            String packageName = id;
            JcrPackage jcrPackage = packageManager.open(new PackageId(PACKAGE_GROUP, packageName, VERSION));

            if (jcrPackage == null) {
                return null;
            }
            return new JcrVaultDistributionPackage(PACKAGING_TYPE, jcrPackage, session);
        } catch (Exception e) {
            return null;
        } finally {
            ungetSession(session);
        }
    }
}
