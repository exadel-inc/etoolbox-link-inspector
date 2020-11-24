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
    var PROCESSING_SUCCESS_MSG = 'The link <b>{{currentLink}}</b> was successfully replaced with <b>{{newLink}}</b><br/> at <i>{{item.path}}@{{propertyName}}</i>';
    var PROCESSING_NOT_FOUND_MSG = 'The link <b>{{currentLink}}</b> was not found at <i>{{path}}@{{propertyName}}</i>';
    var PROCESSING_IDENTICAL_MSG = 'The current link <b>{{currentLink}}</b> is equal to the entered one, replacement was not applied';

    var ui = $(window).adaptTo('foundation-ui');
    var FIX_BROKEN_LINK_COMMAND = '/content/exadel-linkchecker/servlet/fixBrokenLink';

    function onFixAction(name, el, config, collection, selections) {
        var message = buildFixDialogContent(selections);
        ui.prompt(UPDATE_LABEL, message, 'notice', [{
            text: CANCEL_LABEL
        }, {
            text: UPDATE_LABEL,
            primary: true,
            handler: function () {
                var newLink = $('.elc-replacement-input').val();
                var replacementList = selections.map(function (v) {
                    var row = $(v);
                    return {
                        path: row.data('path'),
                        currentLink: row.data('currentLink'),
                        propertyName: row.data('propertyName'),
                        newLink: newLink
                    };
                });
                processBrokenLink(replacementList);
            }
        }]);
    }
    function buildFixDialogContent(selections) {
        // TODO: Still not good enough
        var message = $('<div>');
        $('<p>').text(LINK_TO_UPDATE_LABEL).appendTo(message);

        var list = selections.slice(0, 12).map(function (row) {
            var link = $(row).data('currentLink');
            return '<li>' + link + '</li>';
        });
        if (selections.length > 12) {
            list.push('<li>&#8230;</li>'); // &#8230; is ellipsis
        }
        $('<ul class="elc-processing-link-list">').html(list.join('')).appendTo(message);

        var replacementTextField = new Coral.Textfield().set({
            name: 'replacementLink',
            value: ''
        });
        replacementTextField.classList.add('elc-replacement-input');

        $('<br/>').appendTo(message);
        $('<p>').text(REPLACEMENT_LINK_LABEL).appendTo(message);
        message.append(replacementTextField);
        return message.html();
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

    /**
     * Create {@return ProcessLogger} wrapper
     * @return {ProcessLogger}
     *
     * @typedef ProcessLogger
     * @method finished
     * @method log
     */
    function createLoggerDialog(title, message) {
        var el = new Coral.Dialog({
            backdrop: Coral.Dialog.backdrop.STATIC
        });
        el.header.textContent = title;
        el.header.insertBefore(new Coral.Wait(), el.header.firstChild);
        el.content.innerHTML = message || '';
        el.classList.add('elc-log-dialog');

        document.body.appendChild(el);
        el.show();

        return {
            dialog: el,
            finished: function () {
                el.header.textContent = FINISHED_LABEL;

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
        return text.replace(/{{(\w+)}}/, function (match, term) {
           if (term in dictionary) return String(dictionary[term]);
           return match;
        });
    }

    // INIT
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.exadel.linkchecker.action.fix-broken-link",
        handler: onFixAction
    });
})(window, document, Granite.$, Granite);