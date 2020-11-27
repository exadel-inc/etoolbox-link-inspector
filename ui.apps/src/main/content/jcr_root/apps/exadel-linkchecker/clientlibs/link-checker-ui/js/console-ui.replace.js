/**
 * Exadel LinkChecker clientlib.
 * "Replace By Pattern" action definition.
 */
(function (window, document, $, Granite) {
    'use strict';

    var CLOSE_LABEL = Granite.I18n.get('Close');
    var CANCEL_LABEL = Granite.I18n.get('Cancel');
    var REPLACE_LABEL = Granite.I18n.get('Replace By Pattern');
    var REPLACE_BUTTON_LABEL = Granite.I18n.get('Replace');
    var FINISHED_LABEL = Granite.I18n.get('Finished');
    var PROCESSING_LABEL = Granite.I18n.get('Processing');
    var START_REPLACEMENT_LABEL = Granite.I18n.get('Replacement by pattern is in progress ...');
    var PATTERN_LABEL = Granite.I18n.get('Please enter the regex pattern to be replaced');
    var REPLACEMENT_LINK_LABEL = Granite.I18n.get('Please enter the replacement');
    var BACKUP_CHECKBOX_LABEL = Granite.I18n.get('Backup before replacement');
    var CSV_OUT_CHECKBOX_LABEL = Granite.I18n.get('Download CSV with updated items');
    var REPLACEMENT_DESCRIPTION = Granite.I18n.get('* Replacement will be applied within the detected broken links scope');

    var PROCESSING_ERROR_MSG = 'Failed to replace by pattern<br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var PROCESSING_SUCCESS_MSG = 'Replacement completed<br/><br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var PROCESSING_NOT_FOUND_MSG = 'Broken links containing the pattern <b>{{pattern}}</b> were not found';
    var PROCESSING_IDENTICAL_MSG = 'The pattern <b>{{pattern}}</b> is equal to the replacement value, no processing was done';

    var REPLACE_BY_PATTERN_COMMAND = '/content/exadel-linkchecker/servlet/replaceLinksByPattern';

    let currentDate = Date.now();
    var CSV_OUTPUT_FILENAME = `replace_by_pattern_${currentDate}.csv`;

    /** Root action handler */
    function onFixAction(name, el, config, collection, selections) {
        showConfirmationModal().then(function (data) {
            var replacementList = [{
                pattern: data.pattern,
                replacement: data.replacement,
                isBackup: data.isBackup,
                isOutputAsCsv: data.isOutputAsCsv
            }];
            //todo - add validation of params: pattern, replacement
            processBrokenLink(replacementList);
        });
    }

    function processBrokenLink(items) {
        var logger = createLoggerDialog(PROCESSING_LABEL, START_REPLACEMENT_LABEL);
        var requests = $.Deferred().resolve();
        requests = items.reduce(function (query, item) {
            return query.then(buildReplaceRequest(item, logger));
        }, requests);
        requests.always(function () {
            logger.finished();
            logger.dialog.on('coral-overlay:close', function () {
                $(window).adaptTo('foundation-ui').wait();
                window.location.reload();
            });
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
            }).fail(function () {
                logger.log(format(PROCESSING_ERROR_MSG, item), false);
            }).done(function (data, textStatus, xhr) {
                if (xhr.status === 202) {
                    logger.log(format(PROCESSING_IDENTICAL_MSG, item), false);
                } else if (xhr.status === 204) {
                    logger.log(format(PROCESSING_NOT_FOUND_MSG, item), false);
                } else {
                    if (xhr.getResponseHeader("Content-disposition") && data) {
                        downloadCsvOutput(data);
                    }
                    logger.log(format(PROCESSING_SUCCESS_MSG, item), false);
                }
            });
        };
    }

    function downloadCsvOutput(data) {
        const blob = new Blob([data], {type : 'text/csv'});
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = CSV_OUTPUT_FILENAME;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
    }

    // Confirmation dialog common methods
    function showConfirmationModal() {
        var deferred = $.Deferred();

        var el = getDialog();
        el.variant = 'notice';
        el.header.textContent = REPLACE_LABEL;
        el.footer.innerHTML = [
            '<button is="coral-button" variant="default" coral-close>' + CANCEL_LABEL + '</button>',
            '<button data-dialog-action is="coral-button" variant="primary" coral-close>' + REPLACE_BUTTON_LABEL + '</button>'
        ].join('');

        el.content.innerHTML = ''; // Clean content

        // Pattern input group
        var $patternTextField =
            $('<input is="coral-textfield" class="elc-pattern-input" name="pattern" value="">');
        $('<p>').text(PATTERN_LABEL).appendTo(el.content);
        $patternTextField.appendTo(el.content);

        // Replacement input group
        var $replacementTextField =
            $('<input is="coral-textfield" class="elc-replacement-input" name="replacement" value="">');
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(el.content);
        $replacementTextField.appendTo(el.content);

        // Backup checkbox group
        var $isBackupCheckbox =
            $('<coral-checkbox name="isBackup">');
        $isBackupCheckbox.text(BACKUP_CHECKBOX_LABEL).appendTo(el.content);

        // CSV output checkbox group
        var $isCsvOutputCheckbox =
            $('<coral-checkbox name="isOutputAsCsv">');
        $isCsvOutputCheckbox.text(CSV_OUT_CHECKBOX_LABEL).appendTo(el.content);

        ($('<i>').append($('<p>').text(REPLACEMENT_DESCRIPTION))).appendTo(el.content);

        var onResolve = function () {
            var data = {
                pattern: $patternTextField.val(),
                replacement: $replacementTextField.val(),
                isBackup: $isBackupCheckbox.prop("checked"),
                isOutputAsCsv: $isCsvOutputCheckbox.prop("checked")
            }
            deferred.resolve(data);
        };

        el.on('click', '[data-dialog-action]', onResolve);
        el.on('coral-overlay:close', function () {
            el.off('click', '[data-dialog-action]', onResolve);
            deferred.reject();
        });
        el.show();

        return deferred.promise();
    }

    /**
     * Create {@return ProcessLogger} wrapper
     * @return {ProcessLogger}
     *
     * @typedef ProcessLogger
     * @method finished
     * @method log
     */
    function createLoggerDialog(title, processingMsg) {
        var el = getDialog();
        el.variant = 'default';
        el.header.textContent = title;
        el.header.insertBefore(new Coral.Wait(), el.header.firstChild);
        el.footer.innerHTML = '';
        el.content.innerHTML = '';

        var processingLabel = document.createElement('p');
        processingLabel.textContent = processingMsg;
        el.content.append(processingLabel);

        document.body.appendChild(el);
        el.show();

        return {
            dialog: el,
            finished: function () {
                el.header.textContent = FINISHED_LABEL;
                processingLabel.remove();

                var closeBtn = new Coral.Button();
                closeBtn.variant = 'primary';
                closeBtn.label.textContent = CLOSE_LABEL;
                closeBtn.on('click', function () {
                    el.hide();
                });

                el.footer.appendChild(closeBtn);
            },
            log: function (message, safe) {
                var logItem = document.createElement('div');
                logItem.className = 'elc-log-item';
                logItem[safe ? 'textContent' : 'innerHTML'] = message;
                el.content.insertAdjacentElement('beforeend', logItem);
            }
        };
    }

    /**
     * @param {string} text - text to format
     * @param {object} dictionary - dictionary object to replace '{{key}}' injections
     * @return {string}
     */
    function format(text, dictionary) {
        return text.replace(/{{(\w+)}}/g, function (match, term) {
            if (term in dictionary) return String(dictionary[term]);
            return match;
        });
    }

    let sharableDialog;

    /** Common sharable dialog instance getter */
    function getDialog() {
        if (!sharableDialog) {
            sharableDialog = new Coral.Dialog().set({
                backdrop: Coral.Dialog.backdrop.STATIC,
                interaction: 'off'
            }).on('coral-overlay:close', function (e) {
                e.target.remove();
            });
            sharableDialog.classList.add('elc-dialog');
        }
        return sharableDialog;
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.exadel.linkchecker.action.replace-by-pattern",
        handler: onFixAction
    });
})(window, document, Granite.$, Granite);