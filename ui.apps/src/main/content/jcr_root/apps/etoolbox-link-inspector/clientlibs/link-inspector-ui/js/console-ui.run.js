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
(function (document, $, Granite) {
    'use strict';

    const TRIGGER_DATA_FEED_GENERATION = '/content/etoolbox-link-inspector/servlet/triggerDataFeedGeneration';
    const CHECK_JOB_STATUS = '/content/etoolbox-link-inspector/servlet/jobStatus';
    const RUN_REPORT_MSG = Granite.I18n.get('Generation of new report has started.');
    const JOB_IN_PROGRESS_MESSAGE = Granite.I18n.get('Generation of new report in progress.');

    function addPopup(type, message) {
         const alertPopup = new Coral.Alert().set({
             variant: type,
             header: {
                 innerHTML: 'INFO'
             },
             content: {
                 textContent: message
             }
         });
         alertPopup.classList.add('elc-coral-alert');
         document.body.append(alertPopup);
    }

    function jobIsActive() {
        let isActive = false;
        $.ajax({
            url: CHECK_JOB_STATUS,
            type: 'GET',
            async: false,
            success: function (data) {
                isActive = data && data.status && data.status == 'ACTIVE' | 'QUEUED';
            }
        });
        return isActive;
    }

    function onRunAction() {
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'GET',
            success: function (data, textStatus, xhr) {
                if (xhr.status === 200) {
                    addPopup('success', RUN_REPORT_MSG)
                }
            }
        });
    }

    // INIT
    $(document).ready(function () {
        $('.elc-run-new-report-button').click(function (e) {
            e.preventDefault();
            jobIsActive() ? addPopup('info', JOB_IN_PROGRESS_MESSAGE) : onRunAction();
        });
    });
})(document, Granite.$, Granite);