/** Main view page for add. */
var ManagedServiceFactoryView = Backbone.View.extend({
    /**
     * Button events, right now there's a submit button
     * I do not know where to go with the cancel button.
     */
    events: {
        "click .submit-button": "submitData",
        "click #cancel": "cancel",
        "click .enable-checkbox" : "toggleEnable",
        "change .sourceTypesSelect" : "renderDisplay"
    },

    /**
     * Initialize  the binder with the ManagedServiceFactory model.
     * @param options
     */
    initialize: function(options) {
        _.bindAll(this, "render", "close", "cancel", "setupPopOvers", "renderDisplay", "renderDynamicFields", "submitData", "setSelectedType", "toggleEnable");
        if(_.isUndefined(options.managedServiceFactory)) {
            options.managedServiceFactory = new ManagedServiceFactory();
        }
        if(!_.isUndefined(options.sourceModel))
        {
            options.managedServiceFactory.attributes = _.clone(options.sourceModel.properties);
        }
        this.managedServiceFactory = options.managedServiceFactory;
        this.managedServiceFactoryList = options.managedServiceFactoryList;
        this.model = options.sourceModel;
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
        var view = this,
            jsonObj = view.managedServiceFactory.toJSON();
        view.$el.append(ich.editTemplate(jsonObj));
        view.$(".sourceTypesSelect").html("");
        view.renderTypeDropdown();
        return view.renderDisplay();
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
    renderDisplay: function() {
        var view = this;
        view.$(".data-section").html("");
        view.setSelectedType();
        view.renderDynamicFields();
        view.setupPopOvers();
        view.modelBinder.bind(view.managedServiceFactory, view.$(".add-federated-source"),
            null, {initialCopyDirection: Backbone.ModelBinder.Constants.ViewToModel});
        return view;
    },
    /**
     * Renders the type dropdown box
     */
    renderTypeDropdown: function() {
        var view = this;
        view.$(".sourceTypesSelect").append(ich.optionListType({"list": view.managedServiceFactoryList.toJSON()}));
        if(view.model)
        {
            view.$(".sourceTypesSelect").val(view.model.fpid);
        }
    },

    /**
     * Walk the collection of metatypes
     * Setup the ui based on the type
     * Append it to the bottom of this data-section selector
     */
    renderDynamicFields: function() {
        var view = this;
        //view.$(".data-section").append(ich.checkboxEnableType(view.managedServiceFactory.toJSON()));

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

        if(view.model)
        {
            for(var property in view.model.properties)
            {
                view.$("#"+property).val(view.model.properties[property]);
            }
        }
    },
    /**
     * Submit to the backend.
     */
    submitData: function() {
        var view = this;
        view.managedServiceFactory.save();
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
        sPage.render();
        sPage.refreshSources();
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
        if(this.managedServiceFactory.isEnabled){
            console.log('currently enabled.  disabling.'); 
    	    this.managedServiceFactory.isEnabled = false;
    	}
    	else {
    	    console.log('currently disabled.  enabling.');
    	    this.managedServiceFactory.isEnabled = true;
    	}
    },
    /**
     * Sets the selected type from the dropdown
     */
    setSelectedType: function() {
        var view = this;
        var selectedValue = view.$(".sourceTypesSelect").val();
        view.managedServiceFactoryList.forEach(function(each) {
            if(each.get("id") === selectedValue) {
                view.managedServiceFactory.name = each.get("name");
                view.managedServiceFactory.serviceFactoryPid = each.get("id");
                view.collection = new MetaType.Collection(each.get("metatype"));
            }
        });
    }
});

