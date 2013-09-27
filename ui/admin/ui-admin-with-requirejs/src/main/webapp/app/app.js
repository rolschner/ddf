/*global define*/
/*jslint nomen: false*/

define (function (require) {
   'use strict';
    var Backbone = require("backbone");

    require("marionette");

    var App = new Backbone.Marionette.Application();

    App.addRegions({

        main: '#main',
        breadcrumb: "#bread-crumb"
    });

    App.on("initialize:after", function(){
         Backbone.history.start({pushState: true});
    });


    return App;

});
