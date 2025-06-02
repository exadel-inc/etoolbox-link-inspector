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
 * Common utilities
 */
(function (window, document, $, Granite, Coral) {
    'use strict';

    var Utils = Granite.ELC = (Granite.ELC || {});

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
    Utils.format = format;

    const sharableDialogs = {};
    /** Common sharable dialog instance getter */
    function getDialog(id = 'default', options) {
        if (!sharableDialogs[id]) {
            let dialogOptions = {
                backdrop: Coral.Dialog.backdrop.STATIC
            };
            if (options && typeof options === 'object') {
                dialogOptions = Object.assign(dialogOptions, options);
            }
            sharableDialogs[id] = new Coral.Dialog().set(dialogOptions).on('coral-overlay:close', function (e) {
                e.target.remove();
            });
            sharableDialogs[id].id = id;
            sharableDialogs[id].classList.add('elc-dialog');
            sharableDialogs[id].content.classList.add('content');
        }
        return sharableDialogs[id];
    }
    Utils.getDialog = getDialog;

    var CLOSE_LABEL = Granite.I18n.get('Close');
    var FINISHED_LABEL = Granite.I18n.get('Finished');

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
    Utils.createLoggerDialog = createLoggerDialog;

    var PROCESSING_LABEL = Granite.I18n.get('Processing');
    var START_REPLACEMENT_LABEL = Granite.I18n.get('Links update is in progress ...');

    /**
     * Process bulk update for the links.
     * @param {Array} items - items to update
     * @param {Function} updateRequest - item update request builder
     * @return {JQuery.Deferred}
     */
    function bulkLinksUpdate(items, updateRequest) {
        var logger = createLoggerDialog(PROCESSING_LABEL, START_REPLACEMENT_LABEL);
        var requests = $.Deferred().resolve();
        requests = items.reduce(function (query, item) {
            return query.then(updateRequest(item, logger));
        }, requests);
        requests.always(function () {
            logger.finished();
            logger.dialog.on('coral-overlay:close', function () {
                $(window).adaptTo('foundation-ui').wait();
                window.location.reload();
            });
        });
        return requests;
    }
    Utils.bulkLinksUpdate = bulkLinksUpdate;

    var ACL_CHECK_COMMAND = '/content/etoolbox/link-inspector/servlet/aclCheck';

    /**
     * Check if user has specified permissions for the given path.
     *
     * @param {String} path - the path in a repository for checking
     * @param {String} permissions - comma separated set of permissions
     * @returns {boolean}
     */
    function aclCheck(path, permissions) {
        var hasPermissions = false;
        $.ajax({
            url: ACL_CHECK_COMMAND,
            type: 'POST',
            async: false,
            data: {
                _charset_: "UTF-8",
                path: path,
                permissions: permissions
            },
            success: function (data) {
                hasPermissions = data && data.hasPermissions;
            }
        });
        return hasPermissions;
    }
    Utils.aclCheck = aclCheck;

    var RESOURCE_EXIST_CHECK_COMMAND = '/content/etoolbox/link-inspector/servlet/resourceExistCheck';

    /**
     * Checks if the resource with specified paths exists
     *
     * @param {String} path - resource path to check
     * @returns {boolean}
     */
    function resourceExistCheck(path) {
        var resourceExists = false;
        $.ajax({
            url: RESOURCE_EXIST_CHECK_COMMAND,
            type: 'POST',
            async: false,
            data: {
                _charset_: "UTF-8",
                path: path
            },
            success: function (data) {
                resourceExists = data && data.resourceExists;
            }
        });
        return resourceExists;
    }
    Utils.resourceExistCheck = resourceExistCheck;

})(window, document, Granite.$, Granite, Coral);