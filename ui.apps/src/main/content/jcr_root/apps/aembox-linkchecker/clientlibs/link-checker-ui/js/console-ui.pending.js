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
 * AEMBox LinkChecker clientlib.
 * Post-load utility to check the updates, happened in the report list.
 */
(function (window, document, $, Granite) {
    "use strict";

    var CHECK_URL = '/content/aembox-linkchecker/servlet/pendingGenerationCheck';
    var UPDATE_MSG = Granite.I18n.get('Some links were updated. Changes will be reflected in the report after data feed regeneration');

    $(document).ready(function () {
        $.ajax({
            url: CHECK_URL,
            type: 'POST',
            success: function (data, textStatus, xhr) {
                if (xhr.status === 200) {
                    var alertPopup = new Coral.Alert().set({
                        header: {
                            innerHTML: 'INFO'
                        },
                        content: {
                            textContent: UPDATE_MSG
                        }
                    });
                    alertPopup.classList.add('elc-coral-alert');
                    document.body.append(alertPopup);
                }
            }
        });
    });
})(window, document, Granite.$, Granite);