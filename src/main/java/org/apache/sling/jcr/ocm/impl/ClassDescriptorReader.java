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
package org.apache.sling.jcr.ocm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ImplementDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.kxml2.io.KXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The <code>ClassDescriptorReader</code> TODO
 *
 * Use:
 * 1. Instantiatate
 * 2. Multiply call {@link #parse(InputStream)}
 * 3. Call {@link #getMappingDescriptor()} (more than once always returns the same result)
 * 4. Call {@link #reset()}  and go back to step 2
 */
public class ClassDescriptorReader {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ClassDescriptorReader.class);

    private static final int STATE_NULL = 0;
    private static final int STATE_MAPPING = 2;
    private static final int STATE_CLASS = 3;
    private static final int STATE_DESCRIPTOR = 4;

    private KXmlParser parser;

    private int state = STATE_NULL;

    private MappingDescriptor descriptors;
    boolean verified;
    private ClassDescriptor currentClassDescriptor;

    public ClassDescriptorReader() {
        this.parser = new KXmlParser();
        this.reset();
    }

    public void reset() {
        this.descriptors = new MappingDescriptor();
        this.verified = false;
    }

    /**
     * @throws IllegalStateException If no descriptors are available
     * @throws XmlPullParserException If an error occurrs validating the descriptors
     * @return The mapping descriptor.
     */
    public MappingDescriptor getMappingDescriptor() throws XmlPullParserException {
        this.verify();
        return this.descriptors;
    }

    public void parse(List<URL> urlList) throws IOException, XmlPullParserException {
        for (URL url : urlList) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                this.parser.setProperty("http://xmlpull.org/v1/doc/properties.html#location", url);
                this.parse(ins);
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }

    /**
     *
     * @param ins
     * @throws IOException
     * @throws XmlPullParserException
     * @throws IllegalStateException If the {@link #getMappingDescriptor()}
     *      method has been called but not the {@link #reset()} method before
     *      calling this method.
     */
    public void parse(InputStream ins) throws IOException, XmlPullParserException {

        if (this.verified) {
            throw new IllegalStateException("Please reset before parsing");
        }

        // set the parser input, use null encoding to force detection with <?xml?>
        this.parser.setInput(ins, null);
        this.state = STATE_NULL;

        int eventType = this.parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (this.state) {
                    case STATE_NULL:
                        if ("graffito-jcr".equals(this.parser.getName())) {
                            // accept for backwards compatibility
                            // might want to log this situation
                        } else if (!"jackrabbit-ocm".equals(this.parser.getName())) {
                            throw this.unexpectedElement();
                        }
                        this.descriptors.setPackage(this.getOptionalAttribute("package"));
                        this.currentClassDescriptor = null;
                        this.state = STATE_MAPPING;
                        break;

                    case STATE_MAPPING:
                        if (!"class-descriptor".equals(this.parser.getName())) {
                            throw this.unexpectedElement();
                        }
                        this.currentClassDescriptor = this.parseClassDescriptor();
                        this.state = STATE_CLASS;
                        break;

                    case STATE_CLASS:
                        if ("implement-descriptor".equals(this.parser.getName())) {
                            this.currentClassDescriptor.addImplementDescriptor(this.parseImplementDescriptor());
                        } else if ("field-descriptor".equals(this.parser.getName())) {
                            this.currentClassDescriptor.addFieldDescriptor(this.parseFieldDescriptor());
                        } else if ("bean-descriptor".equals(this.parser.getName())) {
                            this.currentClassDescriptor.addBeanDescriptor(this.parseBeanDescriptor());
                        } else if ("collection-descriptor".equals(this.parser.getName())) {
                            this.currentClassDescriptor.addCollectionDescriptor(this.parseCollectionDescriptor());
                        } else {
                            throw this.unexpectedElement();
                        }
                        this.state = STATE_DESCRIPTOR;
                        break;

                    case STATE_DESCRIPTOR:
                        // single descriptors are empty, fail
                        throw this.unexpectedElement();

                    default:
                        // don't care
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                switch (this.state) {
                    case STATE_NULL:
                        // do not expected an end tag in this state...
                        break;

                    case STATE_MAPPING:
                        if (!"graffito-jcr".equals(this.parser.getName())
                            && !"jackrabbit-ocm".equals(this.parser.getName())) {
                            throw this.unexpectedElement();
                        }

                        this.state = STATE_NULL;
                        break;

                    case STATE_CLASS:
                        if (!"class-descriptor".equals(this.parser.getName())) {
                            throw this.unexpectedElement();
                        }

                        this.descriptors.addClassDescriptor(this.currentClassDescriptor);

                        this.state = STATE_MAPPING;
                        break;

                    case STATE_DESCRIPTOR:
                        if (!"implement-descriptor".equals(this.parser.getName())
                                && !"field-descriptor".equals(this.parser.getName())
                                && !"bean-descriptor".equals(this.parser.getName())
                                && ! "collection-descriptor".equals(this.parser.getName())) {
                            throw this.unexpectedElement();
                        }

                        this.state = STATE_CLASS;
                        break;

                    default:
                        // don't care
                }
            }

            eventType = this.parser.next();
        }
    }

    private ClassDescriptor parseClassDescriptor() throws XmlPullParserException {
        ClassDescriptor fd = new ClassDescriptor();

        /*
         * className CDATA #REQUIRED
         * jcrType CDATA #IMPLIED
         * jcrSuperTypes CDATA #IMPLIED
         * jcrMixinTypes CDATA #IMPLIED
         * extend CDATA #IMPLIED
         * abstract (true|false) "false"
         * interface (true|false) "false"
         * discriminator (true|false) "true"
         */

        fd.setClassName(this.getRequiredAttribute("className"));
        fd.setJcrType(this.getOptionalAttribute("jcrType"));
        fd.setJcrSuperTypes(this.getOptionalAttribute("jcrSuperTypes"));
        fd.setJcrMixinTypes(this.getOptionalAttribute("jcrMixinTypes", (String[]) null));

        fd.setExtend(this.getOptionalAttribute("extend"));

        fd.setAbstract(this.getOptionalAttribute("abstract", false));
        fd.setInterface(this.getOptionalAttribute("interface", false));
        fd.setDiscriminator(this.getOptionalAttribute("discriminator", true));

        return fd;
    }

    private ImplementDescriptor parseImplementDescriptor() throws XmlPullParserException {
        ImplementDescriptor fd = new ImplementDescriptor();

        /*
         * interfaceName CDATA #REQUIRED
         */

        fd.setInterfaceName(this.getRequiredAttribute("interfaceName"));

        return fd;
    }

    private FieldDescriptor parseFieldDescriptor() throws XmlPullParserException {
        FieldDescriptor fd = new FieldDescriptor();

        /*
         *  fieldName CDATA #REQUIRED
         *  jcrName CDATA #IMPLIED
         *  id (true | false) "false"
         *  path (true | false) "false"
         *  jcrType (String | Date | Long | Double | Boolean | Binary) #IMPLIED
         *  jcrAutoCreated (true | false) "false"
         *  jcrMandatory (true | false) "false"
         *  jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) "COPY"
         *  jcrProtected (true | false) "false"
         *  jcrMultiple (true | false) "false"
         */

        fd.setFieldName(this.getRequiredAttribute("fieldName"));
        fd.setJcrName(this.getOptionalAttribute("jcrName", fd.getFieldName()));

        fd.setId(this.getOptionalAttribute("id", false));
        fd.setPath(this.getOptionalAttribute("path", false));

        fd.setJcrType(this.getOptionalAttribute("jcrType"));
        fd.setJcrAutoCreated(this.getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(this.getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(this.getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(this.getOptionalAttribute("jcrProtected", false));
        fd.setJcrMultiple(this.getOptionalAttribute("jcrMultiple", false));
        fd.setJcrDefaultValue(this.getOptionalAttribute("jcrDefaultValue"));

        return fd;
    }

    private BeanDescriptor parseBeanDescriptor() throws XmlPullParserException {
        BeanDescriptor fd = new BeanDescriptor();

        /*
         * fieldName CDATA #REQUIRED
         * jcrName CDATA #IMPLIED
         * proxy (true | false) "false"
         * autoRetrieve (true|false) "true"
         * autoUpdate (true|false) "true"
         * autoInsert (true|false) "true"
         * converter CDATA #IMPLIED
         * jcrType CDATA #IMPLIED
         * jcrAutoCreated (true | false) "false"
         * jcrMandatory (true | false) "false"
         * jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) "COPY"
         * jcrProtected (true | false) "false"
         * jcrSameNameSiblings (true | false) "false"
         */

        fd.setFieldName(this.getRequiredAttribute("fieldName"));
        fd.setJcrName(this.getOptionalAttribute("jcrName", fd.getFieldName()));
        fd.setProxy(this.getOptionalAttribute("proxy", false));

        fd.setAutoRetrieve(this.getOptionalAttribute("autoRetrieve", true));
        fd.setAutoUpdate(this.getOptionalAttribute("autoUpdate", true));
        fd.setAutoInsert(this.getOptionalAttribute("autoInsert", true));

        fd.setJcrType(this.getOptionalAttribute("jcrType"));
        fd.setJcrAutoCreated(this.getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(this.getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(this.getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(this.getOptionalAttribute("jcrProtected", false));
        fd.setJcrSameNameSiblings(this.getOptionalAttribute("jcrSameNameSiblings", false));

        fd.setJcrMultiple(this.getOptionalAttribute("jcrMultiple", false));

        return fd;
    }

    private CollectionDescriptor parseCollectionDescriptor() throws XmlPullParserException {
        CollectionDescriptor fd = new CollectionDescriptor();

        /*
         * fieldName CDATA #REQUIRED
         * jcrName CDATA #IMPLIED
         * proxy (true | false) "false"
         * autoRetrieve (true|false) "true"
         * autoUpdate (true|false) "true"
         * autoInsert (true|false) "true"
         * elementClassName CDATA #REQUIRED
         * collectionClassName CDATA #IMPLIED
         * collectionConverter CDATA #IMPLIED
         * jcrType CDATA #IMPLIED
         * jcrAutoCreated (true | false) "false"
         * jcrMandatory (true | false) "false"
         * jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) "COPY"
         * jcrProtected (true | false) "false"
         * jcrSameNameSiblings (true | false) "false"
         */

        fd.setFieldName(this.getRequiredAttribute("fieldName"));
        fd.setJcrName(this.getOptionalAttribute("jcrName", fd.getFieldName()));
        fd.setProxy(this.getOptionalAttribute("proxy", false));

        fd.setAutoRetrieve(this.getOptionalAttribute("autoRetrieve", true));
        fd.setAutoUpdate(this.getOptionalAttribute("autoUpdate", true));
        fd.setAutoInsert(this.getOptionalAttribute("autoInsert", true));

        fd.setElementClassName(this.getRequiredAttribute("elementClassName"));
        fd.setCollectionClassName(this.getOptionalAttribute("collectionClassName"));
        fd.setCollectionConverter(this.getOptionalAttribute("collectionConverter"));

        fd.setJcrAutoCreated(this.getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(this.getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(this.getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(this.getOptionalAttribute("jcrProtected", false));
        fd.setJcrSameNameSiblings(this.getOptionalAttribute("jcrSameNameSiblings", false));

        fd.setJcrType(this.getOptionalAttribute("jcrType"));
        fd.setJcrMultiple(this.getOptionalAttribute("jcrMultiple", false));

        return fd;
    }

    //---------- Attribute access helper --------------------------------------

    private String getRequiredAttribute(String attrName) throws XmlPullParserException {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        if (attrVal != null) {
            return attrVal;
        }

        // fail if value is missing
        throw this.missingAttribute(attrName);
    }

    private String getOptionalAttribute(String attrName) {
        return this.getOptionalAttribute(attrName, (String) null);
    }

    private String getOptionalAttribute(String attrName, String defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? attrVal : defaultValue;
    }

    private String[] getOptionalAttribute(String attrName, String[] defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? attrVal.split(",") : defaultValue;
    }

    private boolean getOptionalAttribute(String attrName, boolean defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? "true".equalsIgnoreCase(attrVal) : defaultValue;
    }

    //---------- Error Handling support ---------------------------------------

    private XmlPullParserException missingAttribute(String attrName) {
        String message = "Missing Attribute " + attrName + " in element " + this.parser.getName();
        return new XmlPullParserException(message, this.parser, null);
    }

    private XmlPullParserException unexpectedElement() {
        String message = "Illegal Element " + this.parser.getName();
        return new XmlPullParserException(message, this.parser, null);
    }

    //---------- Verification of the mapping descriptors ----------------------

    private void verify() throws XmlPullParserException {
        // nothing to do anymore
        if (this.verified) {
            return;
        }

        if (this.descriptors == null) {
            throw new IllegalStateException("Nothing has been read yet");
        }

        List<String> errors = new ArrayList<String>();
        List<ClassDescriptor> rootClassDescriptors = new ArrayList<ClassDescriptor>();
        errors = this.solveReferences(errors, rootClassDescriptors);
        errors = this.validateDescriptors(errors, rootClassDescriptors);

        if (!errors.isEmpty()) {
            throw new XmlPullParserException("Mapping files contain errors."
                + this.getErrorMessage(errors));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> solveReferences(List<String> errors, List<ClassDescriptor> rootClassDescriptors) {
        Set<ClassDescriptor> toRemove = new HashSet<ClassDescriptor>();
        for( ClassDescriptor cd : (Collection<ClassDescriptor>)this.descriptors.getClassDescriptorsByClassName().values() ) {

            if (null != cd.getExtend() && !"".equals(cd.getExtend())) {
                ClassDescriptor superClassDescriptor = this.descriptors.getClassDescriptorByName(cd.getExtend());

                if (superClassDescriptor == null) {
                    log.warn("Dropping class {}: Base class {} not registered", cd.getClassName(), cd.getExtend());
                    toRemove.add(cd);
//                    errors.add("Cannot find mapping for class "
//                        + cd.getExtend() + " referenced as extends from "
//                        + cd.getClassName());
                } else {
                    log.debug("Class {} extends {}", cd.getClassName(), cd.getExtend());
                    cd.setSuperClassDescriptor(superClassDescriptor);
                }
            } else {
                log.debug("Class {} is a root class", cd.getClassName());
                rootClassDescriptors.add(cd);
            }

            Collection<String> interfaces = cd.getImplements();
            for (String interfaceName : interfaces) {
                ClassDescriptor interfaceClassDescriptor = this.descriptors.getClassDescriptorByName(interfaceName);

                if (interfaceClassDescriptor == null) {
                    log.warn("Dropping class {}: Interface class {} not registered", cd.getClassName(), interfaceName);
                    toRemove.add(cd);
//                        errors.add("Cannot find mapping for interface "
//                            + interfaceName + " referenced as implements from "
//                            + cd.getClassName());
                } else {
                    log.debug("Class " + cd.getClassName() + " implements "
                        + interfaceName);
                    //cd.setSuperClassDescriptor(interfaceClassDescriptor);
                    interfaceClassDescriptor.addDescendantClassDescriptor(cd);
                }

            }

        }

        if (!toRemove.isEmpty()) {
            log.info("Removing dropped classes from map");
            for (ClassDescriptor cd : toRemove) {
                this.dropClassDescriptor(cd);
            }
        }
        return errors;
    }

    @SuppressWarnings("unchecked")
    private void dropClassDescriptor(ClassDescriptor cd) {
        // remove descriptor
        this.descriptors.getClassDescriptorsByClassName().remove(cd.getClassName());
        this.descriptors.getClassDescriptorsByNodeType().remove(cd.getJcrType());

        if (cd.hasDescendants()) {
            for( ClassDescriptor desc : (Collection<ClassDescriptor>)cd.getDescendantClassDescriptors()) {
                log.warn("Dropping class {}: Depends on non-resolvable class {}", desc.getClassName(), cd.getClassName());
                this.dropClassDescriptor(desc);
            }
        }
    }

    /**
     * Validate all class descriptors.
     * This method validates the toplevel ancestors and after the descendants.
     * Otherwise, we can have invalid settings in the class descriptors
     * @param errors all errors found during the validation process
     * @param classDescriptors the ancestor classdescriptors
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<String> validateDescriptors(List<String> errors, Collection<ClassDescriptor> classDescriptors) {
        for (ClassDescriptor classDescriptor : classDescriptors) {
            try {
                classDescriptor.afterPropertiesSet();
                if (classDescriptor.hasDescendants()) {
                    errors = this.validateDescriptors(errors,
                        classDescriptor.getDescendantClassDescriptors());
                }
            } catch (JcrMappingException jme) {
                // TODO: consider dropping the descriptor due to errors ...
                log.warn("Mapping of class " + classDescriptor.getClassName()
                    + " is invalid", jme);
                errors.add(jme.getMessage());
            }
        }
        return errors;
    }


    private String getErrorMessage(List<String> errors) {
        final String lineSep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        for(String msg : errors) {
            buf.append(lineSep).append(msg);
        }

        return buf.toString();
    }


}
