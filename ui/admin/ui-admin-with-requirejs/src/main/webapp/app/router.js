/*global define, OWF */
/*jslint nomen:true */

// #Main Application
define(function(require) {
   'use strict';

    // Load attached libs and application modules
    var Backbone = require("backbone");
    require("marionette");


    var Router = Backbone.Marionette.AppRouter.extend({
        appRoutes: {
            'add': 'addFederatedSource'
        }
    });

    return Router;
});
