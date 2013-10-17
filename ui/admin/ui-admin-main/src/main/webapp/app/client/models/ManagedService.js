var ManagedService = Backbone.Model.extend({

});

ManagedService.Collection = Backbone.Collection.extend({
    model : ManagedServiceFactory,
    url : "/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredConfigurations/",
    sync: function(method, model, options) {
        options.dataType = "json";
        return Backbone.sync(method, model, options);
    },
    parse: function (response) {
        var parsedRepsonse = [];
        response.value.forEach(function(each){
            if(!each["fpid"])
            {
                parsedRepsonse.push(each);
            }
        });
        return parsedRepsonse;
    }
});
