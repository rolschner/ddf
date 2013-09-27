/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

var pageHtml = "<a href='add.html' class='addSourceLink'><span class='icon-plus'/>Add Source</a>"+
        "<table class='sourcesTable table table-striped'><thead><th>Status</th><th>Name</th>"+
        "<th>Version</th></thead><tbody></tbody></table>"+
        "<button class='refreshButton btn btn-info'>Refresh</button>";
var sList;
var sPage;

// Backbone Objects
var Source;
var SourceList;
var SourceTable;
var SourceRow;

$(function(){
    //checkIfLoaded(0)();
    initializeBackboneObjects();
    instantiateSources();
});

//Checks if Bacbone is loaded before proceeding
/*function checkIfLoaded(counter) {
    if( (typeof Backbone.Collection) === "function"){
        initializeBackboneObjects();
        instantiateSources();
    }else{
        if(counter === undefined){
            counter = 0;
        }else if(counter > 20){
            alert("Error loading JavaScript");
            return;
        }
        counter += 1;
        setTimeout(function(){checkIfLoaded(counter);},100);
    }
}*/

function instantiateSources() {

    sList = new SourceList();
    sList.fetch();

    sPage = new SourcePage({
        el: $(".sourcesMain")
    });
    sPage.render();
}

/**
 * Bootstrap goodness begins here!
 **/
function initializeBackboneObjects(){
    Source = Backbone.Model.extend({
        defaults: {
            "id" : "N/A",
            "sourceStatus" : "N/A",
            "version" : "N/A",
            "action" : "N/A",
            "sourceType" : "N/A",
            "contentTypes" : []
        },
        initialize: function(sourceJson) {

			// MBean AI Results
			/*
			var config = sourceJson.value[0].configurations[0];
			console.log("config id = " + config.id);
			console.log("shortName = " + config.properties.shortname);
			*/
			console.log(sourceJson);
			//var sourceJson2 = sourceJson.value[0].configurations;
			//console.log(sourceJson2);

			/*
			$.each( sourceJson.value[0].configurations, function( key, value ) {
				alert( key + ": " + value );
			});
			*/
			// Hardcoded for now
			this.sourceStatus = "Available";

			if(sourceJson.properties.shortname) {
			  this.id = sourceJson.properties.shortname;
            }

            if(sourceJson.fpid) {
				this.version = sourceJson.fpid;
            }


			/*
            if(sourceJson.id) {
                this.id = sourceJson.id;
            }

            if(sourceJson.available == true) {
                this.sourceStatus = "Available";
            } else if (sourceJson.available == false) {
                this.sourceStatus = "Not Available";
            }

            if(sourceJson.version) {
                this.version = sourceJson.version;
            }

            if(sourceJson.contentTypes){
                this.contentTypes = sourceJson.contentTypes;
            }
            */
        },
        getContentTypesAsString: function() {
            return this.contentTypes.toString();
        }

    });

    SourceList = Backbone.Collection.extend({
        model: Source,
        //url: "/services/catalog/sources",
        url : "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredFactoryConfigurations",
        sync: function(method, model, options) {
            //options.dataType = "jsonp";
            options.dataType = "json";
            return Backbone.sync(method, model, options);
        },
        parse: function (response) {
		   return response.value[0].configurations;
		}
    });

    SourcePage = Backbone.View.extend({
        events: {
            'click .refreshButton' : 'refreshSources',
            'click .addSourceLink' : 'addSource'
        },
        initialize: function() {
            _.bindAll(this, "refreshSources");
        },
        render: function() {
            this.$el.html(pageHtml);
            var sTable = new SourceTable({
                collection: sList,
                el: this.$el.children(".sourcesTable").children("tbody")
            });
            sTable.render();
            return this;
        },
        refreshSources: function() {
            sList.fetch();
        },
        addSource: function() {
            //alert("TODO: Create Add Dialog");
        }
    });

    SourceTable = Backbone.View.extend({
        sourceRows: [],
        initialize: function(){
            _.bindAll(this, 'appendSource', 'render', 'removeSource', 'changeSource');
            this.collection.bind("add", this.appendSource);
            this.collection.bind("remove", this.removeSource);
            this.collection.bind("change", this.changeSource);
        },
        render: function() {
            for(m in this.collection.models){
                var newRow = new SourceRow({model: this.collection.models[m]});
                this.sourceRows.push(newRow);
                this.$el.append(newRow.render().el);
            }
            return this;
        },
        appendSource: function(s) {
            var newRow = new SourceRow({model: s});
            this.sourceRows.push(newRow);
            this.$el.append(newRow.render().el);
        },
        removeSource: function(s) {
            for(i in this.sourceRows) {
                if(this.sourceRows[i].model.id == s.id) {
                    this.sourceRows[i].remove();
                    this.sourceRows.splice(i,1);
                    break;
                }
            }
        },
        changeSource: function(change) {
            this.removeSource(change);
            this.appendSource(new Source(change.attributes));
        }
    });

    SourceRow = Backbone.View.extend({
        tagName: "tr",
        template: _.template("<% _.each(attrs, function(value) { %> <td><%= value %></td> <% }); %>"),
        events: {
            'click .editLink' : 'editSource'
        },
        render: function() {
            this.$el.html(this.template({attrs: [
                        this.createStatusHtml(this.model.sourceStatus),
                        this.createNameHtml(this.model.id),
                        this.model.version]}));
            return this;
        },
        createNameHtml: function(id) {
            return "<a href='#' class='editLink'>"+id+"</a>";
        },
        createStatusHtml: function(sourceStatus) {
            var labelClass = "label ";
            if(sourceStatus === "Available") {
                labelClass += "label-success";
            } else if(sourceStatus === "Not Available") {
                labelClass += "label-important";
            }

            return "<span class='"+labelClass+"'>"+sourceStatus+"</span>";
        },
        editSource: function() {
            alert("TODO: Create Edit Dialog");
        }
    });
}
