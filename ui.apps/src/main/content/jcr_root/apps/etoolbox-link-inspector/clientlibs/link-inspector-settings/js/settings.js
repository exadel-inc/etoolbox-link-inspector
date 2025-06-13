(function(document, Granite, $) {
    'use strict';

    const $document = $(document);

    function preloadSettings(data) {
        Object.keys(data).forEach(function (key) {
            const $input = $document.find(`[name="${key}"]`);
            if ($input.length) {
                if ($input.is(':checkbox') && data[key]) {
                    $input.prop('checked');
                } else {
                    $input.val(data[key]);
                }
            }
        });
    }

    $document
        .off('.elc-settings')
        .one('foundation-contentloaded.elc-settings', function () {
            $.get('/apps/etoolbox-link-inspector/components/pages/settings/jcr:content/content/items/columns/items/form/items/tabs/datasource.json')
                .then(preloadSettings)
                .fail((error) => {
                    console.error('Error preloading settings', error);
                });
        })
        .on('submit.elc-settings', '#elc-settings-save', function (e) {
            e.preventDefault();
            const $form = $(this);
            $.ajax({
                type: "POST", url: $form.attr('action'), data: $form.serialize(), success: function () {
                    location.replace("/etoolbox/link-inspector.html")
                }, error: function () {
                    $(window).adaptTo('foundation-ui').alert(Granite.I18n.get('Error'), Granite.I18n.get('Error while saving settings'), 'error');
                }
        });
    });
})(document, Granite, Granite.$);

