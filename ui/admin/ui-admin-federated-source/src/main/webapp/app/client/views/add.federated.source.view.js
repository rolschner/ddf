var AddFederatedView = Backbone.View.extend({
    events: {
        "click .advance-options" : "toggleAdvance",
        "click .submit-button": "submitData"
    },

    initialize: function() {
        _.bindAll(this, "render", "toggleAdvance", "close", "setupPopOvers");
        this.modelToSend = new FederatedSource();
        this.modelBinder = new Backbone.ModelBinder();
    },

    render: function() {
        var view = this,
            jsonObj = view.model.toJSON();
        view.$el.append(ich.mainTemplate(jsonObj));
        view.$(".data-section").html("");
        view.renderDynamicFields();
        view.setupPopOvers();
        view.modelBinder.bind(view.modelToSend, view.$(".add-federated-source"),
            null, {initialCopyDirection: Backbone.ModelBinder.Constants.ViewToModel});
        return view;
    },
    renderDynamicFields: function() {
        var view = this;

        view.collection.forEach(function(each) {
           var type = each.get("type");
           if(!_.isUndefined(type)) {
               if (type === 1) {
                   view.$(".data-section").append(ich.textType(each.toJSON()));
               } else if (type === 11) {
                   view.$(".data-section").append(ich.checkboxType(each.toJSON()));
               } else if (type === 12) {
                   view.$(".data-section").append(ich.passwordType(each.toJSON()));
               }

           }
        });
    },
    submitData: function() {
      var view = this;
      view.modelToSend.save();
    },

    close: function(){
      var view = this;
      view.modelBinder.unbind();
    },

    toggleAdvance: function() {
        var view = this;
        view.$(".advance-section").animate({height:"toggle"});
    },

    setupPopOvers: function() {
        var view = this;
        view.collection.forEach(function(each) {
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

