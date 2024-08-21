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
 * "Delete Report" action definition.
 */
(function (window, document, $, ELC, Granite, Coral) {
    'use strict';
    const DIALOG_TITLE_LABEL = Granite.I18n.get('Delete Report');
    const CANCEL_LABEL = Granite.I18n.get('Cancel');
    const SUBMIT_FILTER_LABEL = Granite.I18n.get('Delete');

    function onFilterAction(name, el, config, collection, selections) {
        const dialog = document.querySelector('#delete-dialog');
        dialog.show();
    }

    function initDeleteDialog(){
        const dialog = new Coral.Dialog().set({
            id : 'delete-dialog',
            closable: Coral.Dialog.closable.ON,
            backdrop: Coral.Dialog.backdrop.STATIC,
            interaction: 'off',
            header :{
                innerHTML : DIALOG_TITLE_LABEL
            }
        });

        $('<p>').html('Are you sure you want to delete the report?').appendTo(dialog.content);

        const $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        const $updateBtn =
            $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(SUBMIT_FILTER_LABEL);

        $cancelBtn.appendTo(dialog.footer);
        $updateBtn.appendTo(dialog.footer);

        function onDeleteDialogSubmit(){
            dialog.trigger('coral-overlay:close');
            $.ajax({
                url: '/content/etoolbox-link-inspector/servlet/deleteReport',
                type: 'GET',
                success: function(){
                    window.location.reload();
                }
            });
        }

        dialog.on('click', '[data-dialog-action]', onDeleteDialogSubmit);
        dialog.on('coral-overlay:close', function (event) {
            dialog.remove();
            initDeleteDialog();
        });

        document.body.appendChild(dialog);
    }

    $(window).adaptTo('foundation-registry').register('foundation.collection.action.action', {
        name: 'cq-admin.etoolbox.linkinspector.action.delete-report',
        handler: onFilterAction
    });

    $(document).ready(function () {
       initDeleteDialog();
    });

})(window, document, Granite.$, Granite.ELC, Granite, Coral);