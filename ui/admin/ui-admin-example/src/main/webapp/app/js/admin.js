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

 // create our angular module and tell angular what route(s) it will handle

 var simplePlugin = angular.module('admin_plugin', ['hawtioCore'])
   .config(function($routeProvider) {
     $routeProvider.
       when('/admin_plugin', {
           templateUrl: '../hawtio/ui-admin-example/admin.html'
         });
   })


 simplePlugin.run(function(workspace, viewRegistry, layoutFull) {

     // tell the app to use the full layout, also could use layoutTree
     // to get the JMX tree or provide a URL to a custom layout
     viewRegistry["admin_plugin"] = layoutFull;

     // Set up top-level link to our plugin
     workspace.topLevelTabs.push({
       content: "Admin",
       title: "Admin plugin loaded dynamically",
       isValid: function() { return true; },
       href: function() { return "#/admin_plugin"; },
       isActive: function() { return workspace.isLinkActive("admin_plugin"); }

     });

   });

 // tell the hawtio plugin loader about our plugin so it can be
 // bootstrapped with the rest of angular
 hawtioPluginLoader.addModule('admin_plugin');