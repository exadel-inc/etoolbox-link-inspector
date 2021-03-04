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
 * AEMBox LinkChecker clientlib.
 * Download report action.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var NO_REPORT_MSG = Granite.I18n.get('No report found');
    var REPORT_URL = '/content/aembox-linkchecker/download/report.csv';

    function onDownloadAction() {
        if (ELC.resourceExistCheck(REPORT_URL)) {
            window.location = REPORT_URL;
        } else {
            showWarningPopup();
        }
    }

    function showWarningPopup() {
        $('#elc-no-report-found-alert').remove();
        var alertPopup = new Coral.Alert().set({
            variant: 'warning',
            header: {
                innerHTML: 'WARNING'
            },
            content: {
                textContent: NO_REPORT_MSG
            },
            id: 'elc-no-report-found-alert'
        });
        alertPopup.classList.add('elc-coral-alert');
        document.body.append(alertPopup);
        setTimeout(function () {
            $(alertPopup).fadeOut();
        }, 2000);
    }

    // INIT
    $(document).ready(function () {
        $('.elc-download-report-button').click(function (e) {
            e.preventDefault();
            onDownloadAction();
        });
    });
})(window, document, Granite.$, Granite.ELC, Granite);