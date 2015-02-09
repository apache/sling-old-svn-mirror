package org.apache.sling.distribution.serialization.impl;



import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.distribution.component.impl.DistributionComponent;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuilderProvider;

import java.util.List;

@Component
@Service(DistributionPackageBuilderProvider.class)
public class DefaultDistributionPackageBuilderProvider implements DistributionPackageBuilderProvider {

    @Reference
    DistributionComponentProvider componentProvider;

    public DistributionPackageBuilder getPackageBuilder(String type) {
        List<DistributionComponent> componentList = componentProvider.getComponents(DistributionComponentKind.PACKAGE_BUILDER);
        DistributionPackageBuilder packageBuilder = filterPackageBuildersByType(componentList, type);

        return packageBuilder;
    }


    private static DistributionPackageBuilder filterPackageBuildersByType(List<DistributionComponent> componentList, String type) {

        if (type == null) {
            return null;
        }

        for (DistributionComponent component : componentList) {
            Object service = component.getService();

            if (service instanceof DistributionPackageBuilder) {
                DistributionPackageBuilder packageBuilder = (DistributionPackageBuilder) service;

                if (type.equals(packageBuilder.getType())) {
                    return packageBuilder;
                }
            }
        }

        return null;
    }
}
