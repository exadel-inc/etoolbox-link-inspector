(function(window, document, $, Granite) {
    "use strict";

    var ui = $(window).adaptTo("foundation-ui");
    var FIX_BROKEN_LINK_COMMAND = "/content/exadel-linkchecker/servlet/fixBrokenLink";
    var REPORT_URL = "/content/exadel-linkchecker/download/report.csv";
    var updateText = Granite.I18n.get("Update Link");
    var cancelText = Granite.I18n.get("Cancel");

    function progressTicker(title, message) {
        var el = new Coral.Dialog();
        el.backdrop = Coral.Dialog.backdrop.STATIC;
        el.header.textContent = title;
        el.header.insertBefore(new Coral.Wait(), el.header.firstChild);
        el.content.innerHTML = message || "";
        el.id = "fix-broken-link-dialog";

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

    function fixBrokenLink(paths, propertyName, currentLink, newLink) {

        var tickerMessage = $(document.createElement("div"));

        var wt = progressTicker("Processing", "Starting broken link replacement ...");

        function createFixRequest(path) {
            return function () {

                wt.updateMessage(tickerMessage.html()
                    + "Replacement in progress ...<br/>");

                var deferred = $.Deferred();
                $.ajax({
                    url: FIX_BROKEN_LINK_COMMAND,
                    type: "POST",
                    data: {
                        _charset_: "UTF-8",
                        cmd: "fixBrokenLink",
                        path: paths,
                        propertyName: propertyName,
                        currentLink:currentLink,
                        newLink: newLink
                    }
                }).fail(function() {
                    $(document.createElement("div"))
                        .html("Failed to replace the link <b>" + currentLink + "</b> with <b>" + newLink + "</b><br/> at <i>" + path + "@" + propertyName + "</i>")
                        .appendTo(tickerMessage);
                }).done(function(data, textStatus, xhr) {
                    console.log("status: " + xhr.status);
                    var message = "The link <b>" + currentLink + "</b> was successfully replaced with <b>" + newLink + "</b><br/> at <i>" + path + "@" + propertyName + "</i>";
                    if (xhr.status == 204) {
                        message =  "The link <b>" + currentLink + "</b> was not found at <i>" + path + "@" + propertyName + "</i>"
                    }
                    if (xhr.status == 202) {
                        message =  "The current link <b>" + currentLink + "</b> is equal to the entered one, replacement was not applied";
                    }
                    $(document.createElement("div"))
                        .html(message)
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
                value: "",
                id: "new-link"
            });

            $(document.createElement("p")).html("<br/>Please enter the replacement link").appendTo(message)
            message.append(textField);

            ui.prompt(updateText, message.html(), "notice", [{
                text: cancelText
            }, {
                text: updateText,
                primary: true,
                handler: function() {
                    var path = selections.map(function(v) {
                        return $(v).data("path");
                    });
                    var currentLink = selections.map(function(v) {
                        return $(v).find("#current-link").text();
                    });
                    var propertyName = selections.map(function(v) {
                        return $(v).find("#property-location").text();
                    });
                    var newLink = $('#new-link').val();

                    fixBrokenLink(path, propertyName, currentLink, newLink);
                }
            }]);
        }
    });

    $(document).ready(function(){
        $(".download-full-report-button").click(function(e) {
            e.preventDefault();
            $.ajax({
                url: REPORT_URL,
                method: 'HEAD',
                success: function() {
                    window.location = REPORT_URL;
                },
                error: function () {
                    //todo - replace with coral alert
                    alert("Report hasn't been generated yet");
                }
            });
        });

        $.ajax({
            url: "/content/exadel-linkchecker/servlet/pendingGenerationCheck",
            async: false,
            type: "POST",
            success: function(data, textStatus, xhr) {
                if (xhr.status == 200) {
                    var alertPopup = new Coral.Alert().set({
                        header: {
                            innerHTML: "INFO"
                        },
                        content: {
                            innerHTML: "Some links were updated. Changes will be reflected in the report after data feed regeneration"
                        },
                        id: "linkchecker-info-alert"
                    });
                    document.body.append(alertPopup);
                }
            }
        });
    });

})(window, document, Granite.$, Granite);