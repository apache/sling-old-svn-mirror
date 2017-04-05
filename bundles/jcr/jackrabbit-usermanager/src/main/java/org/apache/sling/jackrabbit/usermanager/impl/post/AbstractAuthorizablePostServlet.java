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
package org.apache.sling.jackrabbit.usermanager.impl.post;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResourceProvider;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.servlets.post.impl.helper.DateParser;
import org.apache.sling.servlets.post.impl.helper.RequestProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all the POST servlets for the UserManager operations
 */
public abstract class AbstractAuthorizablePostServlet extends
        AbstractPostServlet {
    private static final long serialVersionUID = -5918670409789895333L;

    public static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAuthorizablePostServlet.class);

    private DateParser dateParser;

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(Map<String, Object> props) {
        dateParser = new DateParser();
        String[] dateFormats = OsgiUtil.toStringArray(props.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            dateParser.register(dateFormat);
        }
    }

    protected void deactivate() {
        dateParser = null;
    }

    // ------ The methods below are based on the private methods from the
    // ModifyOperation class -----

    /**
     * Collects the properties that form the content to be written back to the
     * repository.
     * @param properties the properties out of which to generate the {@link RequestProperty}s
     * @return the list of {@link RequestProperty}s
     */
    protected Collection<RequestProperty> collectContent(
            Map<String, ?> properties) {

        boolean requireItemPrefix = requireItemPathPrefix(properties);

        // walk the request parameters and collect the properties (the key is the property path).
        Map<String, RequestProperty> reqProperties = new HashMap<String, RequestProperty>();
        for (Map.Entry<String, ?> e : properties.entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with sling:post
            if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
                continue;
            }
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }

            // ensure the paramName is an absolute property path (i.e. starts with "/", where root refers to the authorizable's root, https://issues.apache.org/jira/browse/SLING-1577)
            String propPath;
            if (paramName.startsWith("./")) {
                propPath = paramName.substring(1);
            } else {
                propPath = "/" + paramName;
            }

            if (propPath.indexOf("..") != -1) {
                // it is not supported to set properties potentially outside of the authorizable node
                LOG.warn("Property path containing '..' is not supported, skipping parameter {}", SlingPostConstants.SUFFIX_COPY_FROM, paramName);
                continue; // skip it.
            }

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.TYPE_HINT_SUFFIX);

                String typeHintValue = convertToString(e.getValue());
                if (typeHintValue != null) {
                    prop.setTypeHintValue(typeHintValue);
                }

                continue;
            }

            // @DefaultValue
            if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.DEFAULT_VALUE_SUFFIX);

                prop.setDefaultValues(convertToRequestParameterArray(e.getValue()));

                continue;
            }

            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            // @ValueFrom example:
            // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
            // causes the JCR Text property to be set to the value of the
            // fulltext form field.
            if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.VALUE_FROM_SUFFIX);

                // @ValueFrom params must have exactly one value, else ignored
                String [] valueFrom = convertToStringArray(e.getValue());
                if (valueFrom.length == 1) {
                    String refName = valueFrom[0];
                    RequestParameter[] refValues = convertToRequestParameterArray(refName);
                    if (refValues != null) {
                        prop.setValues(refValues);
                    }
                }

                continue;
            }

            // SLING-458: Allow Removal of properties prior to update
            // @Delete example:
            // <input name="./Text@Delete" type="hidden" />
            // causes the JCR Text property to be deleted before update
            if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath, SlingPostConstants.SUFFIX_DELETE);

                prop.setDelete(true);

                continue;
            }

            // SLING-455: @MoveFrom means moving content to another location
            // @MoveFrom example:
            // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by moving the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
                // don't support @MoveFrom here
                LOG.warn("Suffix {} not supported, skipping parameter {}", SlingPostConstants.SUFFIX_MOVE_FROM, paramName);
                continue;
            }

            // SLING-455: @CopyFrom means moving content to another location
            // @CopyFrom example:
            // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
                // don't support @CopyFrom here
                LOG.warn("Suffix {} not supported, skipping parameter {}", SlingPostConstants.SUFFIX_COPY_FROM, paramName);
                continue;
            }

            // plain property, create from values
            RequestProperty prop = getOrCreateRequestProperty(reqProperties,
                propPath, null);
            prop.setValues(convertToRequestParameterArray(e.getValue()));
        }

        return reqProperties.values();
    }

    /**
     * Returns the request property for the given property path. If such a
     * request property does not exist yet it is created and stored in the
     * <code>props</code>.
     *
     * @param props The map of already seen request properties 
     *               (key is the property path).
     * @param paramPath The absolute path of the property including the
     *            <code>suffix</code> to be looked up.
     * @param suffix The (optional) suffix to remove from the
     *            <code>paramName</code> before looking it up.
     * @return The {@link RequestProperty} for the <code>paramName</code>.
     */
    private RequestProperty getOrCreateRequestProperty(
            Map<String, RequestProperty> props, String paramPath, String suffix) {
        if (suffix != null && paramPath.endsWith(suffix)) {
            paramPath = paramPath.substring(0, paramPath.length()
                - suffix.length());
        }

        RequestProperty prop = props.get(paramPath);
        if (prop == null) {
            prop = new RequestProperty(paramPath);
            props.put(paramPath, prop);
        }

        return prop;
    }

    /**
     * Removes all properties listed as {@link RequestProperty#isDelete()} from
     * the authorizable.
     *
     * @param authorizable The
     *            <code>org.apache.jackrabbit.api.security.user.Authorizable</code>
     *            that should have properties deleted.
     * @param reqProperties The collection of request properties to check for
     *            properties to be removed.
     * @param changes The <code>List</code> to be updated with
     *            information on deleted properties.
     * @throws RepositoryException Is thrown if an error occurrs checking or
     *             removing properties.
     */
    protected void processDeletes(Authorizable authorizable,
            Collection<RequestProperty> reqProperties,
            List<Modification> changes) throws RepositoryException {

        for (RequestProperty property : reqProperties) {
            if (property.isDelete()) {
                if (authorizable.hasProperty(property.getName())) {
                    authorizable.removeProperty(property.getName());
                    changes.add(Modification.onDeleted(property.getPath()));
                }
            }
        }
    }

    /**
     * Writes back the content
     * @param session the sessioin to write the authorizable properties
     * @param authorizable the authorizable to modify
     * @param reqProperties the properties to write
     * @param changes the list of changes which is supposed to be extended
     *
     * @throws RepositoryException if a repository error occurs
     */
    protected void writeContent(Session session, Authorizable authorizable,
            Collection<RequestProperty> reqProperties,
            List<Modification> changes) throws RepositoryException {

        for (RequestProperty prop : reqProperties) {
            if (prop.hasValues()) {
                // remove artificial "/" prepended to the prop path
                String relativePath = prop.getPath().substring(1);
                
                // skip jcr special properties
                if (relativePath.equals("jcr:primaryType")
                    || relativePath.equals("jcr:mixinTypes")) {
                    continue;
                }
                if (authorizable.isGroup()) {
                    if (relativePath.equals("groupId")) {
                        // skip these
                        continue;
                    }
                } else {
                    if (relativePath.equals("userId")
                        || relativePath.equals("pwd")
                        || relativePath.equals("pwdConfirm")) {
                        // skip these
                        continue;
                    }
                }
                if (prop.isFileUpload()) {
                    // don't handle files for user properties for now.
                    continue;
                    // uploadHandler.setFile(parent, prop, changes);
                } else {
                    setPropertyAsIs(session, authorizable, prop, changes);
                }
            }
        }
    }

    /**
     * set property without processing, except for type hints
     *
     * @param parent the parent node
     * @param prop the request property
     * @throws RepositoryException if a repository error occurs.
     */
    private void setPropertyAsIs(Session session, Authorizable parent,
            RequestProperty prop, List<Modification> changes)
            throws RepositoryException {

        String parentPath;
        if (parent.isGroup()) {
            parentPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_GROUP_PREFIX
                + parent.getID();
        } else {
            parentPath = AuthorizableResourceProvider.SYSTEM_USER_MANAGER_USER_PREFIX
                + parent.getID();
        }

        // no explicit typehint
        int type = PropertyType.UNDEFINED;
        if (prop.getTypeHint() != null) {
            try {
                type = PropertyType.valueFromName(prop.getTypeHint());
            } catch (Exception e) {
                // ignore
            }
        }
        // remove artificial "/" prepended to the prop path
        String relativePath = prop.getPath().substring(1);

        String[] values = prop.getStringValues();
        if (values == null) {
            // remove property
            boolean removedProp = removePropertyIfExists(parent, relativePath);
            if (removedProp) {
                changes.add(Modification.onDeleted(parentPath + "/"
                    + relativePath));
            }
        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            if (parent.hasProperty(relativePath)) {
                Value val = session.getValueFactory().createValue("");
                parent.setProperty(relativePath, val);
                changes.add(Modification.onModified(parentPath + "/"
                    + relativePath));
            }
        } else if (values.length == 1) {
            boolean removedProp = removePropertyIfExists(parent, relativePath);
            // if the provided value is the empty string, we don't have to do
            // anything.
            if (values[0].length() == 0) {
                if (removedProp) {
                    changes.add(Modification.onDeleted(parentPath + "/"
                        + relativePath));
                }
            } else {
                // modify property
                if (type == PropertyType.DATE) {
                    // try conversion
                    Calendar c = dateParser.parse(values[0]);
                    if (c != null) {
                        if (prop.hasMultiValueTypeHint()) {
                            final Value[] array = new Value[1];
                            array[0] = session.getValueFactory().createValue(c);
                            parent.setProperty(relativePath, array);
                            changes.add(Modification.onModified(parentPath
                                + "/" + relativePath));
                        } else {
                            Value cVal = session.getValueFactory().createValue(
                                c);
                            parent.setProperty(prop.getName(), cVal);
                            changes.add(Modification.onModified(parentPath
                                + "/" + relativePath));
                        }
                        return;
                    }
                    // fall back to default behaviour
                }
                if (type == PropertyType.UNDEFINED) {
                    Value val = session.getValueFactory().createValue(
                        values[0], PropertyType.STRING);
                    parent.setProperty(relativePath, val);
                } else {
                    if (prop.hasMultiValueTypeHint()) {
                        final Value[] array = new Value[1];
                        array[0] = session.getValueFactory().createValue(
                            values[0], type);
                        parent.setProperty(relativePath, array);
                    } else {
                        Value val = session.getValueFactory().createValue(
                            values[0], type);
                        parent.setProperty(relativePath, val);
                    }
                }
                changes.add(Modification.onModified(parentPath + "/"
                    + relativePath));
            }
        } else {
            removePropertyIfExists(parent, relativePath);
            if (type == PropertyType.DATE) {
                // try conversion
                ValueFactory valFac = session.getValueFactory();
                Value[] c = dateParser.parse(values, valFac);
                if (c != null) {
                    parent.setProperty(relativePath, c);
                    changes.add(Modification.onModified(parentPath + "/"
                        + relativePath));
                    return;
                }
                // fall back to default behaviour
            }

            Value[] vals = new Value[values.length];
            if (type == PropertyType.UNDEFINED) {
                for (int i = 0; i < values.length; i++) {
                    vals[i] = session.getValueFactory().createValue(values[i]);
                }
            } else {
                for (int i = 0; i < values.length; i++) {
                    vals[i] = session.getValueFactory().createValue(values[i],
                        type);
                }
            }
            parent.setProperty(relativePath, vals);
            changes.add(Modification.onModified(parentPath + "/"
                + relativePath));
        }

    }

    /**
     * Removes the property with the given name from the authorizable if it
     * exists.
     *
     * @param authorizable the <code>org.apache.jackrabbit.api.security.user.Authorizable</code>
     *         that should have properties deleted.
     * @param path the path of the property to remove
     * @return path of the property that was removed or <code>null</code> if it
     *         was not removed
     * @throws RepositoryException if a repository error occurs.
     */
    private boolean removePropertyIfExists(Authorizable authorizable, String path)
            throws RepositoryException {
        if (authorizable.getProperty(path) != null) {
            authorizable.removeProperty(path);
            return true;
        }
        return false;
    }

    // ------ These methods were copied from AbstractSlingPostOperation ------

    /**
     * @param name the name
     * @return <code>true</code> if the <code>name</code> starts with either of
     * the prefixes {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT} and 
     * {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE}
     */
    protected boolean hasItemPathPrefix(String name) {
        return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
    }

    /**
     * @param properties the request parameters
     * @return {@code true} if any of the request parameters starts with
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT}.
     * In this case only parameters starting with either of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE} are
     * considered as providing content to be stored. Otherwise all parameters
     * not starting with the command prefix <code>:</code> are considered as
     * parameters to be stored.
     */
    protected final boolean requireItemPathPrefix(
            Map<String, ?> properties) {

        boolean requirePrefix = false;

        Iterator<String> iterator = properties.keySet().iterator();
        while (iterator.hasNext() && !requirePrefix) {
            String name = iterator.next();
            requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
        }

        return requirePrefix;
    }


    protected String convertToString(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String) {
            return (String)obj;
        } else if (obj instanceof String[]) {
            String [] values = (String[])obj;
            if (values.length > 0) {
                return values[0];
            }
            return null;
        } else if (obj instanceof RequestParameter) {
            ((RequestParameter)obj).getString();
        } else if (obj instanceof RequestParameter[]) {
            RequestParameter[] values = (RequestParameter[])obj;
            if (values.length > 0) {
                return values[0].getString();
            }
            return null;
        }
        return null;
    }

    protected String[] convertToStringArray(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String) {
            return new String[] {(String)obj};
        } else if (obj instanceof String[]) {
            return (String[])obj;
        } else if (obj instanceof RequestParameter) {
            return new String[] {((RequestParameter)obj).getString()};
        } else if (obj instanceof RequestParameter[]) {
            RequestParameter[] values = (RequestParameter[])obj;
            String [] strValues = new String[values.length];
            for (int i=0; i < values.length; i++) {
                strValues[i] = values[i].getString();
            }
            return strValues;
        }
        return null;
    }

    protected RequestParameter[] convertToRequestParameterArray(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String) {
            return new RequestParameter[] {
                new RequestParameterImpl((String)obj, null)
            };
        } else if (obj instanceof String[]) {
            String [] strValues = (String[])obj;
            RequestParameter [] values = new RequestParameter[strValues.length];
            for (int i=0; i < strValues.length; i++) {
                values[i] = new RequestParameterImpl(strValues[i], null);
            }
            return values;
        } else if (obj instanceof RequestParameter) {
            return new RequestParameter[] {(RequestParameter)obj};
        } else if (obj instanceof RequestParameter[]) {
            return (RequestParameter[])obj;
        }
        return null;
    }

    static class RequestParameterImpl implements RequestParameter {

        private String value;
        private String encoding;

        private byte[] content;

        RequestParameterImpl(String value, String encoding) {
            this.encoding = encoding;
            this.value = value;
            this.content = null;
        }

        String getEncoding() {
            return this.encoding;
        }

        void setEncoding(String encoding) {
            // recode this parameter by encoding the string with the current
            // encoding and decode the bytes with the encoding
            try {
                this.value = getString(encoding);
            } catch (UnsupportedEncodingException uee) {
                throw new SlingUnsupportedEncodingException(uee);
            }
            this.encoding = encoding;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#get()
         */
        public byte[] get() {
            if (content == null) {
                try {
                    content = getString().getBytes(getEncoding());
                } catch (Exception e) {
                    // UnsupportedEncodingException, IllegalArgumentException
                    content = getString().getBytes();
                }
            }
            return content;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getContentType()
         */
        public String getContentType() {
            // none known for www-form-encoded parameters
            return null;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getInputStream()
         */
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.get());
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getFileName()
         */
        public String getFileName() {
            // no original file name
            return null;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getSize()
         */
        public long getSize() {
            return this.get().length;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getString()
         */
        public String getString() {
            return value;
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#getString(java.lang.String)
         */
        public String getString(String encoding)
                throws UnsupportedEncodingException {
            return new String(this.get(), encoding);
        }

        /**
         * @see org.apache.sling.api.request.RequestParameter#isFormField()
         */
        public boolean isFormField() {
            // www-form-encoded are always form fields
            return true;
        }

        @Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
        public String toString() {
            return this.getString();
        }
    }

    static class SlingUnsupportedEncodingException extends SlingIOException {

        private static final long serialVersionUID = -4482276105859280247L;

        SlingUnsupportedEncodingException(UnsupportedEncodingException uee) {
            super(uee);
        }

    }

}
