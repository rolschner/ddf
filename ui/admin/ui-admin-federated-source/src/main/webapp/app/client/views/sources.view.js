var Sources = {};
   Sources.Page = Backbone.View.extend({
    events: {
        'click .click-Add':'doAdd'
    },
    initialize: function () {
        _.bindAll(this, "render", "doAdd");
    },
    render: function () {
        var view = this;
        view.$el.html(ich.searchButton());
        return view;
    },

    doAdd: function() {

    },
   
    errorHappened: function() {
        alert("what happened?");
    }
   });
  


   });