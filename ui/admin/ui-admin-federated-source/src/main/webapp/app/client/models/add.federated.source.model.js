var FederatedSource = Backbone.Model.extend({

    // done really need this, but it's here to make things clearer
    serviceConfig: "data",
    //This is probably better off in a properties file.
    configUrl: "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",

    /**
     * Bind all things
     */
    initialize: function () {
        _.bindAll(this, "collectedData", "makeConfigCall");
    },

    /**
     * Collect all the data to save.
     * @param pid The pid id.
     * @returns {{type: string, mbean: string, operation: string}}
     */
    collectedData: function (pid) {
        var model = this;
        var data = {
            type: 'EXEC',
            mbean: 'ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
            operation: 'update'
        };
        data.arguments = [pid];
        data.arguments.push(_.clone(model.attributes));
        return data;
    },

    /**
     * Get the serviceConfig PID
     * @param model, this is really this model.
     * @returns an ajax promis
     */
    makeConfigCall: function (model) {
        if (!model) {
            return;
        }
        var configUrl = [model.configUrl, "createFactoryConfiguration", model.serviceConfig].join("/");
        return $.ajax({type: 'GET',
            url: configUrl
        });
    },

    /**
     * When a model calls save the sync is called in Backbone.  I override it because this isn't a typical backbone
     * object
     * @return Return a deferred which is a hadler with the success and failure callback.
     */
    sync: function () {
        var deferred = $.Deferred(),
            model = this,
            addUrl = [model.configUrl, "add"].join("/")
        model.makeConfigCall(model).done(function (data) {
            var collect = model.collectedData(JSON.parse(data).value),
                jData = JSON.stringify(collect);

            return $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: jData,
                url: addUrl
            }).done(function (result) {
                    deferred.resolve(result);
                }).fail(function (error) {
                    deferred.fail(error);
                });
        }).fail(function (error) {
                deferred.fail(error);
            });
        return deferred;
    }
});

