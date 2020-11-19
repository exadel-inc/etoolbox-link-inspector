/**
 * Exadel LinkChecker clientlib.
 * "Fix link" action definition.
 */
(function (window, document, $, Granite) {
    'use strict';

    var CLOSE_LABEL = Granite.I18n.get('Close');
    var CANCEL_LABEL = Granite.I18n.get('Cancel');
    var UPDATE_LABEL = Granite.I18n.get('Update Link');
    var FINISHED_LABEL = Granite.I18n.get('Finished');
    var PROCESSING_LABEL = Granite.I18n.get('Processing');
   // var REPLACEMENT_PROGRESS_LABEL = Granite.I18n.get('Replacement in progress ...');
    var START_REPLACEMENT_LABEL = Granite.I18n.get('Starting broken link replacement ...');
    var LINK_TO_UPDATE_LABEL = Granite.I18n.get('The following link will be updated:');
    var REPLACEMENT_LINK_LABEL = Granite.I18n.get('Please enter the replacement link');

    var PROCESSING_ERROR_MSG = 'Failed to replace the link <b>{{currentLink}}</b> with <b>{{newLink}}</b><br/> at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_SUCCESS_MSG = 'The link <b>{{currentLink}}</b> was successfully replaced with <b>{{newLink}}</b><br/> at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_NOT_FOUND_MSG = 'The link <b>{{currentLink}}</b> was not found at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_IDENTICAL_MSG = 'The current link <b>{{currentLink}}</b> is equal to the entered one, replacement was not applied';

    var FIX_BROKEN_LINK_COMMAND = '/content/exadel-linkchecker/servlet/fixBrokenLink';

    /** Root action handler */
    function onFixAction(name, el, config, collection, selections) {
        var selectionItems = buildSelectionItems(selections);

        showConfirmationModal(selectionItems).then(function (newLink) {
            var replacementList = selectionItems.map(function (item) {
                return $.extend({
                    newLink: newLink
                }, item);
            });
            processBrokenLink(replacementList);
        });
    }

    function processBrokenLink(items) {
        var logger = createLoggerDialog(PROCESSING_LABEL, START_REPLACEMENT_LABEL);
        var requests = $.Deferred().resolve();
        requests = items.reduce(function (query, item) {
            return query.then(buildFixRequest(item, logger));
        }, requests);
        requests.always(function () {
            logger.finished();
            logger.dialog.on('coral-overlay:close', function () {
                window.location.reload();
            });
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
            }).fail(function () {
                logger.log(format(PROCESSING_ERROR_MSG, item), false);
            }).done(function (data, textStatus, xhr) {
                if (xhr.status === 202) {
                    logger.log(format(PROCESSING_IDENTICAL_MSG, item), false);
                } else if (xhr.status === 204) {
                    logger.log(format(PROCESSING_NOT_FOUND_MSG, item), false);
                } else {
                    logger.log(format(PROCESSING_SUCCESS_MSG, item), false);
                }
            });
        };
    }

    // Confirmation dialog common methods
    function showConfirmationModal(selection) {
        var deferred = $.Deferred();

        var el = getDialog();
        el.variant = 'notice';
        el.header.textContent = UPDATE_LABEL;
        el.footer.innerHTML = [
            '<button is="coral-button" variant="default" coral-close>' + CANCEL_LABEL + '</button>',
            '<button data-dialog-action is="coral-button" variant="primary" coral-close>' + UPDATE_LABEL + '</button>'
        ].join('');

        el.content.innerHTML = ''; // Clean content
        buildConfirmationMessage(selection).appendTo(el.content);

        // Replacement input group
        var $replacementTextField =
            $('<input is="coral-textfield" class="elc-replacement-input" name="replacementLink" value="">');
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(el.content);
        $replacementTextField.appendTo(el.content);

        var onResolve = function () {
            deferred.resolve($replacementTextField.val());
        };

        el.on('click', '[data-dialog-action]', onResolve);
        el.on('coral-overlay:close', function () {
            el.off('click', '[data-dialog-action]', onResolve);
            deferred.reject();
        });
        el.show();

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
            }).on('coral-overlay:close', function(e) {
                e.target.remove();
            });
            sharableDialog.classList.add('elc-dialog');
        }
        return sharableDialog;
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.exadel.linkchecker.action.fix-broken-link",
        handler: onFixAction
    });
})(window, document, Granite.$, Granite);