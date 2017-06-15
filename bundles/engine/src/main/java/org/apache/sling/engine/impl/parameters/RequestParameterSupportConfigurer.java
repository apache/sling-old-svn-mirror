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

import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        name = RequestParameterSupportConfigurer.PID)
@Designate(ocd=RequestParameterSupportConfigurer.Config.class)
public class RequestParameterSupportConfigurer {

    @ObjectClassDefinition(name = "Apache Sling Request Parameter Handling",
        description = "Configures Sling's request parameter handling.")
    public @interface Config {
        @AttributeDefinition(
                name = "Default Parameter Encoding",
                description = "The default request parameter encoding used to decode request "
                    + "parameters into strings. If this property is not set the default encoding "
                    + "is 'ISO-8859-1' as mandated by the Servlet API spec. This default encoding "
                    + "is used if the '_charset_' request parameter is not set to another "
                    + "(supported) character encoding. Applications being sure to always use the "
                    + "same encoding (e.g. UTF-8) can set this default here and may omit the "
                    + "'_charset_' request parameter")
        String sling_default_parameter_encoding() default Util.ENCODING_DIRECT;

        @AttributeDefinition(
                name = "Maximum POST Parameters",
                description = "The maximum number of parameters supported. To prevent a DOS-style attack with an "
                    + "overrunning number of parameters the number of parameters supported can be limited. This "
                    + "includes all of the query string as well as application/x-www-form-urlencoded and "
                    + "multipart/form-data parameters. The default value is " + ParameterMap.DEFAULT_MAX_PARAMS + ".")
       int sling_default_max_parameters() default ParameterMap.DEFAULT_MAX_PARAMS;

        @AttributeDefinition(
                name = "Temporary File Location",
                description = "The temporary directory where uploaded files are written to disk. The default is "
                    + "null, which means the directory given by the 'java.io.tmpdir' system property.")
        String file_location();

        @AttributeDefinition(
                name = "File Save Threshold",
                description = "The size threshold after which the file will be written to disk. The default is 256KB.")
        int file_threshold() default 256000;

        @AttributeDefinition(
                name = "Maximum File Size",
                description = "The maximum size allowed for uploaded files. The default is -1, which means unlimited.")
        long file_max() default -1;

        @AttributeDefinition(
                name = "Maximum Request Size",
                description = "The maximum size allowed for multipart/form-data requests. The default is -1, which means unlimited.")
        long request_max() default -1;

        @AttributeDefinition(
                name = "Check Additional Parameters",
                description = "Enable this if you want to include request parameters added through the container, e.g through a valve.")
        boolean sling_default_parameter_checkForAdditionalContainerParameters() default false;
    }
    static final String PID = "org.apache.sling.engine.parameters";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(PID);



    @Reference
    private SlingSettingsService settignsService;

    @Activate
    private void configure(final Config config) {
        final String fixEncoding = config.sling_default_parameter_encoding();
        final int maxParams = config.sling_default_max_parameters();
        final long maxRequestSize = config.request_max();
        final String fileLocation = getFileLocation(config.file_location());
        final long maxFileSize = config.file_max();
        final int fileSizeThreshold = config.file_threshold();
        final boolean checkAddParameters = config.sling_default_parameter_checkForAdditionalContainerParameters();

        if (log.isInfoEnabled()) {
            log.info("Default Character Encoding: {}", fixEncoding);
            log.info("Parameter Number Limit: {}", (maxParams < 0) ? "unlimited" : maxParams);
            log.info("Maximum Request Size: {}", (maxParams < 0) ? "unlimited" : maxRequestSize);
            log.info("Temporary File Location: {}", fileLocation);
            log.info("Maximum File Size: {}", maxFileSize);
            log.info("Tempory File Creation Threshold: {}", fileSizeThreshold);
            log.info("Check for additional container parameters: {}", checkAddParameters);
        }

        Util.setDefaultFixEncoding(fixEncoding);
        ParameterMap.setMaxParameters(maxParams);
        ParameterSupport.configure(maxRequestSize, fileLocation, maxFileSize,
                fileSizeThreshold, checkAddParameters);
    }

    private String getFileLocation(String fileLocation) {
        if (fileLocation != null) {
            File file = new File(fileLocation);
            if (!file.isAbsolute()) {
                file = new File(this.settignsService.getSlingHomePath(), fileLocation);
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
