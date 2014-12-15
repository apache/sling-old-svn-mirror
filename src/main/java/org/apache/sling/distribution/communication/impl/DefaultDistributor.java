package org.apache.sling.distribution.communication.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestState;
import org.apache.sling.distribution.communication.DistributionResponse;
import org.apache.sling.distribution.communication.Distributor;
import org.apache.sling.distribution.component.impl.DefaultDistributionComponentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Default implementation of Distributor interface that dispatches the request to available agents.
 */
@Component
@Service(Distributor.class)
public class DefaultDistributor implements Distributor {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Reference
    DefaultDistributionComponentProvider componentProvider;

    public DistributionResponse distribute(@Nonnull String agentName, @Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) {
       DistributionAgent agent = componentProvider.getComponent(DistributionAgent.class, agentName);

        if (agentName == null) {
            return new DistributionResponse(DistributionRequestState.FAILED, "Agent is not available");
        }

        try {
            return agent.execute(resourceResolver, distributionRequest);
        } catch (DistributionAgentException e) {
            log.error("cannot execute", e);
            return new DistributionResponse(DistributionRequestState.FAILED, "Cannot execute request");
        }
    }
}
