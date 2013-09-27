/**
 * Federated Search page used to add and edit a search service
 */
define(function(require) {
    'use strict';
    var FederatedSourceView = require('views/federated.source'),
        App = require("app");
    var FederatedSourceController = {
        addFederatedSource : function() {
          App.main.show(new FederatedSourceView());
        }
    };

    return FederatedSourceController;
});