var SourcePage = Backbone.View.extend({
    events: {
        'click .refreshButton' : 'refreshSources',
        'click .addSourceLink' : 'addSource'
    },
    initialize: function() {
        _.bindAll(this, "refreshSources");
    },
    render: function() {
        this.$el.html(ich.listTemplate(sList.toJSON()));
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
        var msfView = new ManagedServiceFactoryView({managedServiceFactoryList: msfList});
        $("#main").html(msfView.render().el);
    }
});

var SourceTable = Backbone.View.extend({
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

var SourceRow = Backbone.View.extend({
    tagName: "tr",
    template: _.template("<% _.each(attrs, function(value) { %> <td><%= value %></td> <% }); %>"),
    events: {
        'click .editLink' : 'editSource'
    },
    render: function() {
        this.$el.html(this.template({attrs: [
            this.createNameHtml((this.model.shortName) ? this.model.shortName : this.model.id),
            this.model.fpid,
            this.createStatusHtml(this.model.sourceStatus)]}));
        return this;
    },
    createNameHtml: function(shortname) {
        return "<a href='#' class='editLink'>"+shortname+"</a>";
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
        var federatedView = new ManagedServiceFactoryView({sourceModel:this.model, managedServiceFactoryList: msfList});
        $("#main").html(federatedView.render().el);
    }
});