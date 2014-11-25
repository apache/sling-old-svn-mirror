package org.apache.sling.distribution.component.impl;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.resources.impl.OsgiUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Service(DistributionComponentManager.class)
public class DefaultDistributionComponentManager implements DistributionComponentManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    DistributionComponentUtils componentUtils = new DistributionComponentUtils();


    @Reference
    ConfigurationAdmin configurationAdmin;

    Map<String, List<String>> managedConfigPids = new ConcurrentHashMap<String, List<String>>();

    @Activate
    void activate() {
    }

    public void createComponent(@Nonnull Class type, @Nonnull String componentName, @Nonnull Map<String, Object> properties) {

        String kind = componentUtils.getKind(type);

        if (kind != null) {
            properties.put(DistributionComponentUtils.KIND, kind);
            Map<String, Map<String, Object>> osgiConfigs = componentUtils.transformToOsgi(properties);

            List<String> createdPids = new ArrayList<String>();
            for(Map.Entry<String, Map<String, Object>> entry : osgiConfigs.entrySet()) {
                String factoryPid = componentUtils.getFactoryPid(entry.getKey());
                if (factoryPid != null) {
                    String configPid = createOsgiConfig(factoryPid, entry.getValue());
                    createdPids.add(configPid);
                }
            }

            String componentFullName = getComponentFullName(type, componentName);

            managedConfigPids.put(componentFullName, createdPids);
        }
    }



    private String createOsgiConfig(String factoryPid, Map<String, Object> properties) {
        try {
            String configName = PropertiesUtil.toString(properties.get(DistributionComponentUtils.NAME), null);
            Configuration[] configurations = getConfigurations(factoryPid, configName);
            Configuration configuration = null;
            if (configurations == null || configurations.length == 0) {
                configuration = configurationAdmin.createFactoryConfiguration(factoryPid);
            }
            else {
                configuration = configurations[0];

            }

            properties = OsgiUtils.sanitize(properties);

            configuration.update(OsgiUtils.toDictionary(properties));

            return configuration.getPid();
        } catch (IOException e) {

        }

        return null;
    }


    Configuration[] getConfigurations(String factoryPid, String configName) {
        try {
            String filter = OsgiUtils.getFilter(factoryPid, DistributionComponentUtils.NAME, configName);

            return configurationAdmin.listConfigurations(filter);
        } catch (IOException e) {
            return null;
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }


    public void deleteComponent(@Nonnull Class type, String componentName) {
        String componentFullName = getComponentFullName(type, componentName);
        if (managedConfigPids.containsKey(componentFullName)) {
            List<String> createdPids = managedConfigPids.get(componentFullName);

            for (String createdPid : createdPids) {
                try {
                    Configuration configuration = configurationAdmin.getConfiguration(createdPid);
                    configuration.delete();
                }
                catch (IOException ex) {
                    log.error("Cannot delete config {} for {}", new Object[] { createdPid, componentFullName, ex } );

                }

            }
        }
    }

    private String getComponentFullName(Class type, String componentName) {
        String typeName = type.getSimpleName();
        return typeName + DistributionComponentUtils.TARGET_DESCRIPTOR_SEPARATOR + componentName;
    }
}
