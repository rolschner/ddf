/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/** Main view page for add. */
var ManagedServiceView = Backbone.View.extend({
    /**
     * Button events, right now there's a submit button
     * I do not know where to go with the cancel button.
     */
    events: {
        "click .submit-button": "submitData",
        "click #cancel": "cancel",
        "click .enable-checkbox" : "toggleEnable",
        "change .sourceTypesSelect" : "render"
    },

    /**
     * Initialize  the binder with the ManagedServiceFactory model.
     * @param options
     */
    initialize: function(options) {
        _.bindAll(this, "render", "close", "cancel", "setupPopOvers", "renderDynamicFields", "submitData");
        this.managedServiceList = options.managedServiceList;
        this.source = options.sourceModel;
        if(_.isUndefined(options.managedService))
        {
            options.managedServiceList.forEach(function(each){
                if(each["id"] === options.sourceModel["id"])
                {
                    options.managedService = each;
                }
            });
        }
        this.managedService = options.managedService;
        //the configuration won't have the service.pid set unless a configuration exists
        //managed services are a little different from managed service factories
        this.source.configuration.set({"service.pid": this.managedService.id});
        this.modelBinder = new Backbone.ModelBinder();
    },

    /**
     * This is where everything is rendered and the model is bound to the dom.
     * Add the main template
     * Clear whats in there now
     * Render the fields available
     * Setup popovers for description
     * Bind the model to the dom
     * return view for rendering to where the caller wants it to render.
     */
    render: function() {
        var view = this;
        var jsonObj = view.managedService.toJSON();
        view.$el.html("");
        view.$el.append(ich.editTemplate(jsonObj));
        view.$(".sourceTypesSelect").remove();
        view.$(".data-section").html("");
        view.renderDynamicFields();
        view.setupPopOvers();
        view.modelBinder.bind(view.source.configuration, view.$(".add-federated-source"));
        return view;
    },

    /**
     * Walk the collection of metatypes
     * Setup the ui based on the type
     * Append it to the bottom of this data-section selector
     */
    renderDynamicFields: function() {
        var view = this;

        view.managedService.metatype.forEach(function(each) {
            var type = each.get("type");
            var cardinality = each.get("cardinality"); //this is ignored for now and lists will be rendered as a ',' separated list
            if(!_.isUndefined(type)) {
                //from the Metatype specification
                // int STRING = 1;
                // int LONG = 2;
                // int INTEGER = 3;
                // int SHORT = 4;
                // int CHARACTER = 5;
                // int BYTE = 6;
                // int DOUBLE = 7;
                // int FLOAT = 8;
                // int BIGINTEGER = 9;
                // int BIGDECIMAL = 10;
                // int BOOLEAN = 11;
                // int PASSWORD = 12;
                if (type === 1 || type === 5 || type === 6 || (type >= 7 && type <= 10)) {
                    view.$(".data-section").append(ich.textType(each.toJSON()));
                }
                else if (type === 11) {
                    view.$(".data-section").append(ich.checkboxType(each.toJSON()));
                }
                else if (type === 12) {
                    view.$(".data-section").append(ich.passwordType(each.toJSON()));
                }
                else if (type === 2 || type === 3 || type === 4) { //this type can only be used for integers
                    view.$(".data-section").append(ich.numberType(each.toJSON()));
                }
            }
        });
    },
    /**
     * Submit to the backend.
     */
    submitData: function() {
        var view = this;
        view.source.configuration.save();
        view.cancel();
    },
    /**
     * unbind the model and dom during close.
     */
    close: function(){
        var view = this;
        view.modelBinder.unbind();
    },
    /**
     * returns the user to the source list page without saving
     */
    cancel: function(){
        var view = this;
        view.close();
        var sPage = new SourcePage({
            el: $("#main")
        });
        sPage.refreshSources();
        sPage.render();
    },
    /**
     * Set up the popovers based on if the selector has a description.
     */
    setupPopOvers: function() {
        var view = this;
        view.managedService.metatype.forEach(function(each) {
            if(!_.isUndefined(each.get("description"))) {
                var options,
                    selector = ".description[data-title=" + each.id + "]";
                options = {
                    title: each.get("name"),
                    content: each.get("description"),
                    trigger: 'hover'
                };
                view.$(selector).popover(options);
            }
        });
    }
});

