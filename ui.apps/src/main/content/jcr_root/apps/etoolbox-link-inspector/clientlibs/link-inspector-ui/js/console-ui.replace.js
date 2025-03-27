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
 * "Replace By Pattern" action definition.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var CANCEL_LABEL = Granite.I18n.get('Cancel');
    var REPLACE_AND_MODIFY_LABEL = Granite.I18n.get('Replace and modify by pattern');
    var REPLACE_LABEL = Granite.I18n.get('Replace');
    var MODIFY_LABEL = Granite.I18n.get('Modify');
    var PATTERN_LABEL = Granite.I18n.get('Please enter the regex pattern to be replaced');
    var REPLACEMENT_LINK_LABEL = Granite.I18n.get('Please enter the replacement');
    var DRY_RUN_CHECKBOX_LABEL = Granite.I18n.get('Dry run');
    var BACKUP_CHECKBOX_LABEL = Granite.I18n.get('Backup before replacement');
    var CSV_OUT_CHECKBOX_LABEL = Granite.I18n.get('Download CSV with updated items');
    var DRY_RUN_TOOLTIP = Granite.I18n.get("If checked, no changes will be stored in the repository");
    var REPLACEMENT_DESCRIPTION = Granite.I18n.get('* Replacement will be applied within the selected scope');
    var REPLACEMENT_ACL_DESCRIPTION = Granite.I18n.get('** User should have sufficient read/write permissions to complete replacement and create the backup package');
    var VALIDATION_MSG = Granite.I18n.get('Replacement cannot be the same as pattern');
    var LINK_TO_UPDATE_LABEL = Granite.I18n.get('The following content will be updated:');

    var PROCESSING_ERROR_MSG = 'Failed to replace by pattern<br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var PERSISTENCE_ERROR_MSG = 'Replacement was interrupted due to the <b>error</b> occurred during persisting changes. Please see logs for more details';
    var FORBIDDEN_ERROR_MSG = 'Failed to build the backup package. Possible reasons: lack of permissions, please see logs for more details.<br/><b>No replacement was applied</b>';
    var PROCESSING_SUCCESS_MSG = 'Replacement completed. %s<br/><br/>Pattern: <b>{{pattern}}</b><br/>Replacement: <b>{{replacement}}</b>';
    var DRY_RUN_PREFIX_MSG = '(Dry run) ';
    var DOWNLOADED_CSV_MSG = 'Please see the downloaded CSV for more details.';
    var PROCESSING_NOT_FOUND_MSG = 'Properties matching the pattern <b>{{pattern}}</b> were not found, or user has insufficient permissions to process them';
    var PROCESSING_IDENTICAL_MSG = 'The pattern <b>{{pattern}}</b> is equal to the replacement value, no processing was done';

    var REPLACE_BY_PATTERN_COMMAND = '/content/etoolbox-link-inspector/servlet/replaceByPattern';
    var READ_PERMISSIONS = 'read';

    var currentDate = Date.now();
    var CSV_OUTPUT_FILENAME = `replace_by_pattern_${currentDate}.csv`;

    /** Root action handler */
    function onFixAction(name, el, config, collection, selections) {
        const selectionItems = buildSelectionItems(selections);

        showConfirmationModal(selectionItems).then(function (data) {
            var replacementList = [{
                pattern: data.pattern,
                replacement: data.replacement,
                isDryRun: data.isDryRun,
                isBackup: data.isBackup,
                isOutputAsCsv: data.isOutputAsCsv,
                advancedMode: data.advancedMode,
                selected: data.selected,
                links: data.links
            }].filter(function (item) {
                return !item.advancedMode || item.pattern && item.replacement && item.pattern !== item.replacement;
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
                    cmd: "replaceByPattern",
                    page: new URL(window.location.href).searchParams.get('page') || 1
                }, item)
            }).fail(function (xhr, status, error) {
                if (xhr.status === 500) {
                    logger.log(ELC.format(PERSISTENCE_ERROR_MSG, item), false);
                } else if (xhr.status === 403) {
                    logger.log(ELC.format(FORBIDDEN_ERROR_MSG, item), false);
                } else {
                    logger.log(ELC.format(PROCESSING_ERROR_MSG, item), false);
                }
            }).done(function (data, textStatus, xhr) {
                if (xhr.status === 202) {
                    logger.log(ELC.format(PROCESSING_IDENTICAL_MSG, item), false);
                } else if (xhr.status === 204) {
                    logger.log(ELC.format(PROCESSING_NOT_FOUND_MSG, item), false);
                } else {
                    handleSuccessRequest(xhr, data, logger, item);
                }
            });
        };
    }

    function handleSuccessRequest(xhr, data, logger, item) {
        if (item.isDryRun) {
            PROCESSING_SUCCESS_MSG = DRY_RUN_PREFIX_MSG + PROCESSING_SUCCESS_MSG;
        }
        if (xhr.getResponseHeader("Content-disposition") && data) {
            PROCESSING_SUCCESS_MSG = PROCESSING_SUCCESS_MSG.replace("%s", DOWNLOADED_CSV_MSG);
            downloadCsvOutput(data);
        }
        if (data && data.updatedItemsCount) {
            var updatedItemsCount = data.updatedItemsCount;
            var countMessage = `The number of updated items: <b>${updatedItemsCount}</b>`;
            PROCESSING_SUCCESS_MSG = PROCESSING_SUCCESS_MSG.replace("%s", countMessage);
        } else {
            PROCESSING_SUCCESS_MSG = PROCESSING_SUCCESS_MSG.replace("%s", "");
        }
        logger.log(ELC.format(PROCESSING_SUCCESS_MSG, item), false);
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
        setTimeout(function(){
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }, 5000);
    }

    // Confirmation dialog common methods
    function showConfirmationModal(selection) {
        var deferred = $.Deferred();

        var el = ELC.getDialog();
        el.variant = 'notice';
        el.header.textContent = REPLACE_AND_MODIFY_LABEL;
        el.footer.innerHTML = ''; // Clean content
        el.content.innerHTML = ''; // Clean content

        var $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        var $updateBtn = $('<button name="update-button" is="coral-button" variant="primary" coral-close>').text(REPLACE_LABEL);
        $cancelBtn.appendTo(el.footer);
        $updateBtn.appendTo(el.footer);

        buildConfirmationMessage(confirmationMessageSelectionItems(selection)).appendTo(el.content);

        let $replaceRadio = new Coral.Radio();
            $replaceRadio.label.textContent = REPLACE_LABEL;
            $replaceRadio.name = 'replace-modify';
            $replaceRadio.checked = true;
            $replaceRadio.value = 'replace';

        let $modifyRadio = new Coral.Radio();
            $modifyRadio.label.textContent = MODIFY_LABEL;
            $modifyRadio.name = 'replace-modify';
            $modifyRadio.value = 'modify';

            el.content.appendChild($replaceRadio);
            el.content.appendChild($modifyRadio);

        let $patternFieldGroup = $('<div class="elc-pattern-field-group" hidden>');
        var $patternTextField =
            $('<input is="coral-textfield" class="elc-pattern-input" name="pattern" value="" placeholder=".+" required>');
        $('<p>').text(PATTERN_LABEL).appendTo($patternFieldGroup);
        $patternTextField.appendTo($patternFieldGroup);
        $patternFieldGroup.appendTo(el.content);

        // Replacement input group
        var $replacementTextField =
            $('<input is="coral-textfield" class="elc-replacement-input" name="replacement" value="" required>');
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(el.content);
        $replacementTextField.appendTo(el.content);

        // Dry run checkbox group
        var $isDryRunCheckbox =
            $('<coral-checkbox name="dry-run-checkbox" title="' + DRY_RUN_TOOLTIP + '" checked>').text(DRY_RUN_CHECKBOX_LABEL);
        $isDryRunCheckbox.appendTo(el.content);

        // Backup checkbox group
        var $isBackupCheckbox = $('<coral-checkbox name="isBackup" disabled>').text(BACKUP_CHECKBOX_LABEL);
        $isBackupCheckbox.appendTo(el.content);

        // CSV output checkbox group
        var $isCsvOutputCheckbox = $('<coral-checkbox name="isOutputAsCsv">').text(CSV_OUT_CHECKBOX_LABEL);
        $isCsvOutputCheckbox.appendTo(el.content);

        $('<p>').append($('<i>').text(REPLACEMENT_DESCRIPTION)).appendTo(el.content);
        $('<p>').append($('<i>').text(REPLACEMENT_ACL_DESCRIPTION)).appendTo(el.content);

        function onValidate() {
            var replVal = $replacementTextField.val();
            var patternVal = $patternTextField.val();
            var advanced = $modifyRadio.checked;
            $replacementTextField.each(function () {
                this.setCustomValidity(replVal === patternVal ? VALIDATION_MSG : '');
            });
            $updateBtn.attr('disabled', !replVal || (advanced && !patternVal || replVal === patternVal));
        }

        /** @param {Event} e */
        function onResolve(e) {
            var data = {
                pattern: $patternTextField.val(),
                advancedMode: $modifyRadio.checked,
                replacement: $replacementTextField.val(),
                isDryRun: $isDryRunCheckbox.prop("checked"),
                isBackup: $isBackupCheckbox.prop("checked"),
                isOutputAsCsv: $isCsvOutputCheckbox.prop("checked"),
                selected: selection.map(item => {
                    return item.path + '@' + item.propertyName
                }),
                links: selection.map(item => {
                    return item.source
                })
            }
            deferred.resolve(data);
        }

        /** @param {Event} e */
        function onDryRunChange(e) {
            $isBackupCheckbox.attr('disabled', $isDryRunCheckbox.prop('checked'));
        }

        el.on('input', 'input', onValidate);
        el.on('click', '[name="update-button"]', onResolve);
        el.on('change', '[name="dry-run-checkbox"]', onDryRunChange);
        el.on('coral-overlay:close', function () {
            el.off('input', 'input', onValidate);
            el.off('click', '[name="update-button"]', onResolve);
            el.off('change', '[name="dry-run-checkbox"]', onDryRunChange);
            deferred.reject();
        });

        el.show();
        onValidate();

        el.content.addEventListener('change', function(event) {
          if (event.target.name == 'replace-modify') {
             $patternFieldGroup.attr('hidden', event.target.value != 'modify');
             onValidate();
          }
        });

        return deferred.promise();
    }

    function buildConfirmationMessage(selections) {
        let list = selections.slice(0, 12).map(function (row) {
            return '<li>' + row.source + ' (' + row.count + ')' + '</li>';
        });
        if (selections.length > 12) {
            list.push('<li>&#8230;</li>'); // &#8230; is ellipsis
        }
        let $msg = $('<div class="elc-confirmation-msg">');
        $('<p>').text(LINK_TO_UPDATE_LABEL).appendTo($msg);
        $('<ul class="elc-processing-link-list">').html(list.join('')).appendTo($msg);
        $('<br/>').appendTo($msg);
        return $msg;
    }

    function confirmationMessageSelectionItems(selections) {
         let valuesMap = {};
         selections.map(function (row) {
            return row.source;
         }).forEach(function (item) {
            valuesMap[item] = (valuesMap[item]||0) + 1;
         });

         let items = [];
         for (const [key, value] of Object.entries(valuesMap)) {
           items.push({source: key, count: value});
         }
         return items;
    }

    function buildSelectionItems(selections) {
        return selections.map(function (v) {
            const $row = $(v);
            return {
                path: $row.data('path'),
                source: $row.find('.result .source').text(),
                propertyName: $row.data('propertyName')
            };
        });
    }

    function onFixActiveCondition(name, el, config, collection, selections) {
        selections.length > 0 ? el.removeAttribute('disabled') : el.setAttribute('disabled', true)
        return true;
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.etoolbox.linkinspector.action.replace-by-pattern",
        handler: onFixAction
    });

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.activecondition", {
        name: "cq-admin.etoolbox.linkinspector.actioncondition.replace-by-pattern",
        handler: onFixActiveCondition
    });

    // ACL check
    $(document).ready(function () {
        var gridHasItems = $('.eli-item[is="coral-table-row"][data-path]').length > 0;
        if (gridHasItems && ELC.aclCheck(REPLACE_BY_PATTERN_COMMAND, READ_PERMISSIONS)) {
            $('#elc-replace-by-pattern').prop('disabled', false);
        }
    });
})(window, document, Granite.$, Granite.ELC, Granite);