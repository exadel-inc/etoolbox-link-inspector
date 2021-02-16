/**
 * AEMBox LinkChecker clientlib.
 * Post-load utility to check the updates, happened in the report list.
 */
(function (window, document, $, Granite) {
    "use strict";

    var CHECK_URL = '/content/aembox-linkchecker/servlet/pendingGenerationCheck';
    var UPDATE_MSG = Granite.I18n.get('Some links were updated. Changes will be reflected in the report after data feed regeneration');

    $(document).ready(function () {
        $.ajax({
            url: CHECK_URL,
            type: 'POST',
            success: function (data, textStatus, xhr) {
                if (xhr.status === 200) {
                    var alertPopup = new Coral.Alert().set({
                        header: {
                            innerHTML: 'INFO'
                        },
                        content: {
                            textContent: UPDATE_MSG
                        }
                    });
                    alertPopup.classList.add('elc-coral-alert');
                    document.body.append(alertPopup);
                }
            }
        });
    });
})(window, document, Granite.$, Granite);