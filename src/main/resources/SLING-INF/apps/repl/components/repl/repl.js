/*******************************************************************************
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
 ******************************************************************************/
use(function () {

    var slingSettings = sling.getService(Packages.org.apache.sling.settings.SlingSettingsService);
    var CLASS_ROOT_FOLDER  = '/var/classes/' + slingSettings.getSlingId() + '/sightly';
    var COMPONENT_PATH     = '/apps/repl/components/repl';
    var JAVA_TEMPLATE_FILE = 'SightlyJava_template.java';

    // Recursively walks down the given path until it finds an apps folder, then returns the full path of the Java compiled template file.
    function getAppsPath(res) {
        return res.getChildren().then(function (children) {
            var length = children.length;

            // Let's see if one of the children is the apps folder.
            for (var i = 0; i < length; i++) {
                if (children[i].name === 'apps') {
                    return res.path + COMPONENT_PATH + '/' + JAVA_TEMPLATE_FILE;
                }
            }

            // If apps wasn't found but there's only one child folder, then let's recrusively walk that one down.
            if (length === 1) {
                return getAppsPath(children[0]);
            }
        });
    }

    return {
        classPath: sightly.resource.resolve(CLASS_ROOT_FOLDER).then(getAppsPath)
    };

});
