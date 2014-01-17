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
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.apache.sling.replication.agent.impl.ReplicationAgentConfigurationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to :
 * - create {@link org.apache.sling.replication.agent.ReplicationAgent}s (via HTTP POST).
 * - retrieve the list of existing {@link org.apache.sling.replication.agent.ReplicationAgent}s (via HTTP GET)
 */
@SuppressWarnings("serial")
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
        @Property(name = "sling.servlet.resourceTypes",
                value = ReplicationAgentConfigurationResource.RESOURCE_ROOT_TYPE),
        @Property(name = "sling.servlet.methods", value = { "POST", "GET" } )})
public class ReplicationConfigurationRootServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ReplicationAgentConfigurationManager agentConfigurationManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String agentName = request.getParameter("name");

        try {
            agentConfigurationManager.createAgentConfiguration(agentName, request.getParameterMap());
            log.info("agent configuration for {} created", agentName);

            ReplicationAgentConfiguration configuration = agentConfigurationManager.getConfiguration(agentName);

            response.getWriter().write(configuration.toString());
            response.setStatus(201);

        } catch (AgentConfigurationException e) {
            log.error("cannot create agent {}", agentName, e);

            response.setStatus(404);
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        try {
            ReplicationAgentConfiguration[] agentConfigurations = agentConfigurationManager.listAllAgentConfigurations();

            response.getWriter().write(toJson(agentConfigurations));
            response.setStatus(200);

        } catch (AgentConfigurationException e) {
            log.error("cannot retrieve agent configurations", e);

            response.setStatus(404);
        }
    }


    String toJson(ReplicationAgentConfiguration[] agentConfigurations){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"agents\": [");

        for(int i=0; i < agentConfigurations.length; i++){
            ReplicationAgentConfiguration configuration = agentConfigurations[i];
            sb.append(configuration.toSimpleString());
            if(i < agentConfigurations.length -1)
                sb.append(",");

        }

        sb.append("]");

        sb.append("}");

        return sb.toString();
    }
}
