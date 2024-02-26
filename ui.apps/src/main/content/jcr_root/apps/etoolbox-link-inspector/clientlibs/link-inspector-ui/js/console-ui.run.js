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
 * Run new report action.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var TRIGGER_DATA_FEED_GENERATION = '/content/etoolbox-link-inspector/servlet/triggerDataFeedGeneration';
    var RUN_REPORT_MSG = Granite.I18n.get('Generation of new report has started.');

    function onRunAction() {
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'GET',
            success: function (data, textStatus, xhr) {
                if (xhr.status === 200) {
                    var alertPopup = new Coral.Alert().set({
                        header: {
                            innerHTML: 'INFO'
                        },
                        content: {
                            textContent: RUN_REPORT_MSG
                        }
                    });
                    alertPopup.classList.add('elc-coral-alert');
                    document.body.append(alertPopup);
                }
            }
        });
    }

    // INIT
    $(document).ready(function () {
        $('.elc-run-new-report-button').click(function (e) {
            e.preventDefault();
            onRunAction();
        });
    });
})(window, document, Granite.$, Granite.ELC, Granite);