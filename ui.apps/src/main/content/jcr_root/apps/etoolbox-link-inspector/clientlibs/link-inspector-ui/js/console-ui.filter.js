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
 * "Filter Options" action definition.
 */
(function (window, document, $, ELC, Granite, Coral) {
    'use strict';
    const DIALOG_TITLE_LABEL = Granite.I18n.get('Filter Options');
    const SUCCESS_DIALOG_TITLE_LABEL = Granite.I18n.get('Filter Options Applied');
    const CANCEL_LABEL = Granite.I18n.get('Cancel');
    const SUBMIT_FILTER_LABEL = Granite.I18n.get('Apply');

    const successDialog = new Coral.Dialog().set({
        id : "filter-dialog-success",
        closable: Coral.Dialog.closable.ON,
        variant: "success",
        header :{
            innerHTML : SUCCESS_DIALOG_TITLE_LABEL
        },
        content :{
            innerHTML: "<p>Filter changes has been applied. Next report will be generated according to the applied settings</p>"
        },
        footer :{
            innerHTML: "<button is=\"coral-button\" variant=\"primary\" coral-close>Ok</button>"
        }
    });

    function onFilterAction(name, el, config, collection, selections) {
        const dialog = document.querySelector('#filter-dialog');
        dialog.show();
    }

    function initActionDialog(){
        const dialog = new Coral.Dialog().set({
            id : 'filter-dialog',
            closable: Coral.Dialog.closable.ON,
            backdrop: Coral.Dialog.backdrop.STATIC,
            interaction: 'off',
            header :{
                innerHTML : DIALOG_TITLE_LABEL
            }
        });

        const $cancelBtn = $('<button is="coral-button" variant="default" coral-close>').text(CANCEL_LABEL);
        const $updateBtn =
            $('<button data-dialog-action is="coral-button" variant="primary" coral-close>').text(SUBMIT_FILTER_LABEL);
        $cancelBtn.appendTo(dialog.footer);
        $updateBtn.appendTo(dialog.footer);

        const filterMultifield = createMultifield();
        $('<p>').text("Filter").appendTo(dialog.content);
        dialog.content.appendChild(filterMultifield);

        const $rootPathField = $('<input is="coral-textfield" class="elc-replacement-input" name="replacement" value="" required>');
        $('<p>').text("Path(The content path for searching broken links. The search path should be located under /content)").appendTo(dialog.content);
        $rootPathField.appendTo(dialog.content);

        const excludedPathsMultifield = createMultifield();
        $('<p>').text("Excluded Paths(The list of paths excluded from processing. The specified path and all its children are excluded. The excluded path should not end with slash. Can be specified as a regex)").appendTo(dialog.content);
        dialog.content.appendChild(excludedPathsMultifield);

        const $activatedContentCheckbox = $('<coral-checkbox value="activatedContent">Activated Content(If checked, links will be retrieved from activated content only)</coral-checkbox>');
        $activatedContentCheckbox.appendTo(dialog.content);

        const $skipContentAfterActivationCheckbox = $('<coral-checkbox value="skipContentAfterActivation">Skip content modified after activation(Works in conjunction with the \'Activated Content\' checkbox only. If checked, links will be retrieved from activated content that is not modified after activation (lastModified is before lastReplicated))</coral-checkbox>');
        $skipContentAfterActivationCheckbox.appendTo(dialog.content);

        const $lastModifiedContentField = $('<input is="coral-textfield" class="elc-replacement-input" name="lastMidified" value="" required>');
        $('<p>').text("Last Modified (The content modified before the specified date will be excluded. Tha date should has the ISO-like date-time format, such as '2011-12-03T10:15:30+01:00')").appendTo(dialog.content);
        $lastModifiedContentField.appendTo(dialog.content);

        $.ajax({
            type: "GET",
            url: "/content/etoolbox-link-inspector/data/config.json"
        }).done(function (data){
            populateMultifield(filterMultifield, data.filter);
            $rootPathField.val(data.path);
            populateMultifield(excludedPathsMultifield, data.excludedPaths);
            $activatedContentCheckbox.attr("checked", data.activatedContent);
            $skipContentAfterActivationCheckbox.attr("checked", data.skipContentAfterActivation);
            $lastModifiedContentField.val(data.lastModifiedBoundary);
        })

        function createMultifield(){
            const multifield = new Coral.Multifield();
            multifield.template.content.appendChild(new Coral.Textfield());

            const add = new Coral.Button();
            add.label.textContent = 'Add';
            add.setAttribute('coral-multifield-add', '');
            multifield.appendChild(add);
            return multifield;
        }

        function populateMultifield(multifield, data){
            if(!data){
                return;
            }
            for (let d of data){
                const item = new Coral.Multifield.Item();
                const textField = new Coral.Textfield();
                textField.value = d;
                item.content.appendChild(textField);
                multifield.items.add(item);
            }
        }

        function getMultifieldValues(multifield){
            let multifieldValues = multifield.items.getAll().map((item) => item.content.children[0].value);
            return !!multifieldValues.length ? multifieldValues : "";
        }

        function onSubmit(){
            $.ajax({
                type: "POST",
                url: "/content/etoolbox-link-inspector/data/config",
                data: {
                    'jcr:primaryType': "nt:unstructured",
                    "filter": getMultifieldValues(filterMultifield),
                    "filter@TypeHint": "String[]",
                    "path": $rootPathField.val(),
                    "excludedPaths": getMultifieldValues(excludedPathsMultifield),
                    "excludedPaths@TypeHint": "String[]",
                    "activatedContent":!!$activatedContentCheckbox.attr("checked"),
                    "activatedContent@TypeHint": "Boolean",
                    "skipContentAfterActivation":!!$skipContentAfterActivationCheckbox.attr("checked"),
                    "skipContentAfterActivation@TypeHint": "Boolean",
                    "lastModifiedBoundary": $lastModifiedContentField.val()
                },
                dataType: "json",
                encode: true
            }).done(function(){
                document.querySelector('#filter-dialog-success').show();
            })
        }

        dialog.on('click', '[data-dialog-action]', onSubmit);
        dialog.on('coral-overlay:close', function (event) {
            dialog.remove();
            initActionDialog();
        });
        document.body.appendChild(dialog);
    }

    // Button action handler assignment
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.etoolbox.linkinspector.action.filter-options",
        handler: onFilterAction
    });

    //Dialog initialization
    $(document).ready(function () {
        initActionDialog();
        document.body.appendChild(successDialog);
    });

})(window, document, Granite.$, Granite.ELC, Granite, Coral);