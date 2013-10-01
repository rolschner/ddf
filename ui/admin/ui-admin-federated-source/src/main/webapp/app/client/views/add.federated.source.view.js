/** Main view page for add. */
var AddFederatedView = Backbone.View.extend({
    /**
     * Button events, right now there's a submit button
     * I do not know where to go with the cancel button.
     */
    events: {
        "click .submit-button": "submitData",
        "click .enable-checkbox" : "toggleEnable" 
    },

    /**
     * Initialize  the binder with the FederatedSource model.
     * @param options
     */
    initialize: function(options) {
        _.bindAll(this, "render", "close", "setupPopOvers", "renderDynamicFields", "submitData");
        if(_.isUndefined(options.modelToSend)) {
            options.modelToSend = new FederatedSource();
        }
        this.modelToSend = options.modelToSend;
        console.log("federated source model: " + this.modelToSend.isEnabled);
        this.modelBinder = new Backbone.ModelBinder();
    },

    /**
     * This is where everything is rendered and the model is binded to the dom.
     * Add the main template
     * Clear whats in there now
     * Render the fields available
     * Setup popovers for description
     * Bind the model to the dom
     * return view for rendering to where the caller wants it to render.
     */
    render: function() {
        var view = this,
            jsonObj = view.model.toJSON();
        view.$el.append(ich.mainTemplate(jsonObj));
        view.$(".data-section").html("");
        view.$(".data-section").append(ich.checkboxEnableType(view.modelToSend.toJSON()));
        view.renderDynamicFields();
        view.setupPopOvers();
        view.modelBinder.bind(view.modelToSend, view.$(".add-federated-source"),
            null, {initialCopyDirection: Backbone.ModelBinder.Constants.ViewToModel});
        return view;
    },

    /**
     * Walk the collection of metatypes
     * Setup the ui based on the type
     * Append it to the bottom of this data-section selector
     */
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
    /**
     * Submit to the backend.
     */
    submitData: function() {
      var view = this;
      view.modelToSend.save();
    },
    /**
     * unbind the model and dom during close.
     */
    close: function(){
      var view = this;
      view.modelBinder.unbind();
    },
    /**
     * Set up the popovers based on if the selector has a description.
     */
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

    },
    
    toggleEnable: function() {
        var view = this;
        console.log('toggling enable field of model'); 
        if(this.modelToSend.isEnabled){
            console.log('currently enabled.  disabling.'); 
    	    this.modelToSend.isEnabled = false;
    	}
    	else {
    	    console.log('currently disabled.  enabling.');
    	    this.modelToSend.isEnabled = true;
    	}
    }
});

