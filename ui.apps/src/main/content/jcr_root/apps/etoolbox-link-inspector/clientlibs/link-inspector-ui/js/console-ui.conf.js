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
 * "Options" action definition.
 */
(function (window, document, $, ELC, Granite, Coral) {
    'use strict';
    const DIALOG_TITLE_LABEL = Granite.I18n.get('Options');
    const SUCCESS_DIALOG_TITLE_LABEL = Granite.I18n.get('Options Applied');
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
            innerHTML: "<p>Filter changes have been applied. The next report will be generated in accordance with the applied settings</p>"
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

        const $rootPathField = $('<input is="coral-textfield" class="elc-replacement-input" name="replacement" value="">');
        $('<p>').html("Path (<span class='dialog-description'>The content path for searching broken links. The search path should be located under /content</span>)").appendTo(dialog.content);
        $rootPathField.appendTo(dialog.content);

        const excludedPathsMultifield = createMultifield();
        $('<p>').html("Excluded Paths (<span class='dialog-description'>The list of paths excluded from processing. The specified path and all its children are excluded. The excluded path should not end with slash. Can be specified as a regex</span>)").appendTo(dialog.content);
        dialog.content.appendChild(excludedPathsMultifield);

        const $activatedContentCheckbox = $('<coral-checkbox value="activatedContent">Activated Content(<span class=\'dialog-description\'>If checked, links will be retrieved from activated content only</span>)</coral-checkbox>');
        $activatedContentCheckbox.appendTo(dialog.content);

        const $skipContentAfterActivationCheckbox = $('<coral-checkbox value="skipContentAfterActivation">Skip content modified after activation(<span class=\'dialog-description\'>Works in conjunction with the \'Activated Content\' checkbox only. If checked, links will be retrieved from activated content that is not modified after activation (lastModified is before lastReplicated)</span>)</coral-checkbox>');
        $skipContentAfterActivationCheckbox.appendTo(dialog.content);

        const filterMultifield = createMultifield();
        $('<p>').html("Excluded links patterns (<span class='dialog-description'>Links are excluded from processing if they match any of the specified regex patterns</span>)").appendTo(dialog.content);
        dialog.content.appendChild(filterMultifield);

        const $lastModifiedContentField = $('<input is="coral-textfield" class="elc-replacement-input" name="lastMidified" value="">');
        $('<p>').html("Last Modified (<span class='dialog-description'>The content modified before the specified date will be excluded. Tha date should has the ISO-like date-time format, such as '2011-12-03T10:15:30+01:00'</span>)").appendTo(dialog.content);
        $lastModifiedContentField.appendTo(dialog.content);

        const excludedPropertiesMultifield = createMultifield();
        $('<p>').html("Excluded Properties (<span class='dialog-description'>The list of properties excluded from processing. Each value can be specified as a regex</span>)").appendTo(dialog.content);
        dialog.content.appendChild(excludedPropertiesMultifield);

        const linksTypeSelect = new Coral.Select().set({
            placeholder: "Choose an item"
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: "Internal + External"
            },
            value: "Internal + External",
            disabled: false
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: "Internal"
            },
            value: "INTERNAL",
            disabled: false
        });
        linksTypeSelect.items.add({
            content:{
                innerHTML: "External"
            },
            value: "EXTERNAL",
            disabled: false
        });
        $('<p>').html("Links type (<span class='dialog-description'>The type of links in the report</span>)").appendTo(dialog.content);
        dialog.content.appendChild(linksTypeSelect);

        const $customTypeAllowedCheckbox = $('<coral-checkbox value="customTypeAllowed">Allow custom link type(<span class=\'dialog-description\'>If checked custom link types will be included in the reports</span>)</coral-checkbox>');
        $customTypeAllowedCheckbox.appendTo(dialog.content);

        const $excludeTagsCheckbox = $('<coral-checkbox value="excludeTags">Exclude tags(<span class=\'dialog-description\'>If checked, the internal links starting with /content/cq:tags will be excluded</span>)</coral-checkbox>');
        $excludeTagsCheckbox.appendTo(dialog.content);

        const statusCodesMultifield = createMultifield();
        $('<p>').html("Status codes (<span class='dialog-description'>The list of status codes allowed for broken links in the report. Set a single negative value to allow all http error codes</span>)").appendTo(dialog.content);
        dialog.content.appendChild(statusCodesMultifield);

        const $threadsPerCoreField = $('<input is="coral-textfield" class="elc-replacement-input" name="threadsPerCore" value="">');
        $('<p>').html("Threads per core (<span class='dialog-description'>The number of threads created per each CPU core for validating links in parallel</span>)").appendTo(dialog.content);
        $threadsPerCoreField.appendTo(dialog.content);

        $.ajax({
            type: "GET",
            url: "/conf/etoolbox-link-inspector/data/config.json"
        }).done(function (data){
            populateMultifield(filterMultifield, data.filter);
            $rootPathField.val(data.path);
            populateMultifield(excludedPathsMultifield, data.excludedPaths);
            $activatedContentCheckbox.attr("checked", data.activatedContent);
            $skipContentAfterActivationCheckbox.attr("checked", data.skipContentAfterActivation);
            $lastModifiedContentField.val(data.lastModifiedBoundary);
            populateMultifield(excludedPropertiesMultifield, data.excludedProperties);
            linksTypeSelect.value = data.linksType;
            $excludeTagsCheckbox.attr("checked", data.excludeTags);
            populateMultifield(statusCodesMultifield, data.statusCodes);
            $threadsPerCoreField.val(data.threadsPerCore);
            $customTypeAllowedCheckbox.attr("checked", data.customTypeAllowed);
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
                url: "/conf/etoolbox-link-inspector/data/config",
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
                    "lastModifiedBoundary": $lastModifiedContentField.val(),
                    "excludedProperties": getMultifieldValues(excludedPropertiesMultifield),
                    "excludedProperties@TypeHint": "String[]",
                    "linksType": linksTypeSelect.value,
                    "excludeTags":!!$excludeTagsCheckbox.attr("checked"),
                    "excludeTags@TypeHint": "Boolean",
                    "statusCodes": getMultifieldValues(statusCodesMultifield),
                    "statusCodes@TypeHint": "String[]",
                    "threadsPerCore": $threadsPerCoreField.val(),
                    "customTypeAllowed":!!$customTypeAllowedCheckbox.attr("checked"),
                    "customTypeAllowed@TypeHint": "Boolean"
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
        name: "cq-admin.etoolbox.linkinspector.action.conf-options",
        handler: onFilterAction
    });

    //Dialog initialization
    $(document).ready(function () {
        initActionDialog();
        document.body.appendChild(successDialog);
    });

})(window, document, Granite.$, Granite.ELC, Granite, Coral);