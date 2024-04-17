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
 * "Filters" action definition.
 */
(function (window, document, $, ELC, Granite, Coral) {
    'use strict';
    const DIALOG_TITLE_LABEL = Granite.I18n.get('Filters');
    const CANCEL_LABEL = Granite.I18n.get('Cancel');
    const SUBMIT_FILTER_LABEL = Granite.I18n.get('Apply');

    function onFilterAction(name, el, config, collection, selections) {
        const dialog = document.querySelector('#filters-result-dialog');
        dialog.show();
    }

    function initFiltersDialog(){
        const dialog = new Coral.Dialog().set({
            id : 'filters-result-dialog',
            closable: Coral.Dialog.closable.ON,
            backdrop: Coral.Dialog.backdrop.STATIC,
            interaction: 'off',
            header :{
                innerHTML : DIALOG_TITLE_LABEL
            }
        });

        const linksTypeSelect = new Coral.Select().set({
            placeholder: "Choose an item",
            multiple: true
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: "Internal"
            },
            value: "internal",
            disabled: false,
            selected: true,
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: "External"
            },
            value: "external",
            disabled: false,
            selected: true,
        });
        $('<p>').html("Links type (<span class='dialog-description'>The type of links in the report</span>)").appendTo(dialog.content);
        dialog.content.appendChild(linksTypeSelect);

        const $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        const $updateBtn =
            $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(SUBMIT_FILTER_LABEL);

        $cancelBtn.appendTo(dialog.footer);
        $updateBtn.appendTo(dialog.footer);

        function onSubmit(){
            insertParam("type", linksTypeSelect.value)
        }

        dialog.on('click', '[data-dialog-action]', onSubmit);
        dialog.on('coral-overlay:close', function (event) {
            dialog.remove();
            initFiltersDialog();
        });

        document.body.appendChild(dialog);
    }

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.etoolbox.linkinspector.action.filter-options",
        handler: onFilterAction
    });

    $(document).ready(function () {
        initFiltersDialog();
    });

    function insertParam(key, value) {
        key = encodeURIComponent(key);
        value = encodeURIComponent(value);

        var kvp = document.location.search.substr(1).split('&');
        let i=0;

        for(; i<kvp.length; i++){
            if (kvp[i].startsWith(key + '=')) {
                let pair = kvp[i].split('=');
                pair[1] = value;
                kvp[i] = pair.join('=');
                break;
            }
        }

        if(i >= kvp.length){
            kvp[kvp.length] = [key,value].join('=');
        }

        let params = kvp.join('&');

        document.location.search = params;
    }

})(window, document, Granite.$, Granite.ELC, Granite, Coral);