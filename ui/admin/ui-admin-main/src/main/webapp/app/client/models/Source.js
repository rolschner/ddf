var Source = Backbone.Model.extend({
    configUrl: "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0",
    defaults: {
        "id": "N/A",
        "bundle_name" : "N/A",
        "name" : "N/A",
        "fpid" : "N/A",
        "bundle" : "N/A",
        "shortName" : "N/A",
        "sourceStatus" : "Available",
        "properties" : {}
    },
    /**
     * Bind all things
     */
    initialize: function(sourceJson) {
        _.bindAll(this, "collectedData");
        // MBean AI Results
        console.log(sourceJson);

        // it appears as the fields aren't getting set via their defaults
        if(sourceJson.id){
            this.id = sourceJson.id;
        }

        if(sourceJson.bundle_name){
            this.bundle_name = sourceJson.bundle_name;
        }

        if(sourceJson.name){
            this.name = sourceJson.name;
        }

        if(sourceJson.fpid){
            this.fpid = sourceJson.fpid;
        }

        if(sourceJson.bundle){
            this.bundle = sourceJson.bundle;
        }

        if(sourceJson.properties.shortname) {
            this.shortName = sourceJson.properties.shortname;
        }

        if(sourceJson.available === true) {
            this.sourceStatus = "Available";
        }
        else if(sourceJson.available === false) {
            this.sourceStatus = "Not Available";
        }
        else {
            this.sourceStatus = "Unknown";
        }

        if(sourceJson.properties){
            this.properties = sourceJson.properties;
        }

        if(sourceJson.metatype){
            this.metatype = sourceJson.metatype;
        }
    },
    getContentTypesAsString: function() {
        return this.contentTypes.toString();
    },

    /**
     * Collect all the data to save.
     * @param pid The pid id.
     * @returns {{type: string, mbean: string, operation: string}}
     */
    collectedData: function () {
        var model = this;
        var data = {
            type: 'EXEC',
            mbean: 'ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
            operation: 'update'
        };
        data.arguments = [model.id];
        data.arguments.push(_.clone(model.attributes));
        return data;
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
        var collect = model.collectedData(),
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
        return deferred;
    }
});

var SourceList = Backbone.Collection.extend({
    model: Source,
    url : "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredConfigurations",
    sync: function(method, model, options) {
        options.dataType = "json";
        return Backbone.sync(method, model, options);
    },
    parse: function (response) {
        return response.value;
    }
});