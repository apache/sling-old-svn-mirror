package org.apache.sling.replication.servlet;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to create {@link org.apache.sling.replication.agent.ReplicationAgent}s (via HTTP PUT).
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.paths", value = "/system/replication/agent"),
        @Property(name = "sling.servlet.methods", value = "POST")})
public class ReplicationAgentCreateServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationAgentConfigurationManager agentConfigurationManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String name = request.getParameter("name");

        try {
            agentConfigurationManager.createAgentConfiguration(request.getParameterMap());
            if (log.isInfoEnabled()) {
                log.info("agent configuration for {} created", name);
            }

        } catch (AgentConfigurationException e) {
            if (log.isErrorEnabled()) {
                log.error("cannot create agent {}", name, e);
            }
        }
        Resource agentResource = request.getResource().getChild(name);
        if (agentResource != null) {
            Resource resource = agentResource.getChild("configuration");
            ReplicationAgentConfiguration configuration = resource
                    .adaptTo(ReplicationAgentConfiguration.class);
            response.getWriter().write(configuration.toString());
        } else {
            response.setStatus(404);
            response.getWriter().write("the configuration was correctly created but the related agent cannot be found");
        }
    }
}
