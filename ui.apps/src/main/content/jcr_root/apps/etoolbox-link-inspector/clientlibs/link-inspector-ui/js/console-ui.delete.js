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

    function onFilterAction() {
        const dialog = document.querySelector('#delete-report');
        dialog.show();
    }

    function initDeleteDialog(){
        const dialog = ELC.getDialog('delete-report', {
            id: 'delete-dialog',
            header: {
                textContent: Granite.I18n.get('Delete Report')
            },
            content: {
                innerHTML: '<div class="elc-dialog-content">Are you sure you want to delete the report?</div>'
            },
            footer: {
                innerHTML: `
                  <button is="coral-button" variant="default" coral-close>${Granite.I18n.get('Cancel')}</button>
                  <button data-dialog-action is="coral-button" variant="primary" coral-close>${Granite.I18n.get('Delete')}</button>
                `
            }
        });

        function onDeleteDialogSubmit(){
            dialog.trigger('coral-overlay:close');
            $.ajax({
                url: '/bin/etoolbox/link-inspector/delete-report',
                type: 'DELETE',
                success: function(){
                    window.location.reload();
                },
                error: function(){
                    $(window).adaptTo('foundation-ui').alert(
                        Granite.I18n.get('Error'),
                        Granite.I18n.get('Error while deleting report'),
                        'error');
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