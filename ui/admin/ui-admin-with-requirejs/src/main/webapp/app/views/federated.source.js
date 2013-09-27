/**
 * Federated Search page used to add and edit a search service
 */
define(function(require) {
   'use strict';

    var ich = require('icanhaz'),
        Backbone = require('backbone');
        ich.addTemplate("federatedSource", require('text!templates/federated.source.html'));

    var FederatedSource = Backbone.View.extend({
         render: function() {
             var view = this;
             view.$el.html(ich.federatedSource());
             return view;
         }
    });

    return FederatedSource;

});
