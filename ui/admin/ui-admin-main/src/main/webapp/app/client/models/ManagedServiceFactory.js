var ManagedServiceFactory = Backbone.Model.extend({

});

ManagedServiceFactory.Collection = Backbone.Collection.extend({
    model : ManagedServiceFactory,
    url : "/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredFactoryConfigurations/",
    sync: function(method, model, options) {
        options.dataType = "json";
        return Backbone.sync(method, model, options);
    },
    parse: function (response) {
        return response.value;
    }
});
