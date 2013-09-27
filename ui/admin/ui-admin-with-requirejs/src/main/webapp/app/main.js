/*global define, console */
/*jslint nomen:false*/
define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Application = require('app'),
        Router = require('router'),
        Controller = require('controllers/federated.source.controller');

        var startApplication = function () {
            Application.Router = new Router({controller:  Controller});
            Application.start();
        };

    $(function () {
        startApplication();
    });
});