/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

//This .js file just needs to be referenced by the blueprint config to load the tab in hawtio
//We don't need to actually load this JavaScript in our web app (and we shouldn't)

// create our angular module and tell angular what route(s) it will handle

var simplePlugin = angular.module('sources_plugin', ['hawtioCore'])
    .config(function($routeProvider) {
        $routeProvider.
        when('/sources_plugin', {
            templateUrl: '../hawtio/ui-admin-sources/sources.html'
        });
})


simplePlugin.run(function(workspace, viewRegistry, layoutFull) {

// tell the app to use the full layout, also could use layoutTree
// to get the JMX tree or provide a URL to a custom layout
viewRegistry["sources_plugin"] = layoutFull;

// Set up top-level link to our plugin
workspace.topLevelTabs.push({
    content: "Sources",
    title: "Sources plugin loaded dynamically",
    isValid: function() { return true; },
    href: function() { return "#/sources_plugin"; },
    isActive: function() { return workspace.isLinkActive("sources_plugin"); }
});

});

// tell the hawtio plugin loader about our plugin so it can be
// bootstrapped with the rest of angular
hawtioPluginLoader.addModule('sources_plugin');
