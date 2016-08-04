/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;
import org.apache.sling.ide.util.PathUtil;

public class VltNodeTypeFactory {

    private Map<String,VltNodeType> nodeTypes = new HashMap<>();
    
    Map<String, VltNodeType> getNodeTypes() {
        return nodeTypes;
    }

    public VltNodeType getNodeType(String name) {
        return nodeTypes.get(name);
    }

    void init(VltRepository repository) throws RepositoryException {
        Result<ResourceProxy> jcrSystem = repository.newListTreeNodeCommand("/jcr:system/jcr:nodeTypes", 3).execute();
        // phase 1: create all node types
        for (ResourceProxy child : jcrSystem.get().getChildren()) {
            
            VltNodeType nt = createNodeType(child);
            nodeTypes.put(nt.getName(), nt);
        }
        // phase 2: init declared fields
        for (VltNodeType nt : nodeTypes.values()) {
            initDeclaredFields(nt);
        }
        
        // at this stage all declared fields are initialized, we can now calculate
        // the rest
        
        // phase 2: initialize the dependency tree (eg superTypes)
        // - this is a separate phase to make sure, all VltNodeType objects have been created (above)
        //   hence initTypeDependencyTree can assume all nodetypes exist
        for (VltNodeType nt : nodeTypes.values()) {
            initTypeDependencyTree(nt);
        }

        // phase 3: init property definitions
        for (VltNodeType nt : nodeTypes.values()) {
            final ResourceProxy child = nt.getResourceProxy();
            initPropertyDefinitions(nt);
            initProperties(nt, child);
        }
        
        // phase 4: initialize the allowed primary childnodetypes
        for (VltNodeType nt : nodeTypes.values()) {
            initAllowedPrimaryChildNodeTypes(nt);
        }
    }
    
    private void initDeclaredFields(VltNodeType nt) {
        final ResourceProxy child = nt.getResourceProxy();
        String[] superTypeNamess = (String[]) child.getProperties()
                .get("jcr:supertypes");
        nt.setDeclaredSupertypeNames(superTypeNamess);
        if (superTypeNamess!=null) {
            NodeType[] superTypes = new NodeType[superTypeNamess.length];
            for (int i = 0; i < superTypeNamess.length; i++) {
                superTypes[i] = getNodeType(superTypeNamess[i]);
            }
            nt.setDeclaredSupertypes(superTypes);
        }

        Set<VltNodeDefinition> nds = new HashSet<>();
        for (ResourceProxy ntChild : child.getChildren()) {
            String ntChildName = PathUtil.getName(ntChild.getPath());
            if (ntChildName.startsWith("jcr:childNodeDefinition")) {
                VltNodeDefinition nd = handleChildNodeDefinition(ntChild);
                nds.add(nd);
            } else if (ntChildName.startsWith("rep:residualChildNodeDefinitions")) {
                // go through children
                for (ResourceProxy residualChild : ntChild.getChildren()) {
                    nds.add(handleChildNodeDefinition(residualChild));
                }
            }
      }
        nt.setDeclaredChildNodeDefinitions(nds.toArray(new NodeDefinition[0]));
        initDeclaredPropertyDefinitions(nt, child);
    }

    private VltNodeType createNodeType(ResourceProxy child) {
        final VltNodeType nt = new VltNodeType(child);
        final String name = PathUtil.getName(child.getPath());
        nt.setName(name);
        return nt;
    }
    
    private void initDeclaredPropertyDefinitions(VltNodeType nt, ResourceProxy child) {
        Map<String,VltPropertyDefinition> pds = new HashMap<>();
        
        // load propertyDefinition children
        for (ResourceProxy aChild : child.getChildren()) {
            String childName = PathUtil.getName(aChild.getPath());
            if (childName.startsWith("jcr:propertyDefinition")) {
                String jcrName = (String)aChild.getProperties().get("jcr:name");
                if (jcrName!=null) {
                    VltPropertyDefinition pd = new VltPropertyDefinition();
                    pd.setName(jcrName);
                    Boolean autoCreated = (Boolean)aChild.getProperties().get("jcr:autoCreated");
                    if (autoCreated!=null) {
                        pd.setAutoCreated(autoCreated);
                    }
                    Boolean multiple = (Boolean)aChild.getProperties().get("jcr:multiple");
                    if (multiple!=null) {
                        pd.setMultiple(multiple);
                    }
                    Boolean mandatory = (Boolean)aChild.getProperties().get("jcr:mandatory");
                    if (mandatory!=null) {
                        pd.setMandatory(mandatory);
                    }
                    Boolean isProtected = (Boolean)aChild.getProperties().get("jcr:protected");
                    if (isProtected!=null) {
                        pd.setProtected(isProtected);
                    }
                    final Object object = aChild.getProperties().get("jcr:requiredType");
                    if (object!=null) {
                        String requiredType = (String)object;
                        if (requiredType!=null) {
                            pd.setRequiredType(propertyTypeFromName(requiredType));
                        }
                    }
                    pds.put(jcrName, pd);
                }
            }
        }
        
        // load mandatory
        String[] mandatoryProperties = (String[]) child.getProperties().get("rep:mandatoryProperties");
        if (mandatoryProperties!=null) {
            for (int i = 0; i < mandatoryProperties.length; i++) {
                String aMandatoryProperty = mandatoryProperties[i];
                VltPropertyDefinition vpd = pds.get(aMandatoryProperty);
                if (vpd==null) {
                    vpd = new VltPropertyDefinition();
                    vpd.setName(aMandatoryProperty);
                    pds.put(aMandatoryProperty, vpd);
                }
                vpd.setMandatory(true);
            }
        }
        
        // load protected
        String[] protectedProperties = (String[]) child.getProperties().get("rep:protectedProperties");
        if (protectedProperties!=null) {
            for (int i = 0; i < protectedProperties.length; i++) {
                String aProtectedProperties = protectedProperties[i];
                VltPropertyDefinition vpd = pds.get(aProtectedProperties);
                if (vpd==null) {
                    vpd = new VltPropertyDefinition();
                    vpd.setName(aProtectedProperties);
                    pds.put(aProtectedProperties, vpd);
                }
                vpd.setProtected(true);
            }
        }
        
        nt.setDeclaredPropertyDefinitions(pds.values().toArray(new VltPropertyDefinition[pds.size()]));
    }
    
    private int propertyTypeFromName(String name) {
        if (name.equalsIgnoreCase(PropertyType.TYPENAME_STRING)) {
            return PropertyType.STRING;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_BINARY)) {
            return PropertyType.BINARY;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_BOOLEAN)) {
            return PropertyType.BOOLEAN;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_LONG)) {
            return PropertyType.LONG;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_DOUBLE)) {
            return PropertyType.DOUBLE;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_DECIMAL)) {
            return PropertyType.DECIMAL;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_DATE)) {
            return PropertyType.DATE;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_NAME)) {
            return PropertyType.NAME;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_PATH)) {
            return PropertyType.PATH;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_REFERENCE)) {
            return PropertyType.REFERENCE;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_WEAKREFERENCE)) {
            return PropertyType.WEAKREFERENCE;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_URI)) {
            return PropertyType.URI;
        } else if (name.equalsIgnoreCase(PropertyType.TYPENAME_UNDEFINED)) {
            return PropertyType.UNDEFINED;
        } else {
            throw new IllegalArgumentException("unknown type: " + name);
        }
    }

    private void initPropertyDefinitions(VltNodeType nt) {
        Map<String,VltPropertyDefinition> pds = new HashMap<>();
        
        PropertyDefinition[] declaredPds = nt.getDeclaredPropertyDefinitions();
        if (declaredPds!=null) {
            for (int i = 0; i < declaredPds.length; i++) {
                PropertyDefinition pd = declaredPds[i];
                pds.put(pd.getName(), (VltPropertyDefinition) pd);
            }
        }
        
        NodeType[] superTypes = nt.getSupertypes();
        if (superTypes!=null) {
            for (int i = 0; i < superTypes.length; i++) {
                VltNodeType aSuperType = (VltNodeType) superTypes[i];
                
                VltPropertyDefinition[] superTypePds = (VltPropertyDefinition[])aSuperType.getPropertyDefinitions();
                for (int j = 0; j < superTypePds.length; j++) {
                    VltPropertyDefinition pd = superTypePds[j];
                    VltPropertyDefinition existingPd = pds.get(pd.getName());
                    if (existingPd!=null) {
                        // merge the two
                        existingPd.mergeFrom(pd);
                    } else {
                        pds.put(pd.getName(), pd);
                    }
                }
            }
        }
        nt.setPropertyDefinitions(pds.values().toArray(new VltPropertyDefinition[pds.size()]));
    }

    private void initProperties(VltNodeType nt, ResourceProxy child) {
        Boolean isMixin = (Boolean) child.getProperties().get("jcr:isMixin");
        if (isMixin==null) {
            nt.setMixin(false);
        } else {
            nt.setMixin(isMixin);
        }
    }
    
    private VltNodeDefinition handleChildNodeDefinition(ResourceProxy child) {
        
        VltNodeDefinition nd = new VltNodeDefinition();
        nd.setName((String) child.getProperties().get("jcr:name"));
        String[] requiredPrimaryTypes = (String[]) child.getProperties().get(
                "jcr:requiredPrimaryTypes");
        nd.setRequiredPrimaryTypeNames(requiredPrimaryTypes);
        if (requiredPrimaryTypes != null && requiredPrimaryTypes.length!=0) {
            NodeType[] nts = new NodeType[requiredPrimaryTypes.length];
            for (int i = 0; i < requiredPrimaryTypes.length; i++) {
                String pt = requiredPrimaryTypes[i];
                VltNodeType nt = getNodeType(pt);
                nts[i] = nt;
            }
            nd.setRequiredPrimaryTypes(nts);
        }
        
//        String[] defaultPrimaryTypes = (String[])
//        child.getProperties().get("jcr:defaultPrimaryTypes");
        
        return nd;
    }

    private void initTypeDependencyTree(VltNodeType nt) {
        final String[] superTypeNames = nt.getDeclaredSupertypeNames();
        if (superTypeNames == null) {
            return;
        }
        // collect all the supertype names
        Set<NodeType> allSuperTypes = new HashSet<>();
        initSuperTypes(allSuperTypes, nt);
        nt.setSupertypes(allSuperTypes.toArray(new NodeType[0]));
    }

    private void initSuperTypes(Set<NodeType> allSuperTypes, VltNodeType nt) {
        final NodeType[] declaredSupertypes = nt.getDeclaredSupertypes();
        if (declaredSupertypes==null) {
            return;
        }
        for (int i = 0; i < declaredSupertypes.length; i++) {
            VltNodeType superType = (VltNodeType) declaredSupertypes[i];
            allSuperTypes.add(superType);
            nt.addSuperType(superType);
            initSuperTypes(allSuperTypes, (VltNodeType) superType);
        }
    }

    private void initAllowedPrimaryChildNodeTypes(VltNodeType nt0) throws RepositoryException {
        NodeDefinition[] declaredCihldNodeDefinitions = nt0.getDeclaredChildNodeDefinitions();
        Set<String> allowedChildNodeTypes = new HashSet<>();
        if (declaredCihldNodeDefinitions!=null) {
            for (int i = 0; i < declaredCihldNodeDefinitions.length; i++) {
                NodeDefinition nodeDefinition = declaredCihldNodeDefinitions[i];
                NodeType[] requiredPrimaryTypes = nodeDefinition.getRequiredPrimaryTypes();
                if (requiredPrimaryTypes!=null) {
                    for (int j = 0; j < requiredPrimaryTypes.length; j++) {
                        VltNodeType aRequiredPrimaryType = (VltNodeType) requiredPrimaryTypes[j];
                        if (aRequiredPrimaryType==null) {
                            System.out.println("this can not be");
                        }
                        Set<VltNodeType> allKnownChildTypes = aRequiredPrimaryType.getAllKnownChildTypes();
                        for (Iterator<VltNodeType> it = allKnownChildTypes.iterator(); it.hasNext();) {
                            VltNodeType aChildType = it.next();
                            VltNodeType nt2 = getNodeType(aChildType.getName());
                            if (!nt2.isMixin()) {
                                // mixins cannot be primary node types!
                                allowedChildNodeTypes.add(nt2.getName());
                            }
                        }
                    }
                }
            }
        }
        nt0.setAllowedPrimaryChildNodeTypes(allowedChildNodeTypes);
    }


}
