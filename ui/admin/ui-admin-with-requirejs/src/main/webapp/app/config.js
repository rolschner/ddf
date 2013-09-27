/*global require*/

var require = {
    baseUrl : 'app',

    paths : {

        // amplify for ajax calls
        amplify : '../assets/js/components/amplify/core/amplify.core',
        amplifyrequest : '../assets/js/components/amplify/request/amplify.request',

        // backbone
        backbone : '../assets/js/components/backbone/backbone-min',
        marionette : '../assets/js/components/marionettejs/lib/backbone.marionette.min',
        modelbinder: '../assets/js/components/Backbone.ModelBinder/Backbone.ModelBinder.min',

        // jquery
        jquery : '../assets/js/components/jquery/jquery.min',

        // bootstrap for ui
        bootstrap : '../assets/js/components/bootstrap/docs/assets/js/bootstrap',
        icanhaz : '../assets/js/components/icanhazjs/ICanHaz.min',
        mustache : '../assets/js/components/mustache/mustache',
        lodash : '../assets/js/components/lodash/dist/lodash.min',

        text : '../assets/js/components/text/text',

        // templates
        templates : '../assets/templates'
    },

    shim :  {

        backbone : {
            deps: ['lodash', 'jquery'],
            exports: 'Backbone'
        },
        modelbinder: ['backbone'],

        lodash: {
            exports: '_'
        },
        marionette: {
            deps: ['backbone'],
            exports: 'Backbone.Marionette'
        },

        icanhaz: {
            deps: ['lodash', 'backbone', 'jquery'],
            exports: 'ich'
        },

        amplify : {
            deps: ['jquery'],
            exports: 'amplify'
        },

        amplifyrequest : {
            deps: ['jquery', 'amplify'],
            exports: 'amplify'
        },

        bootstrap: ['jquery']
    },
    map : {
         '*' : {
             'underscore' : 'lodash'
         }
    }
};