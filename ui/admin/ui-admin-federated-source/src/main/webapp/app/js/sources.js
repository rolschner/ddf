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

//NOTE:Hardcoded link to Kimo's page for now....
var pageHtml = "<a href='add.html' class='addSourceLink'><span class='icon-plus'/>Add Source</a>"+
        "<table class='sourcesTable table table-striped'><thead><th>Name</th>"+
        "<th>Type</th><th>Status</th></thead><tbody></tbody></table>"+
        "<button class='refreshButton btn btn-info'>Refresh</button>";
var sList;
var sPage;

// Backbone Objects
var Source;
var SourceList;
var SourceTable;
var SourceRow;
var editRouter;

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

    var options = {
        // default delay is 1000ms
        delay: 30000
    }

    var poller = Backbone.Poller.get(sList, options);
    poller.start();

    sPage = new SourcePage({
        el: $(".sourcesMain")
    });
    sPage.render();
    
    editRouter = new EditRouter();

    Backbone.history.start();
}

/**
 * Bootstrap goodness begins here!
 **/
function initializeBackboneObjects(){
    Source = Backbone.Model.extend({
        defaults: {
            "id": "N/A",
            "bundle_name" : "N/A",
            "name" : "N/A",
            "fpid" : "N/A",
            "bundle" : "N/A",
            "shortName" : "N/A",
            "sourceStatus" : "Available",
            "properties" : {}
        },
        initialize: function(sourceJson) {

			// MBean AI Results
			console.log(sourceJson);

			// it appears as the fields aren't getting set via their defaults
			if(sourceJson.id){
				this.id = sourceJson.id; 
			}

			if(sourceJson.bundle_name){
				this.bundle_name = sourceJson.bundle_name; 
			}

			if(sourceJson.name){
				this.name = sourceJson.name; 
			}

			if(sourceJson.fpid){
				this.fpid = sourceJson.fpid; 
			}

			if(sourceJson.bundle){
				this.bundle = sourceJson.bundle; 
			}

			if(sourceJson.properties.shortname) {
				  this.shortName = sourceJson.properties.shortname;
	        }

            if(sourceJson.available === true) {
                this.sourceStatus = "Available";
            }
            else if(sourceJson.available === false) {
                this.sourceStatus = "Not Available";
            }
            else {
                this.sourceStatus = "Unknown";
            }

			if(sourceJson.properties){
				this.properties = sourceJson.properties;
			}

			/* 
			 * TODO: implement actually checking the source's status
            if(sourceJson.available == true) {
                this.sourceStatus = "Available";
            } else if (sourceJson.available == false) {
                this.sourceStatus = "Not Available";
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
        url : "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredConfigurations",
        sync: function(method, model, options) {
            //options.dataType = "jsonp";
            options.dataType = "json";
            return Backbone.sync(method, model, options);
        },
        parse: function (response) {
		   return response.value;
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
//        events: {
//            'click .editLink' : 'editSource'
//        },
        render: function() {
            this.$el.html(this.template({attrs: [
                        this.createNameHtml(this.model.shortName),
                        this.model.fpid,
                        this.createStatusHtml(this.model.sourceStatus)]}));
            return this;
        },
        createNameHtml: function(shortname) {
            return "<a href='#edit/" + this.model.id +"' class='editLink'>"+shortname+"</a>";
        },
        createStatusHtml: function(sourceStatus) {
            var labelClass = "label ";
            if(sourceStatus === "Available") {
                labelClass += "label-success";
            }
            else if(sourceStatus === "Not Available") {
                labelClass += "label-important";
            }
            else if(sourceStatus === "Unknown") {
                labelClass += "label-warning";
            }

            return "<span class='"+labelClass+"'>"+sourceStatus+"</span>";
        },
        editSource: function() {
            alert("TODO: Create Edit Dialog" + this.model.id);
//            var editFedSourceView = new AddFederatedView({collection: collection, model: model, modelToSend: modelToSend});
//            $("#sourcesMain").html(editFedSourceView.render().el);
//            window.location.href = "add.html"
        }
    });
    
    EditRouter = Backbone.Router.extend({
    	routes: {
    		"edit/:servicepid" : "edit"
    	},
    	edit: function(servicepid){
    		alert("Edit Route" + servicepid);
//            var model = new Backbone.Model();
//            var collection = new MetaType.Collection();
//            var managedServiceFactory = new ManagedServiceFactory();
//            managedServiceFactory.serviceFactoryPid = "cool-id";
//            managedServiceFactory.name = "cool-name";
//    		this.navigate("add.html?servicepid=" +servicepid);
    		
//    		window.location = "add.html?servicepid=" + servicepid;
//    		var federatedView = new AddFederatedView({collection: collection, model: model, managedServiceFactory: managedServiceFactory});
//    		$("#sourcesMain").html(federatedView.render().el);
    	}
    	
    });
}
