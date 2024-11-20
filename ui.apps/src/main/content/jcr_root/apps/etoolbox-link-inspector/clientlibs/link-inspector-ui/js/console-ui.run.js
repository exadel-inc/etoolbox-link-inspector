/*
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

/**
 * EToolbox Link Inspector clientlib.
 * Logic for running a manual scan
 */
(function (window, document, $, Granite) {
    'use strict';

    const TRIGGER_DATA_FEED_GENERATION = '/content/etoolbox/contractor/servlet/task';
    const ui = $(window).adaptTo('foundation-ui');

    Coral.commons.ready($('body'), onReady);
    $(document).on('click', '#scan', onScanClicked);

    function onReady() {
        $('#wait').on('contractor-ticker:end', function () {
            ui.alert(Granite.I18n.get('Success'), Granite.I18n.get('Data feed generation completed successfully'), 'success');
            setTimeout(() => window.location.reload(), 1000);
        });
    }

    function onScanClicked() {
        const formData = new FormData();
        formData.append('topic', 'etoolbox/link-inspector/job/datafeed/generate');
        formData.append('exclusive', 'true');
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false
        }).error(function (e) {
            ui.notify(Granite.I18n.get('Error'), e.responseText || e.statusText, 'error');
        });
    }
})(window, document, Granite.$, Granite);