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
 * "Filter" action definition.
 */
(function (window, document, $, ELC, Granite, Coral) {
    'use strict';
    const DIALOG_TITLE_LABEL = Granite.I18n.get('Filter Links');
    const CANCEL_LABEL = Granite.I18n.get('Cancel');
    const SUBMIT_FILTER_LABEL = Granite.I18n.get('Apply');

    function onFilterAction(name, el, config, collection, selections) {
        const dialog = document.querySelector('#filter--dialog');
        dialog.show();
    }

    function initFiltersDialog(searchParams){
        const dialog = new Coral.Dialog().set({
            id : 'filter--dialog',
            closable: Coral.Dialog.closable.ON,
            backdrop: Coral.Dialog.backdrop.STATIC,
            interaction: 'off',
            header :{
                innerHTML : DIALOG_TITLE_LABEL
            }
        });

        const linksTypeSelect = new Coral.Select().set({
            placeholder: 'Choose an item',
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: 'All'
            },
            value: '',
            disabled: false,
            selected: !searchParams.get('type')
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: 'Internal'
            },
            value: 'internal',
            disabled: false,
            selected: searchParams.get('type') === 'internal'
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: 'External'
            },
            value: 'external',
            disabled: false,
            selected: searchParams.get('type') === 'external'
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: 'Custom'
            },
            value: 'custom',
            disabled: false,
            selected: searchParams.get('type') === 'custom'
        });

        $('<p>').html('By type').appendTo(dialog.content);
        dialog.content.appendChild(linksTypeSelect);

        const $linkSubstringField = $('<input is="coral-textfield" class="elc-substring-input" name="substring" value="">');
        $linkSubstringField.val(searchParams.get("substring"));
        $('<p>').html('By text').appendTo(dialog.content);
        $linkSubstringField.appendTo(dialog.content);

        const $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        const $updateBtn =
            $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(SUBMIT_FILTER_LABEL);

        $cancelBtn.appendTo(dialog.footer);
        $updateBtn.appendTo(dialog.footer);

        function onSubmit(){
            searchParams.delete('type');
            searchParams.delete('substring');
            if (linksTypeSelect.value) {
                searchParams.append('type', linksTypeSelect.value);
            }
            if ($linkSubstringField.val()) {
                searchParams.append('substring', $linkSubstringField.val());
            }
            searchParams.set('page', '1');
            document.location.search = searchParams;
        }

        dialog.on('click', '[data-dialog-action]', onSubmit);
        dialog.on('change', function(event) {
            linksTypeSelect.value
        })
        dialog.on('coral-overlay:close', function (event) {
            dialog.remove();
            initFiltersDialog(new URL(document.location).searchParams);
        });

        document.body.appendChild(dialog);
    }

    $(window).adaptTo('foundation-registry').register('foundation.collection.action.action', {
        name: 'cq-admin.etoolbox.linkinspector.action.filter-options',
        handler: onFilterAction
    });

    $(document).ready(function () {
       let searchParams = new URL(document.location).searchParams;
       if (searchParams != null && searchParams.get('type') != null || searchParams.get('substring') != null) {
         $('#elc-filter-options').attr('variant', 'primary');
       }
       initFiltersDialog(searchParams);
    });

})(window, document, Granite.$, Granite.ELC, Granite, Coral);