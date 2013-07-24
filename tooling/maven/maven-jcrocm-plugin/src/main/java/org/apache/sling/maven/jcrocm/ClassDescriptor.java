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
package org.apache.sling.maven.jcrocm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.plugin.logging.Log;

import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;

/**
 * The <code>ClassDescriptor</code> class provides support for the
 * class-descriptor element of a class mapping, which has the following
 * attribute list definition:
 * 
 * <pre>
 *  &lt;!ATTLIST class-descriptor
 *      className CDATA #REQUIRED
 *      jcrType CDATA #IMPLIED
 *      jcrSuperTypes CDATA #IMPLIED
 *      jcrMixinTypes CDATA #IMPLIED
 *      extend CDATA #IMPLIED
 *      abstract (true|false) &quot;false&quot;
 *      interface (true|false) &quot;false&quot;
 *      discriminator (true|false) &quot;true&quot;
 *  &gt;
 *  &lt;!ATTLIST implement-descriptor
 *      interfaceName CDATA #REQUIRED
 *  &gt;
 * </pre>
 * 
 * <p>
 * Additionally, as can be seen from above, the
 * <code>implement-descriptor</code> is also supported by this class. It
 * retrieves the list of interfaces from the described class.
 * </p>
 */
public class ClassDescriptor extends AbstractDescriptorElement {

    public static final String TAG_CLASS_DESCRIPTOR = "ocm.mapped";

    public static final String ELEMENT_CLASS_DESCRIPTOR = "class-descriptor";

    public static final String CLASS_NAME = "className";

    public static final String JCR_TYPE = "jcrType";

    public static final String JCR_SUPER_TYPES = "jcrSuperTypes";

    public static final String JCR_MIXIN_TYPES = "jcrMixinTypes";

    public static final String EXTEND = "extend";
    
    public static final String INTERFACES = "interfaces";

    public static final String ABSTRACT = "abstract";

    public static final String INTERFACE = "interface";

    public static final String DISCRIMINATOR = "discriminator";

    public static final String ELEMENT_IMPLEMENT_DESCRIPTOR = "implement-descriptor";

    public static final String INTERFACE_NAME = "interfaceName";

    private String className;

    private String jcrType;

    private String jcrSuperTypes;

    private String jcrMixinTypes;

    private boolean discriminator = true;

    private String extend;

    private boolean isAbstract;

    private boolean isInterface;

    private Set interfaces;

    private List children;

    static ClassDescriptor fromClass(Log log, JavaClass javaClass) {

        DocletTag tag = javaClass.getTagByName(TAG_CLASS_DESCRIPTOR);
        if (tag == null) {
            return null;
        }

        ClassDescriptor cd = new ClassDescriptor(log, tag);
        cd.className = javaClass.getFullyQualifiedName();
        cd.isInterface = javaClass.isInterface();
        cd.isAbstract = !cd.isInterface && javaClass.isAbstract();

        cd.extend = tag.getNamedParameter(EXTEND);
        if (cd.extend == null) {
            if (javaClass.getSuperJavaClass() != null) {
                cd.extend = javaClass.getSuperJavaClass().getFullyQualifiedName();

                // do not declare extending Object :-)
                if (Object.class.getName().equals(cd.extend)) {
                    cd.extend = null;
                }
            }
        } else if (cd.extend.length() == 0) {
            // explicit empty extend value prevents extend attribute from being
            // set
            cd.extend = null;
        }

        String interfaceList = tag.getNamedParameter(INTERFACES);
        if (interfaceList == null) {
            if (javaClass.getImplementedInterfaces() != null) {
                JavaClass[] implInterfaces = javaClass.getImplementedInterfaces();
                cd.interfaces = new HashSet();
                for (int i = 0; i < implInterfaces.length; i++) {
                    cd.interfaces.add(implInterfaces[i].getFullyQualifiedName());
                }
            }
        } else if (interfaceList.length() == 0) {
            // empty interface list prevents creation of interface element
            cd.interfaces = null;
        } else {
            // split list and create set for interface elements
            StringTokenizer tokener = new StringTokenizer(interfaceList, ",");
            cd.interfaces = new HashSet();
            while (tokener.hasMoreTokens()) {
                String iface = tokener.nextToken().trim();
                if (iface.length() > 0) {
                    cd.interfaces.add(iface);
                }
            }
        }

        cd.jcrType = tag.getNamedParameter(JCR_TYPE);
        cd.jcrSuperTypes = tag.getNamedParameter(JCR_SUPER_TYPES);
        cd.jcrMixinTypes = tag.getNamedParameter(JCR_MIXIN_TYPES);

        // only reset default if explicitly stated
        if (tag.getNamedParameter(DISCRIMINATOR) != null) {
            cd.discriminator = Boolean.valueOf(
                tag.getNamedParameter(DISCRIMINATOR)).booleanValue();
        }

        return cd;
    }

    private ClassDescriptor(Log log, DocletTag tag) {
        super(log, tag);
    }

    void addChild(AbstractDescriptorElement child) {
        if (child != null) {
            if (children == null) {
                children = new ArrayList();
            }

            children.add(child);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#generate(org.apache.sling.maven.jcrocm.XMLWriter)
     */
    void generate(XMLWriter xmlWriter) {

        xmlWriter.println();
        xmlWriter.printComment("Class: " + className);

        xmlWriter.printElementStart(ELEMENT_CLASS_DESCRIPTOR, true);
        xmlWriter.printAttribute(CLASS_NAME, className);
        xmlWriter.printAttribute(JCR_TYPE, jcrType);
        xmlWriter.printAttribute(JCR_SUPER_TYPES, jcrSuperTypes);
        xmlWriter.printAttribute(JCR_MIXIN_TYPES, jcrMixinTypes);
        xmlWriter.printAttribute(EXTEND, extend);
        xmlWriter.printAttribute(ABSTRACT, isAbstract);
        xmlWriter.printAttribute(INTERFACE, isInterface);

        // only write discriminator if false, true is the default here
        if (!discriminator) {
            xmlWriter.printAttribute(DISCRIMINATOR, "false");
        }

        xmlWriter.printElementStartClose(false);

        // interface implementations
        if (interfaces != null) {
            for (Iterator ii = interfaces.iterator(); ii.hasNext();) {
                String iface = (String) ii.next();
                xmlWriter.println();
                xmlWriter.printElementStart(ELEMENT_IMPLEMENT_DESCRIPTOR, true);
                xmlWriter.printAttribute(INTERFACE_NAME, iface);
                xmlWriter.printElementStartClose(true);
            }
        }

        // fields, beans and collections
        if (children != null) {
            for (Iterator ci = children.iterator(); ci.hasNext();) {
                AbstractDescriptorElement child = (AbstractDescriptorElement) ci.next();
                xmlWriter.println();
                child.generate(xmlWriter);
            }
        }

        xmlWriter.printElementEnd(ELEMENT_CLASS_DESCRIPTOR);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.sling.maven.jcrocm.AbstractDescriptorElement#validate()
     */
    boolean validate() {
        boolean valid = true;

        if (children != null) {
            if (children != null) {
                for (Iterator ci = children.iterator(); ci.hasNext();) {
                    AbstractDescriptorElement child = (AbstractDescriptorElement) ci.next();
                    valid &= child.validate();
                    if (!valid) break;
                }
            }
        }

        return valid;
    }
}
