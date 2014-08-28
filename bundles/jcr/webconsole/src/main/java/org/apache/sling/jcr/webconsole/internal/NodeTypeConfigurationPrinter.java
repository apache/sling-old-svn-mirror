/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.webconsole.internal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.ModeAwareConfigurationPrinter;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * A Felix WebConsole ConfigurationPrinter which outputs the current JCR
 * nodetypes.
 */
@Component
@Service(ConfigurationPrinter.class)
@Properties({
    @Property(name = "service.description", value = "JCR Nodetype Configuration Printer"),
    @Property(name = "service.vendor", value = "The Apache Software Foundation"),
    @Property(name = "felix.webconsole.configprinter.web.unescaped", boolValue = true)
})
public class NodeTypeConfigurationPrinter implements ModeAwareConfigurationPrinter {

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile SlingRepository slingRepository;

    /**
     * Get the title of the configuration status page.
     *
     * @return the title
     */
    public String getTitle() {
        return "JCR NodeTypes";
    }

    /**
     * {@inheritDoc}
     */
    public void printConfiguration(PrintWriter pw, String mode) {
        if (slingRepository != null) {
            Session session = null;
            try {
                session = slingRepository.loginAdministrative(null);
                NodeTypeManager ntm = session.getWorkspace().getNodeTypeManager();
                NodeTypeIterator it = ntm.getAllNodeTypes();
                List<NodeType> sortedTypes = sortTypes(it);

                for (NodeType nt : sortedTypes) {
                    pw.printf("[%s]", nt.getName());

                    printSuperTypes(pw, nt);

                    if (nt.hasOrderableChildNodes()) {
                        pw.print(" orderable");
                    }
                    if (nt.isMixin()) {
                        pw.print(" mixin");
                    }
                    linebreak(pw, mode);

                    for (PropertyDefinition prop : nt.getPropertyDefinitions()) {
                        if (prop.getDeclaringNodeType() == nt) {
                            startBold(pw, mode);
                        }

                        pw.printf("- %s", prop.getName());
                        printDefaultValues(pw, prop);
                        if (prop.getName().equals(nt.getPrimaryItemName())) {
                            pw.print(" primary");
                        }
                        if (prop.isMandatory()) {
                            pw.print(" mandatory");
                        }
                        if (prop.isAutoCreated()) {
                            pw.print(" autocreated");
                        }
                        if (prop.isProtected()) {
                            pw.print(" protected");
                        }
                        if (prop.isMultiple()) {
                            pw.print(" multiple");
                        }
                        pw.printf(" %s", OnParentVersionAction.nameFromValue(prop.getOnParentVersion()));
                        printConstraints(pw, prop);

                        if (prop.getDeclaringNodeType() == nt) {
                            stopBold(pw, mode);
                        }

                        linebreak(pw, mode);
                    }
                    for (NodeDefinition child : nt.getChildNodeDefinitions()) {
                        if (child.getDeclaringNodeType() == nt) {
                            startBold(pw, mode);
                        }

                        pw.printf("+ %s", child.getName());

                        printRequiredChildTypes(pw, child);

                        if (child.getDefaultPrimaryType() != null) {
                            pw.printf(" = %s", child.getDefaultPrimaryType().getName());
                        }

                        if (child.isMandatory()) {
                            pw.print(" mandatory");
                        }
                        if (child.isAutoCreated()) {
                            pw.print(" autocreated");
                        }
                        if (child.isProtected()) {
                            pw.print(" protected");
                        }
                        if (child.allowsSameNameSiblings()) {
                            pw.print(" multiple");
                        }
                        pw.printf(" %s", OnParentVersionAction.nameFromValue(child.getOnParentVersion()));

                        if (child.getDeclaringNodeType() == nt) {
                            stopBold(pw, mode);
                        }

                        linebreak(pw, mode);
                    }

                    linebreak(pw, mode);

                }
            } catch (RepositoryException e) {
                pw.println("Unable to output namespace mappings.");
                e.printStackTrace(pw);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        } else {
            pw.println("SlingRepository is not available.");
        }
    }

    /**
     * Output a list of node types from the NamespaceRegistry.
     *
     * @param pw a PrintWriter
     */
    public void printConfiguration(PrintWriter pw) {
        printConfiguration(pw, ConfigurationPrinter.MODE_TXT);

    }

    private List<NodeType> sortTypes(NodeTypeIterator it) {
        List<NodeType> types = new ArrayList<NodeType>();
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            types.add(nt);
        }
        Collections.sort(types, new Comparator<NodeType>(){
            public int compare(NodeType o1, NodeType o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return types;
    }

    private void linebreak(PrintWriter pw, String mode) {
        if (ConfigurationPrinter.MODE_WEB.equals(mode)) {
            pw.println("<br/>");
        } else {
            pw.println();
        }
    }

    private void stopBold(PrintWriter pw, String mode) {
        if (ConfigurationPrinter.MODE_WEB.equals(mode)) {
            pw.print("</b>");
        }
    }

    private void startBold(PrintWriter pw, String mode) {
        if (ConfigurationPrinter.MODE_WEB.equals(mode)) {
            pw.print("<b>");
        }
    }

    private void printRequiredChildTypes(PrintWriter pw, NodeDefinition child) {
        if (child.getRequiredPrimaryTypes() != null && child.getRequiredPrimaryTypes().length > 0) {
            pw.print(" (");

            boolean first = true;
            for (NodeType required : child.getRequiredPrimaryTypes()) {
                if (!first) {
                    pw.print(", ");
                }
                pw.print(required.getName());
                first = false;
            }
            pw.print(")");
        }

    }

    private void printDefaultValues(PrintWriter pw, PropertyDefinition prop) throws RepositoryException {
        if (prop.getDefaultValues() != null && prop.getDefaultValues().length > 0) {
            pw.print(" = ");

            boolean first = true;
            for (Value v : prop.getDefaultValues()) {
                if (!first) {
                    pw.print(", ");
                }
                pw.print(v.getString());
                first = false;
            }
        }
    }

    private void printConstraints(PrintWriter pw, PropertyDefinition prop) throws RepositoryException {
        if (prop.getValueConstraints() != null && prop.getValueConstraints().length > 0) {
            pw.print(" < ");
            boolean first = true;
            for (String s : prop.getValueConstraints()) {
                if (!first) {
                    pw.print(", ");
                }
                pw.print(s);
                first = false;
            }
        }
    }

    private void printSuperTypes(PrintWriter pw, NodeType nt) {
        pw.print(" > ");
        boolean first = true;
        for (NodeType st : nt.getSupertypes()) {
            if (!first) {
                pw.print(", ");
            }
            pw.print(st.getName());
            first = false;
        }
    }
}