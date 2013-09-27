var Results={}
    Results.Model = Backbone.Model.extend({
       // I don't do anything in here because I don't care.
    });
    Results.Collection = Backbone.Collection.extend({
        model: Results.Model,
        url : "../json/search.json"
    });
