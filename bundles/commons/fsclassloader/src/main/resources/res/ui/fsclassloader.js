/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
(function () {
    'use strict';

    $(document).ready(function () {
        toggleButton(hasClasses());

        $('#clear').on('click', function (e) {
            e.preventDefault();
            var classes = hasClasses();
            if (classes) {
                clearCache();
                toggleButton(classes);
            }

        });
    });

    function clearCache() {
        $.ajax({
            type: 'POST',
            data: {'clear': true},
            dataType: 'json',
            global: false
        }).success(
            function () {
                window.location.reload();
            }
        ).fail(
            function (jqXHR) {
                var response, message;
                try {
                    response = JSON.parse(jqXHR.responseText);
                    message = response.message;
                } catch (err) {
                    // do nothing
                }
                if (message) {
                    alert('Error: ' + message);
                } else {
                    alert('An unknown error was encountered. Please check the server logs.');
                }
            }
        );
    }

    function hasClasses () {
        return $('table.fsclassloader-has-classes').length > 0;
    }

    function toggleButton(toggle) {
        $('#clear').attr('disabled', !toggle);
    }
})();
