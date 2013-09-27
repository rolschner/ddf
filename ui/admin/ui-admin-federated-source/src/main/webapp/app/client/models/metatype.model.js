/**
 * This just holds the collection of metatypes.
 * You could just use the default backbone model and collection, since nothing special
 * is happening here.
 */
var MetaType = {};
MetaType.Model = Backbone.Model.extend({

});

MetaType.Collection = Backbone.Collection.extend({
   model : MetaType.Model
});
