
var Search = {};
   Search.Page = Backbone.View.extend({
    events: {
        'click .click-search':'doSearch'
    },
    initialize: function () {
        _.bindAll(this, "render", "doSearch", "searchReceived");
    },
    render: function () {
        var view = this;
        view.$el.html(ich.searchButton());
        return view;
    },

    doSearch: function() {
        var view = this;
        view.results = new Results.Collection();
        $.when(view.results.fetch()).then(view.searchReceived, view.errorHappened);
    },
    searchReceived: function() {
        var view = this;
        if(view.resultsView) {
            // close is a no op, but you should call it when swapping views.  In here you close all the children views,
            //stop listening for events.
            view.resultsView.close();
        }
        view.resultsView = new Search.Results({collection: view.results});
        view.$(".results").html(view.resultsView.render().el);
        view.resultsView.renderDataTable();
    },
    errorHappened: function() {
        alert("what happened?");
    }
   });
   Search.Results = Backbone.View.extend({
     initialize: function() {
         _.bindAll(this,"render");
     },
     render: function() {
         var view = this;
         view.$el.html(ich.searchResults({"results": view.collection.toJSON()}));
         return view;
     },

     renderDataTable: function() {
         var outputTable = $("#resultTable");
        outputTable.dataTable({
            bLengthChange: false,
            bFilter: true,
            sDom: 'lfrtpi',
            aoColumnDefs: [
                {
                    bSortable: false,
                    aTargets: [1]
                }
            ]
        });
     }


   });



