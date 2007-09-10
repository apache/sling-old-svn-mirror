/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.content.jcr.internal.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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
        parser = new KXmlParser();
        reset();
    }
    
    public void reset() {
        descriptors = new MappingDescriptor();
        verified = false;
    }
    
    /**
     * @throws IllegalStateException If no descriptors are available
     * @throws XmlPullParserException If an error occurrs validating the descriptors
     * @return
     */
    public MappingDescriptor getMappingDescriptor() throws XmlPullParserException {
        verify();
        return descriptors;
    }
    
    public void parse(List urlList) throws IOException, XmlPullParserException {
        for (Iterator ui=urlList.iterator(); ui.hasNext(); ) {
            URL url = (URL) ui.next();
            InputStream ins = null;
            try {
                ins = url.openStream();
                parser.setProperty("http://xmlpull.org/v1/doc/properties.html#location", url);
                parse(ins);
            } finally {
                IOUtils.closeQuietly(ins);
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

        if (verified) {
            throw new IllegalStateException("Please reset before parsing");
        }
        
        // set the parser input, use null encoding to force detection with <?xml?>
        parser.setInput(ins, null);
        state = STATE_NULL;
        
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (state) {
                    case STATE_NULL:
                        if (!"graffito-jcr".equals(parser.getName())) {
                            throw unexpectedElement();
                        }
                        descriptors.setPackage(getOptionalAttribute("package"));
                        currentClassDescriptor = null;
                        state = STATE_MAPPING;
                        break;
                        
                    case STATE_MAPPING:
                        if (!"class-descriptor".equals(parser.getName())) {
                            throw unexpectedElement();
                        }
                        currentClassDescriptor = parseClassDescriptor();
                        state = STATE_CLASS;
                        break;

                    case STATE_CLASS:
                        if ("implement-descriptor".equals(parser.getName())) {
                            currentClassDescriptor.addImplementDescriptor(parseImplementDescriptor());
                        } else if ("field-descriptor".equals(parser.getName())) {
                            currentClassDescriptor.addFieldDescriptor(parseFieldDescriptor());
                        } else if ("bean-descriptor".equals(parser.getName())) {
                            currentClassDescriptor.addBeanDescriptor(parseBeanDescriptor());
                        } else if ("collection-descriptor".equals(parser.getName())) {
                            currentClassDescriptor.addCollectionDescriptor(parseCollectionDescriptor());
                        } else {
                            throw unexpectedElement();
                        }
                        state = STATE_DESCRIPTOR;
                        break;
                        
                    case STATE_DESCRIPTOR:
                        // single descriptors are empty, fail
                        throw unexpectedElement();

                    default:
                        // don't care
                }
                
            } else if (eventType == XmlPullParser.END_TAG) {
                switch (state) {
                    case STATE_NULL:
                        // do not expected an end tag in this state...
                        break;
                        
                    case STATE_MAPPING:
                        if (!"graffito-jcr".equals(parser.getName())) {
                            throw unexpectedElement();
                        }
                        
                        state = STATE_NULL;
                        break;

                    case STATE_CLASS:
                        if (!"class-descriptor".equals(parser.getName())) {
                            throw unexpectedElement();
                        }

                        descriptors.addClassDescriptor(currentClassDescriptor);
                        
                        state = STATE_MAPPING;
                        break;
                        
                    case STATE_DESCRIPTOR:
                        if (!"implement-descriptor".equals(parser.getName())
                                && !"field-descriptor".equals(parser.getName())
                                && !"bean-descriptor".equals(parser.getName())
                                && ! "collection-descriptor".equals(parser.getName())) {
                            throw unexpectedElement();
                        }
                        
                        state = STATE_CLASS;
                        break;

                    default:
                        // don't care
                }
            }
            
            eventType = parser.next();
        }
    }
    
    private ClassDescriptor parseClassDescriptor() throws XmlPullParserException {
        ClassDescriptor fd = new ClassDescriptor();
        
        /*
         * className CDATA #REQUIRED
         * jcrNodeType CDATA #IMPLIED
         * jcrSuperTypes CDATA #IMPLIED
         * jcrMixinTypes CDATA #IMPLIED
         * extend CDATA #IMPLIED
         * abstract (true|false) "false"
         * interface (true|false) "false"
         * discriminator (true|false) "true"
         */
        
        fd.setClassName(getRequiredAttribute("className"));
        fd.setJcrNodeType(getOptionalAttribute("jcrNodeType"));
        fd.setJcrSuperTypes(getOptionalAttribute("jcrSuperTypes"));
        fd.setJcrMixinTypes(getOptionalAttribute("jcrMixinTypes", (String[]) null));
        
        fd.setExtend(getOptionalAttribute("extend"));
        
        fd.setAbstract(getOptionalAttribute("abstract", false));
        fd.setInterface(getOptionalAttribute("interface", false));
        fd.setDiscriminator(getOptionalAttribute("discriminator", true));
        
        return fd;
    }
    
    private ImplementDescriptor parseImplementDescriptor() throws XmlPullParserException {
        ImplementDescriptor fd = new ImplementDescriptor();
        
        /*
         * interfaceName CDATA #REQUIRED
         */
        
        fd.setInterfaceName(getRequiredAttribute("interfaceName"));
        
        return fd;
    }
    
    private FieldDescriptor parseFieldDescriptor() throws XmlPullParserException {
        FieldDescriptor fd = new FieldDescriptor();
        
        /*
         *  fieldName CDATA #REQUIRED
         *  fieldType CDATA #IMPLIED
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
        
        fd.setFieldName(getRequiredAttribute("fieldName"));
        fd.setFieldType(getOptionalAttribute("fieldType"));
        fd.setJcrName(getOptionalAttribute("jcrName", fd.getFieldName()));
        
        fd.setId(getOptionalAttribute("id", false));
        fd.setPath(getOptionalAttribute("path", false));
        
        fd.setJcrType(getOptionalAttribute("jcrType"));
        fd.setJcrAutoCreated(getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(getOptionalAttribute("jcrProtected", false));
        fd.setJcrMultiple(getOptionalAttribute("jcrMultiple", false));
        
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
         * jcrNodeType CDATA #IMPLIED
         * jcrAutoCreated (true | false) "false"
         * jcrMandatory (true | false) "false"
         * jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) "COPY"
         * jcrProtected (true | false) "false"
         * jcrSameNameSiblings (true | false) "false"         
         */
        
        fd.setFieldName(getRequiredAttribute("fieldName"));
        fd.setJcrName(getOptionalAttribute("jcrName", fd.getFieldName()));
        fd.setProxy(getOptionalAttribute("proxy", false));
        
        fd.setAutoRetrieve(getOptionalAttribute("autoRetrieve", true));
        fd.setAutoUpdate(getOptionalAttribute("autoUpdate", true));
        fd.setAutoInsert(getOptionalAttribute("autoInsert", true));
        
        fd.setJcrNodeType(getOptionalAttribute("jcrNodeType"));
        fd.setJcrAutoCreated(getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(getOptionalAttribute("jcrProtected", false));
        fd.setJcrSameNameSiblings(getOptionalAttribute("jcrSameNameSiblings", false));
        
        fd.setJcrType(getOptionalAttribute("jcrType"));
        fd.setJcrMultiple(getOptionalAttribute("jcrMultiple", false));

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
         * jcrNodeType CDATA #IMPLIED
         * jcrAutoCreated (true | false) "false"
         * jcrMandatory (true | false) "false"
         * jcrOnParentVersion (COPY | VERSION | INITIALIZE | COMPUTE | IGNORE | ABORT) "COPY"
         * jcrProtected (true | false) "false"
         * jcrSameNameSiblings (true | false) "false"  
         */
        
        fd.setFieldName(getRequiredAttribute("fieldName"));
        fd.setJcrName(getOptionalAttribute("jcrName", fd.getFieldName()));
        fd.setProxy(getOptionalAttribute("proxy", false));
        
        fd.setAutoRetrieve(getOptionalAttribute("autoRetrieve", true));
        fd.setAutoUpdate(getOptionalAttribute("autoUpdate", true));
        fd.setAutoInsert(getOptionalAttribute("autoInsert", true));
        
        fd.setElementClassName(getRequiredAttribute("elementClassName"));
        fd.setCollectionClassName(getOptionalAttribute("collectionClassName"));
        fd.setCollectionConverter(getOptionalAttribute("collectionConverter"));
        
        fd.setJcrNodeType(getOptionalAttribute("jcrNodeType"));
        fd.setJcrAutoCreated(getOptionalAttribute("jcrAutoCreated", false));
        fd.setJcrMandatory(getOptionalAttribute("jcrMandatory", false));
        fd.setJcrOnParentVersion(getOptionalAttribute("jcrOnParentVersion", "COPY"));
        fd.setJcrProtected(getOptionalAttribute("jcrProtected", false));
        fd.setJcrSameNameSiblings(getOptionalAttribute("jcrSameNameSiblings", false));
        
        fd.setJcrType(getOptionalAttribute("jcrType"));
        fd.setJcrMultiple(getOptionalAttribute("jcrMultiple", false));
        
        return fd;
    }
    
    //---------- Attribute access helper --------------------------------------
    
    private String getRequiredAttribute(String attrName) throws XmlPullParserException {
        String attrVal = parser.getAttributeValue(null, attrName);
        if (attrVal != null) {
            return attrVal;
        }
        
        // fail if value is missing
        throw missingAttribute(attrName);
    }
    
    private String getOptionalAttribute(String attrName) {
        return getOptionalAttribute(attrName, (String) null);
    }
    
    private String getOptionalAttribute(String attrName, String defaultValue) {
        String attrVal = parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? attrVal : defaultValue;
    }
    
    private String[] getOptionalAttribute(String attrName, String[] defaultValue) {
        String attrVal = parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? attrVal.split(",") : defaultValue;
    }
    
    private boolean getOptionalAttribute(String attrName, boolean defaultValue) {
        String attrVal = parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? "true".equalsIgnoreCase(attrVal) : defaultValue;
    }
    
    //---------- Error Handling support ---------------------------------------
    
    private XmlPullParserException missingAttribute(String attrName) {
        String message = "Missing Attribute " + attrName + " in element " + parser.getName();
        return new XmlPullParserException(message, parser, null);
    }
    
    private XmlPullParserException unexpectedElement() {
        String message = "Illegal Element " + parser.getName();
        return new XmlPullParserException(message, parser, null);
    }
    
    //---------- Verification of the mapping descriptors ----------------------
    
    private void verify() throws XmlPullParserException {
        // nothing to do anymore
        if (verified) {
            return;
        }

        if (descriptors == null) {
            throw new IllegalStateException("Nothing has been read yet");
        }

        List errors = new ArrayList();
        List rootClassDescriptors = new ArrayList();
        errors = solveReferences(errors, rootClassDescriptors);
        errors = validateDescriptors(errors, rootClassDescriptors);

        if (!errors.isEmpty()) {
            throw new XmlPullParserException("Mapping files contain errors."
                + getErrorMessage(errors));
        }
    }

    private List solveReferences(List errors, List rootClassDescriptors) {
        Set toRemove = new HashSet();
        for (Iterator it = descriptors.getClassDescriptorsByClassName().entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            ClassDescriptor cd = (ClassDescriptor) entry.getValue();

            if (null != cd.getExtend() && !"".equals(cd.getExtend())) {
                ClassDescriptor superClassDescriptor = descriptors.getClassDescriptorByName(cd.getExtend());

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

            Collection interfaces = cd.getImplements();
            if (interfaces.size() > 0) {
                for (Iterator iterator = interfaces.iterator(); iterator.hasNext();) {
                    String interfaceName = (String) iterator.next();
                    ClassDescriptor interfaceClassDescriptor = descriptors.getClassDescriptorByName(interfaceName);

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

        }

        if (!toRemove.isEmpty()) {
            log.info("Removing dropped classes from map");
            for (Iterator ci=toRemove.iterator(); ci.hasNext(); ) {
                ClassDescriptor cd = (ClassDescriptor) ci.next();
                dropClassDescriptor(cd);
            }
        }
        return errors;
    }

    private void dropClassDescriptor(ClassDescriptor cd) {
        // remove descriptor
        descriptors.getClassDescriptorsByClassName().remove(cd.getClassName());
        descriptors.getClassDescriptorsByNodeType().remove(cd.getJcrNodeType());
        
        if (cd.hasDescendants()) {
            for (Iterator di=cd.getDescendantClassDescriptors().iterator(); di.hasNext(); ) {
                ClassDescriptor desc = (ClassDescriptor) di.next();
                log.warn("Dropping class {}: Depends on non-resolvable class {}", desc.getClassName(), cd.getClassName());
                dropClassDescriptor(desc);
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
    private List validateDescriptors(List errors, Collection classDescriptors) {
        for (Iterator it = classDescriptors.iterator(); it.hasNext();) {
            ClassDescriptor classDescriptor = (ClassDescriptor) it.next();
            try {
                classDescriptor.afterPropertiesSet();
                if (classDescriptor.hasDescendants()) {
                    errors = validateDescriptors(errors,
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
    
    
    private String getErrorMessage(List errors) {
        final String lineSep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        for(Iterator it = errors.iterator(); it.hasNext();) {
            buf.append(lineSep).append(it.next());
        }

        return buf.toString();
    }    
    

}
