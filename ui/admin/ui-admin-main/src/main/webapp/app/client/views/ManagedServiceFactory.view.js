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
        "change .sourceTypesSelect" : "render"
    },

    /**
     * Initialize  the binder with the ManagedServiceFactory model.
     * @param options
     */
    initialize: function(options) {
        _.bindAll(this, "render", "close", "cancel", "setupPopOvers", "renderDynamicFields", "submitData", "captureSelectedType", "toggleEnable");
        if(_.isUndefined(options.managedServiceFactory)) {
            if(options.managedServiceFactoryList.at(0))
            {
                options.managedServiceFactory = options.managedServiceFactoryList.at(0);
            }
            else
            {
                options.managedServiceFactory = new ManagedServiceFactory();
            }
        }
        if(_.isUndefined(options.sourceModel))
        {
            options.sourceModel = options.managedServiceFactory.createNewSource();
        }
        this.managedServiceFactory = options.managedServiceFactory;
        this.managedServiceFactoryList = options.managedServiceFactoryList;
        this.source = options.sourceModel;
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
        var selectedType = view.captureSelectedType();
        var jsonObj = view.managedServiceFactory.toJSON();
        view.$el.html("");
        view.$el.append(ich.editTemplate(jsonObj));
        view.$(".sourceTypesSelect").html("");
        view.renderTypeDropdown();
        view.$(".sourceTypesSelect").val(selectedType);
        view.$(".data-section").html("");
        view.renderDynamicFields();
        view.setupPopOvers();
        view.modelBinder.bind(view.source.configuration, view.$(".add-federated-source"));
        return view;
    },
    /**
     * Renders the type dropdown box
     */
    renderTypeDropdown: function() {
        var view = this;
        view.$(".sourceTypesSelect").append(ich.optionListType({"list": view.managedServiceFactoryList.toJSON()}));

        //set the selected type so the page is rendered correctly if we are editing
        //see if the source has an id, if it does, we are editing
        if(view.source.id)
        {
            //if this doesn't have an fpid it isn't a managed service factory
            //if it isn't a managed service factory then we can't select anything in the drop down
            if(view.source.get("fpid"))
            {
                view.$(".sourceTypesSelect").val(view.source.get("fpid"));
            }
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

        view.managedServiceFactory.metatype.forEach(function(each) {
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
        view.managedServiceFactory.metatype.forEach(function(each) {
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
    captureSelectedType: function() {
        var view = this;
        var selectedValue = view.$(".sourceTypesSelect").val();
        if(selectedValue)
        {
            view.managedServiceFactoryList.forEach(function(each) {
                if(each.get("id") === selectedValue) {
                    view.managedServiceFactory = each;
                    view.source = view.managedServiceFactory.createNewSource();
                }
            });
        }
        else //this is only for first load where this component isn't even rendered yet
        {
            selectedValue = view.managedServiceFactory.get("id");
        }
        return selectedValue;
    }
});
