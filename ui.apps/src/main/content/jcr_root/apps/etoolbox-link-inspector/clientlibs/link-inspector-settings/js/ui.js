(function(document, $) {
    "use strict";

    $(document)
        .off('.elc-settings-ui')
        .one("foundation-contentloaded.elc-settings-ui", function() {
            $(".etoolbox-dialog-checkbox-showhide").each(function() {
                showHide($(this));
            });
        })
        .on("change.elc-settings-ui", ".etoolbox-dialog-checkbox-showhide", function() {
            showHide($(this));
        });

    function showHide($el){
        let isChecked = $el.attr("checked");
        let targetElementSelector = $el.data('show-hide-target');
        let $targetElement = $('.' + targetElementSelector);

        $targetElement.attr('hidden', !isChecked);
        $targetElement.children('coral-datepicker').attr('required', !!isChecked);
    }

})(document,Granite.$);