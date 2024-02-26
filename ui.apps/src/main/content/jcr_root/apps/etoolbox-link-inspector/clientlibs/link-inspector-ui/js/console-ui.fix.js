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
 * "Fix link" action definition.
 */
(function (window, document, $, ELC, Granite) {
    'use strict';

    var CANCEL_LABEL = Granite.I18n.get('Cancel');
    var UPDATE_LABEL = Granite.I18n.get('Fix Broken Link');
    var LINK_TO_UPDATE_LABEL = Granite.I18n.get('The following link will be updated:');
    var NOT_SELECTED_ITEMS_LABEL = Granite.I18n.get('Not selected items. Please select one or more.');
    var REPLACEMENT_LINK_LABEL = Granite.I18n.get('Please enter the replacement link');
    var SKIP_VALIDATION_LABEL = 'Skip input link check before replacement'

    var PROCESSING_ERROR_MSG = 'Failed to replace the link <b>{{currentLink}}</b> with <b>{{newLink}}</b><br/> at <i>{{path}}@{{propertyName}}</i>';
    var LINK_VALIDATION_ERROR_MSG = 'The input link <b>{{newLink}}</b> is not valid.%s<br/><br/>Please enter a valid link and try again';
    var PROCESSING_SUCCESS_MSG = 'The link <b>{{currentLink}}</b> was successfully replaced with <b>{{newLink}}</b><br/> at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_NOT_FOUND_MSG = 'The link <b>{{currentLink}}</b> was not found at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_IDENTICAL_MSG = 'The current link <b>{{currentLink}}</b> is equal to the entered one, replacement was not applied';

    var FIX_BROKEN_LINK_COMMAND = '/content/etoolbox-link-inspector/servlet/fixBrokenLink';
    var READ_WRITE_PERMISSIONS = "read,set_property";

    /** Root action handler */
    function onFixAction(name, el, config, collection, selections) {
        var selectionItems = buildSelectionItems(selections);

        showConfirmationModal(selectionItems).then(function (data) {
            var replacementList = selectionItems.map(function (item) {
                return $.extend({
                    newLink: data.newLink,
                    isSkipValidation: data.isSkipValidation,
                    page: new URL(window.location.href).searchParams.get('page') || 1
                }, item);
            });
            ELC.bulkLinksUpdate(replacementList, buildFixRequest);
        });
    }

    function buildFixRequest(item, logger) {
        return function () {
            return $.ajax({
                url: FIX_BROKEN_LINK_COMMAND,
                type: "POST",
                data: $.extend({
                    _charset_: "UTF-8",
                    cmd: "fixBrokenLink"
                }, item)
            }).fail(function (xhr, status, error) {
                if (xhr.status === 400) {
                    var statusCode = xhr.responseJSON.statusCode;
                    var statusMessage = xhr.responseJSON.statusMessage;
                    if (statusCode && statusMessage) {
                        var errorMsgDetails = `<br/>Status: ${statusCode}, ${statusMessage}`;
                        LINK_VALIDATION_ERROR_MSG = LINK_VALIDATION_ERROR_MSG.replace("%s", errorMsgDetails);
                    } else {
                        LINK_VALIDATION_ERROR_MSG = LINK_VALIDATION_ERROR_MSG.replace("%s", "");
                    }
                    logger.log(ELC.format(LINK_VALIDATION_ERROR_MSG, item), false);
                } else {
                    logger.log(ELC.format(PROCESSING_ERROR_MSG, item), false);
                }
            }).done(function (data, textStatus, xhr) {
                if (xhr.status === 202) {
                    logger.log(ELC.format(PROCESSING_IDENTICAL_MSG, item), false);
                } else if (xhr.status === 204) {
                    logger.log(ELC.format(PROCESSING_NOT_FOUND_MSG, item), false);
                } else {
                    logger.log(ELC.format(PROCESSING_SUCCESS_MSG, item), false);
                }
            });
        };
    }

    // Confirmation dialog common methods
    function showConfirmationModal(selection) {
        var deferred = $.Deferred();

        var el = ELC.getSharableDlg();
        el.variant = 'notice';
        el.header.textContent = UPDATE_LABEL;
        el.footer.innerHTML = ''; // Clean content
        el.content.innerHTML = ''; // Clean content

        var $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        var $updateBtn = $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(UPDATE_LABEL);
        $cancelBtn.appendTo(el.footer);
        $updateBtn.appendTo(el.footer);

        buildConfirmationMessage(selection).appendTo(el.content);

        // Replacement input group
        var $replacementTextField =
            $('<input is="coral-textfield" class="elc-replacement-input" name="replacementLink" value="" required>');
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(el.content);
        $replacementTextField.appendTo(el.content);

        // Skip validation checkbox group
        var $isSkipValidation = $('<coral-checkbox name="isSkipValidation">').text(SKIP_VALIDATION_LABEL);
        $isSkipValidation.appendTo(el.content);

        function onValidate() {
            var newLink = $replacementTextField.val();
            var currentLink = selection && selection[0] ? selection[0].currentLink : '';
            $replacementTextField.each(function () {
                this.setCustomValidity(newLink === currentLink ? "Input link shouldn't be equal to the current one" : '');
            });
            $updateBtn.attr('disabled', !newLink || newLink === currentLink);
        }

        var onResolve = function () {
            var data = {
                newLink: $replacementTextField.val(),
                isSkipValidation: $isSkipValidation.prop("checked")
            }
            deferred.resolve(data);
        };

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

    function buildSelectionItems(selections) {
        return selections.map(function (v) {
            var row = $(v);
            return {
                path: row.data('path'),
                currentLink: row.data('currentLink'),
                propertyName: row.data('propertyName')
            };
        });
    }

    function buildConfirmationMessage(selections) {
        var list = selections.slice(0, 12).map(function (row) {
            return '<li>' + row.currentLink + '</li>';
        });
        if (selections.length > 12) {
            list.push('<li>&#8230;</li>'); // &#8230; is ellipsis
        }
        var $msg = $('<div class="elc-confirmation-msg">');
        $('<p>').text(LINK_TO_UPDATE_LABEL).appendTo($msg);
        $('<ul class="elc-processing-link-list">').html(list.join('')).appendTo($msg);
        $('<br/>').appendTo($msg);
        return $msg;
    }

    function aclCheckPermissions(name, el, config, collection, selections) {
        //todo check permissions for selection?
        var path = selections.map(function (v) {
            return $(v).data('path');
        });
        return ELC.aclCheck(path, READ_WRITE_PERMISSIONS);
    }

    function onFixActiveCondition(name, el, config, collection, selections) {
        selections.length > 0 ? el.removeAttribute('disabled') : el.setAttribute('disabled', true)
        return true;
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.etoolbox.linkinspector.action.fix-broken-link",
        handler: onFixAction
    });

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.activecondition", {
        name: "cq-admin.aembox.linkchecker.actioncondition.fix-broken-link",
        handler: onFixActiveCondition
    });

})(window, document, Granite.$, Granite.ELC, Granite);