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

var baseUrl = "https://localhost:8993";
var sList;
var sView;
var Source;
var SourceList;
var SourceTable;
var SourceRow;

$(function(){
  loadSources();
});

function loadSources(){
    $.ajax(
    {
        url: baseUrl+"/services/catalog/sources",
        dataType: "jsonp"
    }).done(initializeOnScriptLoad);
}

function initializeOnScriptLoad(sources) {
    checkIfLoaded(sources, 0);
}

//Checks if Bacbone is loaded before proceeding
function checkIfLoaded(sources, counter) {
    if( (typeof Backbone.Collection) === "function"){
        initializeBackboneObjects();
        instantiateSources(sources);
    }else{
        if(counter === undefined){
            counter = 0;
        }else if(counter > 20){
            alert("Error loading JavaScript");
            return;
        }
        counter += 1;
        setTimeout(function(){checkIfLoaded(sources, counter);},100);
    }
}

function instantiateSources(sources) {
    if(sView !== undefined){
        //Should eventually be replaced by some sView.remove() call
        $("#sourcesTable tbody").html("");
    }
    sList = new SourceList();

    for(s in sources){
        var newS = new Source(sources[s]);
        sList.add(newS);
    }

    sView = new SourceTable({
                collection: sList,
                el: $("#sourcesTable tbody")
            });
    sView.render();
}

function refresh(){
    $.ajax(
        {
            url: baseUrl+"/services/catalog/sources",
            dataType: "jsonp"
        }).done(function(sources){
            var sourceArray = [];
            for(s in sources){
                var newS = new Source(sources[s]);
                sourceArray.push(newS);
            } 
            sList.set(sourceArray);
        });
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
        },
        getContentTypesAsString: function() {
            return this.contentTypes.toString();
        }
    
    });

    SourceList = Backbone.Collection.extend({
        model: Source
    });

    SourceTable = Backbone.View.extend({
        sourceRows: [],
        initialize: function(){
            _.bindAll(this, 'appendSource', 'render', 'removeSource');
            this.collection.bind("add", this.appendSource);
            this.collection.bind("remove", this.removeSource);
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
                if(this.sourceRows[i].model == s) {
                    this.sourceRows[i].remove();
                    this.sourceRows.splice(i,1);
                    break;
                }
            }
        }
    });

    SourceRow = Backbone.View.extend({
        tagName: "tr",
        template: _.template("<% _.each(attrs, function(value) { %> <td><%= value %></td> <% }); %>"),
        render: function() {
            this.$el.html(this.template({attrs: [
                        this.model.sourceStatus,
                        this.model.id,
                        this.model.version]}));
            return this;
        },
        createTd: function(val) {
            return "<td>"+val+"</td>";
        }
    });
}
