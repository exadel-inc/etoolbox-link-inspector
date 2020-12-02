/**
 * Exadel LinkChecker clientlib.
 * "Replace By Pattern" action definition.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var CANCEL_LABEL = Granite.I18n.get('Cancel');
    var REPLACE_LABEL = Granite.I18n.get('Replace By Pattern');
    var REPLACE_BUTTON_LABEL = Granite.I18n.get('Replace');
    var PATTERN_LABEL = Granite.I18n.get('Please enter the regex pattern to be replaced');
    var REPLACEMENT_LINK_LABEL = Granite.I18n.get('Please enter the replacement');
    var BACKUP_CHECKBOX_LABEL = Granite.I18n.get('Backup before replacement');
    var CSV_OUT_CHECKBOX_LABEL = Granite.I18n.get('Download CSV with updated items');
    var REPLACEMENT_DESCRIPTION = Granite.I18n.get('* Replacement will be applied within the detected broken links scope');
    var REPLACEMENT_ACL_DESCRIPTION = Granite.I18n.get('** User should have sufficient read/write permissions in order to complete replacement successfully and create the backup package');
    var VALIDATION_MSG = Granite.I18n.get('Replacement can\'t be the same as pattern');

    var PROCESSING_ERROR_MSG = 'Failed to replace by pattern<br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var PROCESSING_SUCCESS_MSG = 'Replacement completed<br/><br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var PROCESSING_NOT_FOUND_MSG = 'Broken links containing the pattern <b>{{pattern}}</b> were not found or user has insufficient permissions to process them';
    var PROCESSING_IDENTICAL_MSG = 'The pattern <b>{{pattern}}</b> is equal to the replacement value, no processing was done';

    var REPLACE_BY_PATTERN_COMMAND = '/content/exadel-linkchecker/servlet/replaceByPattern';
    var ACL_CHECK_COMMAND = '/content/exadel-linkchecker/servlet/aclCheck';
    var READ_PERMISSIONS = 'read';

    var currentDate = Date.now();
    var CSV_OUTPUT_FILENAME = `replace_by_pattern_${currentDate}.csv`;

    /** Root action handler */
    function onFixAction(name, el, config, collection, selections) {
        showConfirmationModal().then(function (data) {
            var replacementList = [{
                pattern: data.pattern,
                replacement: data.replacement,
                isBackup: data.isBackup,
                isOutputAsCsv: data.isOutputAsCsv
            }].filter(function (item) {
                return item.pattern && item.replacement && item.pattern !== item.replacement;
            });
            ELC.bulkLinksUpdate(replacementList, buildReplaceRequest);
        });
    }

    function buildReplaceRequest(item, logger) {
        return function () {
            return $.ajax({
                url: REPLACE_BY_PATTERN_COMMAND,
                type: "POST",
                data: $.extend({
                    _charset_: "UTF-8",
                    cmd: "replaceByPattern"
                }, item)
            }).fail(function (xhr, status, error) {
                if (xhr.status === 500) {
                    logger.log(ELC.format("Replacement was interrupted due to the <b>error</b> occurred during persisting changes. Please see logs for more details", item), false);
                } else if (xhr.status === 403) {
                    logger.log(ELC.format("Failed to build the backup package. Possible reasons: lack of permissions, please see logs for more details.<br/><b>No replacement was applied</b>", item), false);
                } else {
                    logger.log(ELC.format(PROCESSING_ERROR_MSG, item), false);
                }
            }).done(function (data, textStatus, xhr) {
                if (xhr.status === 202) {
                    logger.log(ELC.format(PROCESSING_IDENTICAL_MSG, item), false);
                } else if (xhr.status === 204) {
                    logger.log(ELC.format(PROCESSING_NOT_FOUND_MSG, item), false);
                } else {
                    if (xhr.getResponseHeader("Content-disposition") && data) {
                        downloadCsvOutput(data);
                    }
                    logger.log(ELC.format(PROCESSING_SUCCESS_MSG, item), false);
                }
            });
        };
    }

    function downloadCsvOutput(data) {
        const blob = new Blob([data], {type: 'text/csv'});
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = CSV_OUTPUT_FILENAME;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }

    // Confirmation dialog common methods
    function showConfirmationModal() {
        var deferred = $.Deferred();

        var el = ELC.getSharableDlg();
        el.variant = 'notice';
        el.header.textContent = REPLACE_LABEL;
        el.footer.innerHTML = ''; // Clean content
        el.content.innerHTML = ''; // Clean content

        var $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        var $updateBtn = $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(REPLACE_BUTTON_LABEL);
        $cancelBtn.appendTo(el.footer);
        $updateBtn.appendTo(el.footer);

        // Pattern input group
        var $patternTextField =
            $('<input is="coral-textfield" class="elc-pattern-input" name="pattern" value="" required>');
        $('<p>').text(PATTERN_LABEL).appendTo(el.content);
        $patternTextField.appendTo(el.content);

        // Replacement input group
        var $replacementTextField =
            $('<input is="coral-textfield" class="elc-replacement-input" name="replacement" value="" required>');
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(el.content);
        $replacementTextField.appendTo(el.content);

        // Backup checkbox group
        var $isBackupCheckbox = $('<coral-checkbox name="isBackup">').text(BACKUP_CHECKBOX_LABEL);
        $isBackupCheckbox.appendTo(el.content);

        // CSV output checkbox group
        var $isCsvOutputCheckbox = $('<coral-checkbox name="isOutputAsCsv">').text(CSV_OUT_CHECKBOX_LABEL);
        $isCsvOutputCheckbox.appendTo(el.content);

        $('<p>').append($('<i>').text(REPLACEMENT_DESCRIPTION)).appendTo(el.content);
        $('<p>').append($('<i>').text(REPLACEMENT_ACL_DESCRIPTION)).appendTo(el.content);

        function onValidate() {
            var replVal = $replacementTextField.val();
            var patternVal = $patternTextField.val();
            $replacementTextField.each(function () {
                this.setCustomValidity(replVal === patternVal ? VALIDATION_MSG : '');
            });
            $updateBtn.attr('disabled', !replVal || !patternVal || replVal === patternVal);
        }
        /** @param {Event} e */
        function onResolve(e) {
            var data = {
                pattern: $patternTextField.val(),
                replacement: $replacementTextField.val(),
                isBackup: $isBackupCheckbox.prop("checked"),
                isOutputAsCsv: $isCsvOutputCheckbox.prop("checked")
            }
            deferred.resolve(data);
        }

        el.on('change', 'input', onValidate);
        el.on('click', '[data-dialog-action]', onResolve);
        el.on('coral-overlay:close', function () {
            el.off('change', 'input', onValidate);
            el.off('click', '[data-dialog-action]', onResolve);
            deferred.reject();
        });

        el.show();
        onValidate();

        return deferred.promise();
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.exadel.linkchecker.action.replace-by-pattern",
        handler: onFixAction
    });

    // ACL check
    $(document).ready(function () {
        $.ajax({
            url: ACL_CHECK_COMMAND,
            type: 'POST',
            data: {
                _charset_: "UTF-8",
                path: REPLACE_BY_PATTERN_COMMAND,
                permissions: READ_PERMISSIONS
            },
            success: function (data) {
                if (data && data.hasPermissions) {
                    $('#elc-replace-by-pattern').prop('disabled', false);
                }
            }
        });
    });
})(window, document, Granite.$, Granite.ELC, Granite);