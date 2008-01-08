package org.apache.sling.usling.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/** usling main servlet
 * 
 * @scr.component immediate="true" 
 *      label="%UslingMainServlet.name"
 *      description="%UslingMainServlet.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="usling main servlet"
 * 
 */
public class UslingMainServlet extends GenericServlet {

    private static final long serialVersionUID = 6895263078698789849L;

    /**
     *  HttpService, provided by OSGi, with which we register
     *  this servlet.
     *   
     * @scr.reference
     */
    private HttpService httpService;
    
    /**
     * OSGi LogService
     * @scr.reference
     */
    private LogService logService;
    
    /**
     * @scr.property value="/"
     */
    private static final String PROP_SERVLET_MOUNT_POINT = "servlet.mount.point";
    private String mountPoint;

    /** Process a servlet request */
    public void service(ServletRequest sReq, ServletResponse sResp)
            throws ServletException, IOException {

        final HttpServletRequest req = (HttpServletRequest) sReq;
        final HttpServletResponse resp = (HttpServletResponse) sResp;
        
        resp.setContentType("text/plain");
        String msg = getClass().getName() + " - this Servlet does nothing useful yet";
        msg += " - it is now " + new Date();
        resp.getOutputStream().write(msg.getBytes());
        resp.getOutputStream().flush();
    }
    
    /** Called by OSGi to activate this service */
    protected void activate(ComponentContext context) throws Exception {
        final Dictionary<String, Object> config = context.getProperties();
        mountPoint = getProperty(config, PROP_SERVLET_MOUNT_POINT, "/");
        logService.log(LogService.LOG_INFO, "Registering " + getClass().getSimpleName() + " on mount point " + mountPoint);
        httpService.registerServlet(mountPoint, this, config, null);
    }
    
    /** Called by OSGi to deactivate this service */
    protected void deactivate(ComponentContext context) throws Exception {
        logService.log(LogService.LOG_INFO, "Unregistering " + getClass().getSimpleName() + " from mount point " + mountPoint);
        httpService.unregister(mountPoint);
    }
    
    /**
     * Returns the named property from the given configuration, or
     * def if the value does not exist.
     */
    private String getProperty(Dictionary<String, Object> config, String name, String def) {
        final Object value = config.get(name);
        if (value instanceof String) {
            return (String) value;
        }

        if (value == null) {
            return def;
        }

        return String.valueOf(value);
    }
}
