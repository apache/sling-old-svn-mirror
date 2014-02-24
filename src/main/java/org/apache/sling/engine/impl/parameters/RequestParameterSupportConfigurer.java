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
package org.apache.sling.engine.impl.parameters;

import java.io.File;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        metatype = true,
        name = RequestParameterSupportConfigurer.PID,
        label = "Apache Sling Request Parameter Handling",
        description = "Configures Sling's request parameter handling.")
@Reference(
        name = "SlingSetting",
        referenceInterface = SlingSettingsService.class,
        policy = ReferencePolicy.DYNAMIC,
        strategy = ReferenceStrategy.LOOKUP)
public class RequestParameterSupportConfigurer {

    static final String PID = "org.apache.sling.engine.parameters";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(PID);

    @Property(
            value = Util.ENCODING_DIRECT,
            label = "Default Parameter Encoding",
            description = "The default request parameter encoding used to decode request "
                + "parameters into strings. If this property is not set the default encoding "
                + "is 'ISO-8859-1' as mandated by the Servlet API spec. This default encoding "
                + "is used if the '_charset_' request parameter is not set to another "
                + "(supported) character encoding. Applications being sure to always use the "
                + "same encoding (e.g. UTF-8) can set this default here and may omit the "
                + "'_charset_' request parameter")
    private static final String PROP_FIX_ENCODING = "sling.default.parameter.encoding";

    @Property(
            intValue = ParameterMap.DEFAULT_MAX_PARAMS,
            label = "Maximum POST Parameters",
            description = "The maximum number of parameters supported. To prevent a DOS-style attack with an "
                + "overrunning number of parameters the number of parameters supported can be limited. This "
                + "includes all of the query string as well as application/x-www-form-urlencoded and "
                + "multipart/form-data parameters. The default value is " + ParameterMap.DEFAULT_MAX_PARAMS + ".")
    private static final String PROP_MAX_PARAMS = "sling.default.max.parameters";

    @Property(
            label = "Temporary File Location",
            description = "The size threshold after which the file will be written to disk. The default is "
                + "null, which means the directory given by the 'java.io.tmpdir' system property.")
    private static final String PROP_FILE_LOCATION = "file.location";

    @Property(
            longValue = 256000,
            label = "File Save Threshold",
            description = "The size threshold after which the file will be written to disk. The default is 256KB.")
    private static final String PROP_FILE_SIZE_THRESHOLD = "file.threshold";

    @Property(
            longValue = -1,
            label = "Maximum File Size",
            description = "The maximum size allowed for uploaded files. The default is -1, which means unlimited.")
    private static final String PROP_FILE_SIZE_MAX = "file.max";

    @Property(
            longValue = -1,
            label = "Maximum Request Size",
            description = "The maximum size allowed for multipart/form-data requests. The default is -1, which means unlimited.")
    private static final String PROP_MAX_REQUEST_SIZE = "request.max";

    @Activate
    @Deactivate
    private void configure(ComponentContext context) {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = context.getProperties();

        final String fixEncoding = PropertiesUtil.toString(props.get(PROP_FIX_ENCODING), Util.ENCODING_DIRECT);
        final int maxParams = PropertiesUtil.toInteger(props.get(PROP_MAX_PARAMS), ParameterMap.DEFAULT_MAX_PARAMS);
        final long maxRequestSize = PropertiesUtil.toLong(props.get(PROP_MAX_REQUEST_SIZE), -1);
        final String fileLocation = getFileLocation(context,
            PropertiesUtil.toString(props.get(PROP_FILE_LOCATION), null));
        final long maxFileSize = PropertiesUtil.toLong(props.get(PROP_FILE_SIZE_MAX), -1);
        final int fileSizeThreshold = PropertiesUtil.toInteger(props.get(PROP_FILE_SIZE_THRESHOLD), -1);

        if (log.isInfoEnabled()) {
            log.info("Default Character Encoding: {}", fixEncoding);
            log.info("Parameter Number Limit: {}", (maxParams < 0) ? "unlimited" : maxParams);
            log.info("Maximum Request Size: {}", (maxParams < 0) ? "unlimited" : maxRequestSize);
            log.info("Temporary File Location: {}", fileLocation);
            log.info("Maximum File Size: {}", maxFileSize);
            log.info("Tempory File Creation Threshold: {}", fileSizeThreshold);
        }

        Util.setDefaultFixEncoding(fixEncoding);
        ParameterMap.setMaxParameters(maxParams);
        ParameterSupport.configure(maxRequestSize, fileLocation, maxFileSize, fileSizeThreshold);
    }

    private String getFileLocation(final ComponentContext context, String fileLocation) {
        if (fileLocation != null) {
            File file = new File(fileLocation);
            if (!file.isAbsolute()) {
                final SlingSettingsService settings = (SlingSettingsService) context.locateService("SlingSettings");
                file = new File(settings.getSlingHomePath(), fileLocation);
                fileLocation = file.getAbsolutePath();
            }
            if (file.exists()) {
                if (!file.isDirectory()) {
                    log.error(
                        "Configured temporary file location {} exists but is not a directory; using java.io.tmpdir instead",
                        fileLocation);
                    fileLocation = null;
                }
            } else {
                if (!file.mkdirs()) {
                    log.error("Cannot create temporary file directory {}; using java.io.tmpdir instead", fileLocation);
                    fileLocation = null;
                }
            }
        }
        return fileLocation;
    }
}
