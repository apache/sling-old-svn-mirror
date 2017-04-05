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
/* global jQuery, ace, document, setTimeout, clearTimeout, console */
jQuery(function ($) {

    'use strict';

    var hash = window.location.hash;
    var currentState = hash ? hash.substr(1) : 'source';

    // Limits the number of times the function gets called for event handlers
    function debounce(fn, delay) {
        var timer = null;

        return function () {
            var context = this;
            var args = arguments;

            clearTimeout(timer);
            timer = setTimeout(function () {
                fn.apply(context, args);
            }, delay);
        };
    }

    /**
     * The editor object, it will take it's configuration from following attributes:
     * id: The unique identifier of the editor (will be used as key in the Editor.all map object).
     * data-src: The URL from which the editor's content has to be loaded.
     * data-mode: The ACE editor's language mode.
     * data-writeable: Boolean attribute to make the editor persisted to the data-src location.
     */
    function Editor(DOMElement, reloadOutputCallback) {
        var that = this;
        var element = $(DOMElement);
        var editor = ace.edit(DOMElement);
        var mode = element.data('mode');
        var url = element.data('src');
        var isWriteable = element.is('[data-writeable]');

        function attachSaveHandler() {
            if (isWriteable) {
                editor.session.on('change', debounce(function () {
                    that.saveChanges();
                }), 500);
            }
        }

        function init() {
            Editor.all[element.attr('id')] = that;

            editor.renderer.setShowGutter(false);
            editor.setHighlightActiveLine(false);
            editor.setShowPrintMargin(false);
            editor.setReadOnly(!isWriteable);
            editor.session.setUseWorker(false);
            editor.session.setMode(mode);

            if (element.is(':visible')) {
                that.loadContent(attachSaveHandler);
            } else {
                attachSaveHandler();
            }
        }

        that.saveChanges = function (cb) {
            if (isWriteable) {
                $.ajax({
                    url: url,
                    type: 'PUT',
                    data: editor.getValue(),
                    contentType: 'plain/text',
                    success: reloadOutputCallback,
                    complete: cb
                });
            }
        };

        that.loadContent = function (cb) {
            $.ajax(url, {
                type: 'GET',
                dataType: 'text',
                cache: false,
                processData: false,
                success: function (data) {
                    editor.setValue(data);
                    editor.clearSelection();
                },
                error: function (req, textStatus, message) {
                    editor.setValue(req.responseText);
                    editor.clearSelection();
                    console.error(message);
                },
                complete: cb
            });
        };

        init();
    }

    // A map of all the editors, the id attribute of their parent DOM element is used as key.
    Editor.all = {};

    // Refreshes the output after changes were made
    function reloadOutput() {
        if (Editor.all[currentState] !== undefined) {
            Editor.all[currentState].loadContent();
        } else {
            document.getElementsByTagName('iframe')[0].contentDocument.location.reload(true);
        }
    }

    function init() {
        // Setup editors
        $('.editor').each(function () {
            new Editor(this, reloadOutput);
        });

        // Setup output tabs
        var allTargets = $('.output-view');
        $('a[data-toggle=tab]').each(function () {
            var link = $(this),
                href = link.attr('href'),
                target = allTargets.filter(href),
                state = target.attr('id');

            link.click(function () {
                currentState = state;
                allTargets.addClass('hidden');
                target.removeClass('hidden');
                reloadOutput();
                window.location = href;
            });
        });

        $(window).on('hashchange', function () {
            hash = window.location.hash;
            currentState = hash ? hash.substr(1) : 'source';
            $('a[href=#' + currentState + ']').click();
        });
    }

    init();
    $('a[href=#' + currentState + ']').click();

});
