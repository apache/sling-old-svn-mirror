package org.apache.sling.distribution.trigger.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.component.impl.DistributionComponentUtils;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;

import javax.annotation.Nonnull;
import java.util.Map;

@Component(metatype = true,
        label = "Sling Distribution - Generic Local Triggers Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(DistributionTrigger.class)
public class LocalDistributionTriggerFactory implements DistributionTrigger {


    /**
     * remote event trigger type
     */
    public static final String TRIGGER_REMOTE_EVENT = "remoteEvent";

    /**
     * remote event endpoint property
     */
    public static final String TRIGGER_REMOTE_EVENT_PROPERTY_ENDPOINT = "endpoint";

    /**
     * resource event trigger type
     */
    public static final String TRIGGER_RESOURCE_EVENT = "resourceEvent";

    /**
     * resource event path property
     */
    public static final String TRIGGER_RESOURCE_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger type
     */
    public static final String TRIGGER_SCHEDULED_EVENT = "scheduledEvent";

    /**
     * scheduled trigger action property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION = "action";

    /**
     * scheduled trigger path property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH = "path";

    /**
     * scheduled trigger seconds property
     */
    public static final String TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS = "seconds";

    /**
     * chain distribution trigger type
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT = "distributionEvent";

    /**
     * chain distribution path property
     */
    public static final String TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger type
     */
    public static final String TRIGGER_JCR_EVENT = "jcrEvent";

    /**
     * jcr event trigger path property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr event trigger service user property
     */
    public static final String TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger type
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT = "persistedJcrEvent";

    /**
     * jcr persisting event trigger path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH = "path";

    /**
     * jcr persisting event trigger service user property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME = "servicename";

    /**
     * jcr persisting event trigger nuggets path property
     */
    public static final String TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH = "nuggetsPath";



    DistributionTrigger trigger;

    @Reference
    private SlingRepository repository;

    @Reference
    private Scheduler scheduler;


    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> config) {
        String factory = PropertiesUtil.toString(config.get(DistributionComponentUtils.TYPE), null);

        if (TRIGGER_RESOURCE_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_RESOURCE_EVENT_PROPERTY_PATH), null);

            trigger = new ResourceEventDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_SCHEDULED_EVENT.equals(factory)) {
            String action = PropertiesUtil.toString(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_ACTION), DistributionRequestType.PULL.name());
            String path = PropertiesUtil.toString(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_PATH), "/");
            int interval = PropertiesUtil.toInteger(config.get(TRIGGER_SCHEDULED_EVENT_PROPERTY_SECONDS), 30);

            trigger =  new ScheduledDistributionTrigger(action, path, interval, scheduler);
        } else if (TRIGGER_DISTRIBUTION_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_DISTRIBUTION_EVENT_PROPERTY_PATH), null);

            trigger =  new ChainDistributeDistributionTrigger(path, bundleContext);
        } else if (TRIGGER_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(config.get(TRIGGER_JCR_EVENT_PROPERTY_SERVICE_NAME), null);

            trigger =  new JcrEventDistributionTrigger(repository, path, serviceName);
        } else if (TRIGGER_PERSISTED_JCR_EVENT.equals(factory)) {
            String path = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_PATH), null);
            String serviceName = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_SERVICE_NAME), null);
            String nuggetsPath = PropertiesUtil.toString(config.get(TRIGGER_PERSISTED_JCR_EVENT_PROPERTY_NUGGETS_PATH), null);

            trigger =  new PersistingJcrEventDistributionTrigger(repository, path, serviceName, nuggetsPath);
        }
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.register(requestHandler);
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        trigger.unregister(requestHandler);
    }
}
