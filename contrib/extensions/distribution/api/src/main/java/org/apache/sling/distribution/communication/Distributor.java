package org.apache.sling.distribution.communication;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.agent.DistributionAgentException;

import javax.annotation.Nonnull;

@ProviderType
public interface Distributor {
    /**
     * Perform a {@link org.apache.sling.distribution.communication.DistributionRequest} to distribute content from a source
     * instance to a target instance.
     * The content to be sent will be assembled according to the information contained in the request.
     * A {@link org.apache.sling.distribution.communication.DistributionResponse} holding the {@link org.apache.sling.distribution.communication.DistributionRequestState}
     * of the provided request will be returned.
     * Synchronous {@link org.apache.sling.distribution.agent.DistributionAgent}s will usually block until the execution has finished
     * while asynchronous agents will usually return the response as soon as the content to be distributed has been assembled
     * and scheduled for distribution.
     *
     * @param agentName the name of the agent used to distribute the request
     * @param distributionRequest the distribution request
     * @param resourceResolver    the resource resolver used for authorizing the request,
     * @return a {@link org.apache.sling.distribution.communication.DistributionResponse}
     */
    DistributionResponse distribute(@Nonnull String agentName, @Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest);


}
