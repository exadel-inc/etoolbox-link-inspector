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
 * Provides the functionality to edit the property value in the table cell
 */
(function (window, document, $, ELC) {
    'use strict';

    const foundationUi = $(window).adaptTo('foundation-ui');
    const $document = $(document);
    $document.ready(function () {
        $document.on('click', '.elc-editproperty-button', onEditorButtonClick);
    });

    /* --------------
       Event handlers
       -------------- */

    function onEditorButtonClick(e) {
        const dataHolder = e.target.closest('tr');
        const contentHolder = e.target.closest('td').querySelector('.primary');

        const path = dataHolder.dataset.path;
        const propertyName = './' + dataHolder.dataset.propertyName;
        const matchedText = dataHolder.dataset.match;
        const content = contentHolder.innerHTML;

        const dialog = ELC.getDialog('editproperty', {
            header: { textContent: 'Edit Property' },
            content: {
                innerHTML: `<div class="editor" contenteditable="true"></div>`
            },
            footer: {
                innerHTML: `
                  <button is="coral-button" variant="default" coral-close>Cancel</button>
                  <button data-dialog-action is="coral-button" variant="primary" coral-close>OK</button>`
            }
        });

        if (!dialog.classList.contains('initialized')) {
            dialog.classList.add('initialized');
            $(dialog)
                .on('click', 'button[variant="primary"]', onEditDialogSubmit)
                .on('coral-overlay:close', onEditDialogClose)
                .on('input', '.editor', onEditorInput)
                .on('keydown', '.editor', onEditorKeyDown);
        }

        $(dialog).one('coral-overlay:open', function () {
            const editor = dialog.querySelector('.editor');
            editor.innerText = content;
            editor.dataset.match = matchedText;
            editor.dataset.path = path;
            editor.dataset.property = propertyName;
            editor.sourceElement = contentHolder;
            setHighlights(editor, matchedText);
        });
        dialog.show();
    }

    function onEditDialogSubmit(e) {
        const dialog = e.target.closest('coral-dialog');
        const editor = dialog.querySelector('.editor');
        const formData = new FormData();
        formData.append('path', editor.dataset.path);
        formData.append('propertyName', editor.dataset.property);
        formData.append('updatedLink', editor.innerText);
        formData.append('currentLink', editor.sourceElement.innerHTML);
        $.ajax({
            type: 'POST',
            url: '/content/etoolbox-link-inspector/servlet/editValue',
            data: formData,
            processData: false,
            contentType: false,
            success: function () {
                editor.sourceElement.innerHTML = editor.innerText;
                foundationUi.notify('Success', 'Value saved successfully', 'info');
            },
            error: function(err) {
                console.log('Error while saving value', err);
                foundationUi.notify('Error', 'Unable to save value', 'error');
            }
        });
    }

    function onEditDialogClose(e) {
        const dialog = e.target.closest('coral-dialog');
        const editor = dialog.querySelector('.editor');
        editor.innerText = '';
        delete editor.dataset.match;
        delete editor.dataset.path;
        delete editor.dataset.property;
        delete editor.sourceElement;
    }

    function onEditorInput(e) {
        const editor = e.target;
        setHighlights(editor, editor.dataset.match);
    }

    function onEditorKeyDown(e) {
        const editor = e.target;
        if (e.key === 'Enter') {
            e.preventDefault();
            editor.closest('coral-dialog').querySelector('button[variant="primary"]').click();
        }
    }

    /* ---------------
       Service methods
       --------------- */

    function setHighlights(editor, highlight) {
        if (!editor) return;

        const cursorPosition = getCursorPosition(editor);
        let content = editor.innerText || '';
        content = escapeHtml(content);
        content = content.replaceAll(highlight, `<mark>${highlight}</mark>`);
        editor.innerHTML = content;

        try {
            setCursorPosition(editor, cursorPosition);
        } catch (e) {
            console.warn('Could not restore caret position:', e);
        }
    }

    function getCursorPosition(element) {
        const selection = window.getSelection();
        if (selection && selection.rangeCount > 0) {
            const range = selection.getRangeAt(0);
            const preCaretRange = range.cloneRange();
            preCaretRange.selectNodeContents(element);
            preCaretRange.setEnd(range.endContainer, range.endOffset);
            return preCaretRange.toString().length;
        }
        return 0;
    }

    function setCursorPosition(element, position) {
        const selection = window.getSelection();
        const range = document.createRange();

        let currentOffset = 0;
        let targetNode = null;
        let targetOffset = 0;
        const findPosition = (node) => {
            if (node.nodeType === Node.TEXT_NODE) {
                if (currentOffset + node.length >= position) {
                    targetNode = node;
                    targetOffset = position - currentOffset;
                    return true;
                }
                currentOffset += node.length;
            } else {
                for (const child of node.childNodes) {
                    if (findPosition(child)) {
                        return true;
                    }
                }
            }
            return false;
        }
        findPosition(element);
        if (targetNode) {
            range.setStart(targetNode, targetOffset);
            range.setEnd(targetNode, targetOffset);
            selection.removeAllRanges();
            selection.addRange(range);
        }
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

})(window, document, Granite.$, Granite.ELC);
