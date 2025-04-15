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
 * Job status check and run functionality.
 */
(function (document, $) {
    'use strict';

    const TRIGGER_DATA_FEED_GENERATION = '/content/etoolbox/link-inspector/servlet/triggerDataFeedGeneration';
    const CHECK_JOB_STATUS = '/content/etoolbox/link-inspector/servlet/jobStatus';
    let $jobStatusContainer = null;

    function jobIsActive(callback) {
        $.ajax({
            url: CHECK_JOB_STATUS,
            type: 'GET',
            async: false,
            success: function (data) {
                if (data && $.inArray(data.status ,['ACTIVE','QUEUED','GIVEN_UP']) !== -1) {
                    callback();
                }
            }
        });
    }

    function onRunAction(callback) {
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'GET',
            success: callback
        });
    }

    function addJobStatusMessage($popover) {
        $jobStatusContainer = $('<p class="u-coral-margin"></p>').text('Job status: ');
        $('<b>...in progress</b>').appendTo($jobStatusContainer);
        $('<br/>').appendTo($jobStatusContainer);
        $('<span>Scan may take some time to complete</span>').appendTo($jobStatusContainer);;
        $popover.find('coral-popover-content').append($jobStatusContainer);
    }

    function createRunJobMessage($popover) {
        const $container = $('<p class="u-coral-margin"></p>');
        $('<button class="elc-run-button">Scan</button>').appendTo($container);
        $popover.find('coral-popover-content').append($container);
    }

    function beforeOpenPopover(e) {
        !$jobStatusContainer && jobIsActive(function () {addJobStatusMessage($(e.currentTarget));});
    }

    function beforeClosePopover(e) {
        removeJobStatusMessage();
    }

    function removeJobStatusMessage() {
        $jobStatusContainer && $jobStatusContainer.remove() && ($jobStatusContainer = null);
    }

    $(document).on('coral-overlay:beforeopen', '.elc-coral-popover', beforeOpenPopover);

    $(document).on('coral-overlay:beforeclose', '.elc-coral-popover', beforeClosePopover);

    $(document).on('click', '#scan', onScanClicked);
    function onScanClicked() {
        const formData = new FormData();
        const dialog = $('#elc-scan-now-dialog');
        formData.append('topic', 'etoolbox/link-inspector/job/datafeed/generate');
        formData.append('exclusive', 'true');
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'GET',
            success: function(){
                dialog.trigger('hide')
                $('#statsPopover').attr('disabled', false);
            },
            error: function(e) {
                ui.notify('Error', e.responseText || e.statusText, 'error');
            }
        });
    }

})(document, Granite.$);