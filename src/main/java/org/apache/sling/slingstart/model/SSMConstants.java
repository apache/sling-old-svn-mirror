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
package org.apache.sling.slingstart.model;


public abstract class SSMConstants {

    /** Name of the configuration containing the web.xml. */
    public static final String CFG_WEB_XML = ":web.xml";

    /** Name of the configuration for the bootstrap contents. */
    public static final String CFG_BOOTSTRAP = ":bootstrap";

    /** Unprocessed configuration values. */
    public static final String CFG_UNPROCESSED = ":rawconfig";

    /** Format of the unprocessed configuration values. */
    public static final String CFG_UNPROCESSED_FORMAT = ":rawconfig.format";

    public static final String CFG_FORMAT_FELIX_CA = "felixca";

    public static final String CFG_FORMAT_PROPERTIES = "properties";

    /** Name of the base run mode for the Sling launchpad. */
    public static final String RUN_MODE_BASE = ":base";

    /** Name of the boot run mode. */
    public static final String RUN_MODE_BOOT = ":boot";

    /** Name of the webapp run mode. */
    public static final String RUN_MODE_WEBAPP = ":webapp";

    /** Name of the standalone run mode. */
    public static final String RUN_MODE_STANDALONE = ":standalone";

}
