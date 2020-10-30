(function(window, document, $, Granite) {
    "use strict";

    var ui = $(window).adaptTo("foundation-ui");
    var COMMAND_URL = Granite.HTTP.externalize("/bin/exadel/fix-broken-link");
    var updateText = Granite.I18n.get("Update Link");
    var cancelText = Granite.I18n.get("Cancel");

    function progressTicker(title, message) {
        var el = new Coral.Dialog();
        el.backdrop = Coral.Dialog.backdrop.STATIC;
        el.header.textContent = title;
        el.header.insertBefore(new Coral.Wait(), el.header.firstChild);
        el.content.innerHTML = message || "";

        document.body.appendChild(el);
        el.show();

        return {
            finished: function(message) {
                el.header.textContent = "Finished"
                el.content.innerHTML = message;

                var b = new Coral.Button();
                b.label.textContent = "Close";
                b.variant = "primary";

                b.on("click", function(e) {
                    //ui.clearWait();
                    window.location.reload();
                });

                el.footer.appendChild(b);
            },
            updateMessage: function(message) {
                el.content.innerHTML = message;
            },
            clear: function() {
                el.hide();

                requestAnimationFrame(function() {
                    el.remove();
                });
            }
        };
    }

    function fixBrokenLink(paths) {

        var tickerMessage = $(document.createElement("div"));

        var wt = progressTicker("Processing", "Starting broken link replacement ...");

        function createFixRequest(path) {
            return function () {

                wt.updateMessage(tickerMessage.html()
                    + path + "&nbsp;&nbsp; [in progress ...]<br/>");

                var deferred = $.Deferred();
                $.ajax({
                    url: COMMAND_URL,
                    type: "POST",
                    data: {
                        _charset_: "UTF-8",
                        cmd: "fixBrokenLink",
                        path: path
                    }
                }).fail(function() {
                    $(document.createElement("div"))
                        .html(path + "&nbsp;&nbsp; <b>FAILED</b>")
                        .appendTo(tickerMessage);
                }).done(function() {
                    $(document.createElement("div"))
                        .html(path + "&nbsp;&nbsp; <b>SUCCESS</b>")
                        .appendTo(tickerMessage);
                }).always(function () {
                    deferred.resolve();
                    wt.updateMessage(tickerMessage.html());
                });

                return deferred.promise();
            };
        }


        var requests = $.Deferred();
        requests.resolve();
        for (var i = 0; i < paths.length; i++) {
            var path = paths[i];
            requests = requests.then(createFixRequest(path));
        }

        requests.always(function() {
            wt.finished(tickerMessage.html());

            setTimeout(function () {
            }, 3000);
        });
    }

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "cq-admin.exadel.linkchecker.action.fix-broken-link",
        handler: function(name, el, config, collection, selections) {
            var message = $(document.createElement("div"));

            var intro = $(document.createElement("p")).appendTo(message);
            intro.text(Granite.I18n.get("The following link will be updated:"));

            var list = [];
            var maxCount = Math.min(selections.length, 12);
            for (var i = 0, ln = maxCount; i < ln; i++) {
                var title = $(selections[i]).find(".main-sync h4").text();
                list.push($("<b>").text(title).html());
            }
            if (selections.length > maxCount) {
                list.push("&#8230;"); // &#8230; is ellipsis
            }

            $(document.createElement("b")).html(list.join("<br>")).appendTo(message);

            var textField = new Coral.Textfield().set({
                name: "replacementLink",
                value: ""
            });

            $(document.createElement("p")).html("<br/>Please enter the replacement link").appendTo(message)
            message.append(textField);

            ui.prompt(updateText, message.html(), "notice", [{
                text: cancelText
            }, {
                text: updateText,
                primary: true,
                handler: function() {
                    var paths = selections.map(function(v) {
                        return $(v).data("path");
                    });

                    fixBrokenLink(paths);
                }
            }]);
        }
    });

    $(document).ready(function(){
        $(".download-full-report-button").click(function(e) {
            e.preventDefault();
            $.ajax({
                url: "/content/exadel-linkchecker/report.csv",
                method: 'HEAD',
                success: function() {
                    window.location = "/content/exadel-linkchecker/report.csv";
                },
                error: function () {
                    //todo - replace with coral alert
                    alert("Report hasn't been generated yet");
                }
            });
        });
    });

})(window, document, Granite.$, Granite);