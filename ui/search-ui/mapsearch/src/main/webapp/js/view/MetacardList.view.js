/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        List = {};

    ich.addTemplate('resultListItem', require('text!templates/resultListItem.handlebars'));
    ich.addTemplate('resultListTemplate', require('text!templates/resultList.handlebars'));


    List.MetacardRow = Marionette.ItemView.extend({
        tagName: "tr",
        template : 'resultListItem',
        events: {
            'click .metacard-link': 'viewMetacard'
        },

        initialize: function (options) {
            _.bindAll(this);
            this.searchControlView = options.searchControlView;
        },

        onRender : function(){
            if(this.model.get('context')){
                this.$el.addClass('selected');
            }
        },

        viewMetacard: function () {
            if(this.model.get('context')){
                this.searchControlView.showMetacardDetail(this.model);
            }
            this.model.set('context', true);
        }

    });

    List.MetacardTable = Marionette.CollectionView.extend({
        itemView : List.MetacardRow,
        initialize: function (options) {
            this.searchControlView = options.searchControlView;
        },
        itemViewOptions : function(model){
            return {
                model : model.get('metacard'),
                searchControlView : this.searchControlView
            };
        }
    });

    List.MetacardListView = Backbone.View.extend({
        className : 'slide-animate',
        events: {
            'click .load-more-link': 'loadMoreResults'
        },
        initialize: function (options) {
            _.bindAll(this);
            //options should be -> { results: results, mapView: mapView }
            this.model = options.result;
            this.searchControlView = options.searchControlView;
        },
        render: function () {
            this.$el.html(ich.resultListTemplate(this.model.toJSON()));
            var metacardTable = new List.MetacardTable({
                collection: this.model.get("results"),
                el: this.$(".resultTable").children("tbody"),
                searchControlView: this.searchControlView
            });
            metacardTable.render();
            this.metacardTable = metacardTable;
            this.showHideLoadMore();
//            var selected = this.$el.find('.selected');
//            var container = $('#searchPages');
//            if(selected.length !== 0)
//            {
////                console.log(selected);
////                container.scrollTop(selected.offset().top - container.offset().top + container.scrollTop());
//                container.scrollTop(400);
////                $('#searchPages').animate({ scrollTop: selected.offset().top}, 1000);
//            }

            this.delegateEvents();
            return this;
        },
        close: function () {
            this.remove();
            this.stopListening();
            this.unbind();
            this.metacardTable.close();
        },
        loadMoreResults: function () {
            var view = this;
            this.model.loadMoreResults().complete(function() {
                view.showHideLoadMore();
            });
        },
        showHideLoadMore: function() {
            if (this.model.get("results").length >= this.model.get("hits") || this.model.get("hits") === 0) {
                $(".load-more-link").hide();
            }
            else {
                $(".load-more-link").show();
            }
        }
    });

    return List;

});
