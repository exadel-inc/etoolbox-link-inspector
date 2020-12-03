/**
 * Exadel LinkChecker clientlib.
 * Download report action.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var NO_REPORT_MSG = Granite.I18n.get('No report found');
    var REPORT_URL = '/content/exadel-linkchecker/download/report.csv';

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