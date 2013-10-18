var Configuration = Backbone.Model.extend({
    configUrl: "/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",
    /**
     * Bind all things
     */
    initialize: function(sourceJson) {
        _.bindAll(this, "sync", "collectedData");
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
     * Get the serviceFactoryPid PID
     * @param model, this is really this model.
     * @returns an ajax promis
     */
    makeConfigCall: function (model) {
        if (!model) {
            return;
        }
        var configUrl = [model.configUrl, "createFactoryConfiguration", model.get("service.factoryPid")].join("/");
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
            addUrl = [model.configUrl, "add"].join("/");
        //if it has a pid we are editing an existing record
        if(model.get("service.pid"))
        {
            var collect = model.collectedData(model.get("service.pid"));
            var jData = JSON.stringify(collect);

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
        }
        else //no pid means this is a new record
        {
            model.makeConfigCall(model).done(function (data) {
                var collect = model.collectedData(JSON.parse(data).value);
                var jData = JSON.stringify(collect);

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
        }
        return deferred;
    }
});

var Source = Backbone.Model.extend({
    configUrl: "/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",
    /**
     * Bind all things
     */
    initialize: function(options) {
        _.bindAll(this, "sync", "initializeConfigurationFromMetatype", "initializeFromMSF");
        if(!_.isUndefined(options) && !_.isUndefined(options.properties)) //a config already exists so we just initialize it
        {
            this.configuration = new Configuration(options.properties);
        }
    },
    initializeFromMSF: function(msf) {
        this.set({"fpid":msf.get("id")});
        this.set({"name":msf.get("name")});
        this.initializeConfigurationFromMetatype(msf.get("metatype"));
        this.configuration.set({"service.factoryPid": msf.get("id")});
    },
    initializeConfigurationFromMetatype: function(metatype) {
        var src = this;
        src.configuration = new Configuration();
        metatype.forEach(function(obj){
            var id = obj["id"];
            var val = obj["defaultValue"];
            src.configuration.set(id, (val) ? val.toString() : null);
        });
    }
});

var SourceList = Backbone.Collection.extend({
    model: Source,
    url : "/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredConfigurations",
    sync: function(method, model, options) {
        options.dataType = "json";
        return Backbone.sync(method, model, options);
    },
    parse: function (response) {
        return response.value;
    }
});
