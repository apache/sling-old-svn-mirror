/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.caconfig.management.impl.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.management.ConfigurationData;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.management.ValueInfo;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web console plugin to test configuration resolution.
 */
@Component(service=Servlet.class,
           property={Constants.SERVICE_DESCRIPTION + "=Apache Sling Context-Aware Configuration Web Console Plugin",
                   WebConsoleConstants.PLUGIN_LABEL + "=" + ConfigurationWebConsolePlugin.LABEL,
                   WebConsoleConstants.PLUGIN_TITLE + "=" + ConfigurationWebConsolePlugin.TITLE,
                   WebConsoleConstants.PLUGIN_CATEGORY + "=Sling"})
@SuppressWarnings("serial")
public class ConfigurationWebConsolePlugin extends AbstractWebConsolePlugin {

    public static final String LABEL = "slingcaconfig";
    public static final String TITLE = "Context-Aware Configuration";
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurationWebConsolePlugin.class);
    
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ResourceResolverFactory resolverFactory;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ConfigurationManager configurationManager;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private XSSAPI xss;

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        final PrintWriter pw = response.getWriter();

        info(pw, "Configurations are managed in the resource tree. Use this tool to test configuration resolutions.");

        pw.println("<br/>");

        printResolutionTestTool(request, pw);
    }

    private String getParameter(final HttpServletRequest request, final String name, final String defaultValue) {
        String value = request.getParameter(name);
        if ( value != null && !value.trim().isEmpty() ) {
            return value.trim();
        }
        return defaultValue;
    }

    private void printResolutionTestTool(HttpServletRequest request, PrintWriter pw) {
        final String path = this.getParameter(request, "path", null);
        String configNameOther = this.getParameter(request, "configNameOther", null);
        String configName = this.getParameter(request, "configName", null);
        if (configName == null) {
            configName = configNameOther;
        }
        else {
            configNameOther = null;
        }
        final boolean resourceCollection = BooleanUtils.toBoolean(this.getParameter(request, "resourceCollection", "false"));

        ResourceResolver resolver = null;
        try {
            Resource contentResource = null;
            if (path != null) {
                resolver = getResolver();
                if (resolver != null) {
                    contentResource = resolver.getResource(path);
                }
            }

            pw.println("<form method='get'>");

            tableStart(pw, "Test Configuration Resolution", 2);
            
            String alertMessage = null;
            if (path != null) {
                if (resolver == null) {
                    alertMessage = "Unable to access repository - please check system configuration.";
                }
                else if (contentResource == null) {
                    alertMessage = "Path does not exist.";
                }
            }
            textField(pw, "Content Path", "path", path, alertMessage);
            
            tableRows(pw);
            selectField(pw, "Config Name", "configName", configName, configurationManager.getConfigurationNames());
            
            tableRows(pw);
            textField(pw, "Other Config Name", "configNameOther", configNameOther);
            
            tableRows(pw);
            checkboxField(pw, "Resource collection", "resourceCollection", resourceCollection);
            
            tableRows(pw);
            pw.println("<td></td>");
            pw.println("<td><input type='submit' value='Resolve'/></td>");
            tableEnd(pw);

            pw.println("</form>");

            pw.println("<br/>");

            if (contentResource != null) {

                // resolve configuration
                Collection<ConfigurationData> configDatas;
                if (resourceCollection) {
                    configDatas = configurationManager.getConfigurationCollection(contentResource, configName).getItems();
                }
                else {
                    ConfigurationData configData = configurationManager.getConfiguration(contentResource, configName);
                    if (configData != null) {
                        configDatas = Collections.singletonList(configData);
                    }
                    else {
                        configDatas = Collections.emptyList();
                    }
                }

                tableStart(pw, "Result", 6);
                
                if (configDatas.size() == 0) {
                    pw.println("<td colspan='6'>");
                    alertDiv(pw, "No matching item found.");
                    pw.println("<br/>&nbsp;</td>");
                }
                else {
                
                    pw.println("<th>Property</th>");
                    pw.println("<th>Effective Value</th>");
                    pw.println("<th>Value</th>");
                    pw.println("<th>Default</th>");
                    pw.println("<th>Inherited</th>");
                    pw.println("<th>Overwritten</th>");

                    for (ConfigurationData data : configDatas) {
                        tableRows(pw);
                        pw.println("<td colspan='6' style='background-color:#f3f3f3'>");
                        pw.print("Path: " + xss.encodeForHTML(data.getResourcePath()));
                        pw.println("</td>");
                        
                        for (String propertyName : data.getPropertyNames()) {
                            ValueInfo<?> valueInfo = data.getValueInfo(propertyName);
                            tableRows(pw);
                            td(pw, propertyName);
                            td(pw, valueInfo.getEffectiveValue());
                            td(pw, valueInfo.getValue());
                            td(pw, valueInfo.isDefault());
                            
                            String title = null;
                            if (valueInfo.isInherited()) {
                                title = "Source path: " + valueInfo.getConfigSourcePath();
                            }
                            td(pw, valueInfo.isInherited(), title);

                            td(pw, valueInfo.isOverridden());
                        }
                        
                   }
                    
                }
                
                tableEnd(pw);
            }

        }
        finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }

    private void info(PrintWriter pw, String text) {
        pw.print("<p class='statline ui-state-highlight'>");
        pw.print(xss.encodeForHTML(text));
        pw.println("</p>");
    }

    private void tableStart(PrintWriter pw, String title, int colspan) {
        pw.println("<table class='nicetable ui-widget'>");
        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.print("<th colspan=");
        pw.print(String.valueOf(colspan));
        pw.print(">");
        pw.print(xss.encodeForHTML(title));
        pw.println("</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");
        pw.println("<tr>");
    }

    private void tableEnd(PrintWriter pw) {
        pw.println("</tr>");
        pw.println("</tbody>");
        pw.println("</table>");
    }

    private void tableRows(PrintWriter pw) {
        pw.println("</tr>");
        pw.println("<tr>");
    }
    
    private void textField(PrintWriter pw, String label, String fieldName, String value, String... alertMessages) {
        pw.print("<td style='width:20%'>");
        pw.print(xss.encodeForHTMLAttr(label));
        pw.println("</td>");
        pw.print("<td><input name='");
        pw.print(xss.encodeForHTMLAttr(fieldName));
        pw.print("' value='");
        pw.print(xss.encodeForHTMLAttr(StringUtils.defaultString(value)));
        pw.print("' style='width:100%'/>");
        for (String alertMessage : alertMessages) {
            alertDiv(pw, alertMessage);
        }
        pw.println("</td>");
    }
    
    private void selectField(PrintWriter pw, String label, String fieldName, String value, Collection<String> options) {
        pw.print("<td style='width:20%'>");
        pw.print(xss.encodeForHTMLAttr(label));
        pw.println("</td>");
        pw.print("<td><select name='");
        pw.print(xss.encodeForHTMLAttr(fieldName));
        pw.print("' style='width:100%'>");
        pw.print("<option value=''>(please select)</option>");
        for (String option : options) {
            pw.print("<option");
            if (StringUtils.equals(option, value)) {
                pw.print(" selected");
            }
            pw.print(">");
            pw.print(xss.encodeForHTMLAttr(option));
            pw.print("</option>");
        }
        pw.print("</select>");
        pw.println("</td>");
    }
    
    private void checkboxField(PrintWriter pw, String label, String fieldName, boolean checked) {
        pw.print("<td style='width:20%'>");
        pw.print(xss.encodeForHTMLAttr(label));
        pw.println("</td>");
        pw.print("<td><input type='checkbox' name='");
        pw.print(xss.encodeForHTMLAttr(fieldName));
        pw.print("' value='true'");
        if (checked) {
            pw.print(" checked");
        }
        pw.print("/></td>");
    }
    
    private void alertDiv(PrintWriter pw, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        pw.println("<div>");
        pw.println("<span class='ui-icon ui-icon-alert' style='float:left'></span>");
        pw.print("<span style='float:left'>");
        pw.print(xss.encodeForHTML(text));
        pw.println("</span>");
        pw.println("</div>");
    }
    
    private void td(PrintWriter pw, Object value, String... title) {
        pw.print("<td");
        if (title.length > 0 && !StringUtils.isBlank(title[0])) {
            pw.print(" title='");
            pw.print(xss.encodeForHTML(title[0]));
            pw.print("'");
        }
        pw.print(">");

        if (value != null) {
            if (value.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(value); i++) {
                    Object itemValue = Array.get(value, i);
                    pw.print(xss.encodeForHTML(ObjectUtils.defaultIfNull(itemValue, "").toString()));
                    pw.println("<br>");
                }
            }
            else {
                pw.print(xss.encodeForHTML(value.toString()));
            }
        }
        
        if (title.length > 0 && !StringUtils.isBlank(title[0])) {
            pw.print("<span class='ui-icon ui-icon-info' style='float:left'></span>");
        }
        pw.print("</td>");
    }

    private ResourceResolver getResolver() {
        try {
            return resolverFactory.getServiceResourceResolver(null);
        }
        catch (final LoginException ex) {
            log.warn("Unable to get resource resolver - please ensure a system user is configured: {}", ex.getMessage());
            return null;
        }
    }

}
