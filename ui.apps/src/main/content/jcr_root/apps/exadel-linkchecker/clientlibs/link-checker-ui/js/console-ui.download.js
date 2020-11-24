/**
 * Exadel LinkChecker clientlib.
 * Download report action.
 */
(function (window, document, $, Granite) {
    'use strict';

    var NO_REPORT_MSG = Granite.I18n.get('No report found');
    var REPORT_URL = '/content/exadel-linkchecker/download/report.csv';

    function onDownloadAction() {
        $.ajax({
            url: REPORT_URL,
            method: 'HEAD'
        }).done(function () {
            window.location = REPORT_URL;
        }).fail(function () {
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
        });
    }

    // INIT
    $(document).ready(function () {
        $('.elc-download-report-button').click(function (e) {
            e.preventDefault();
            onDownloadAction();
        });
    });
})(window, document, Granite.$, Granite);