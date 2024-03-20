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

    const TRIGGER_DATA_FEED_GENERATION = '/content/etoolbox-link-inspector/servlet/triggerDataFeedGeneration';
    const CHECK_JOB_STATUS = '/content/etoolbox-link-inspector/servlet/jobStatus';

    function jobIsActive() {
        let isActive = false;
        $.ajax({
            url: CHECK_JOB_STATUS,
            type: 'GET',
            async: false,
            success: function (data) {
                isActive = data && data.status && data.status == 'ACTIVE' | 'QUEUED' | 'GIVEN_UP' | 'QUEUED';
            }
        });
        return isActive;
    }

    function onRunAction(callback) {
        $.ajax({
            url: TRIGGER_DATA_FEED_GENERATION,
            type: 'GET',
            success: callback
        });
    }

    function createInProgressMessage($popover) {
        const $container = $('<p class="u-coral-margin"></p>').text('Job status: ');
        $('<b>...in progress</b>').appendTo($container);
        $('<br/>').appendTo($container);
        $('<span>search may take some time to complete</span>').appendTo($container);;
        $popover.find('coral-popover-content').append($container);
    }

    function createRunJobMessage($popover) {
        const $container = $('<p class="u-coral-margin"></p>').text('Job status: ');
        $('<b>completed.</b>').appendTo($container);
        $('<button class="elc-run-button">Run Again</button>').appendTo($container);
        $popover.find('coral-popover-content').append($container);
    }

    function beforeOpenPopover(e) {
        jobIsActive() ? createInProgressMessage($(e.currentTarget)) : createRunJobMessage($(e.currentTarget));
    }

    function beforeClosePopover(e) {
        removeLastChild($(e.currentTarget));
    }

    function removeLastChild($popover){
       const $popoverContent = $popover.find('coral-popover-content');
       $popoverContent.children().last().remove();
    }

    $(document).on('coral-overlay:beforeopen', '.elc-coral-popover', beforeOpenPopover);

    $(document).on('coral-overlay:beforeclose', '.elc-coral-popover', beforeClosePopover);

    $(document).on('click', '.elc-run-button', function() {
        onRunAction(function() {
            const $popover = $('.elc-coral-popover');
            removeLastChild($popover);
            createInProgressMessage($popover);
        });
    });

})(document, Granite.$);