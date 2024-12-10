(function(document, $) {
    "use strict";

    $(document).on("foundation-contentloaded", function(e) {
        $(".etoolbox-dialog-checkbox-showhide").each(function() {
            showHide($(this));
        });
    });

    $(document).on("change", ".etoolbox-dialog-checkbox-showhide", function(e) {
        showHide($(this));
    });

    function showHide($el){
        let isChecked = $el.attr("checked");
        let targetElementSelector = $el.data('show-hide-target');
        let $targetElement = $('.' + targetElementSelector);

        $targetElement.attr('hidden', !isChecked);
    }

})(document,Granite.$);