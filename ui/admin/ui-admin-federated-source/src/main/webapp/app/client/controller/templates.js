/**
 * Loading all the templates and getting the listDefaultFilteredFactoryConfiguration.
 */
$(document).ready(function(){
    var promises = [],
        url = "/hawtio/jolokia/exec/ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listDefaultFilteredFactoryConfigurations/";

    promises.push($.ajax("app/templates/editSourceTemplate.html"));
    promises.push($.ajax({url: "app/templates/templates.json", dataType:"json"}));
    promises.push($.ajax({url: url, dataType:"json"}));
    $.when.apply(null, promises).done(function(template1, template2, data){
        if (template1 && template1.length > 0 && template2 && template2.length > 0 && data && data.length > 0) {

            ich.addTemplate("editTemplate", template1[0]);
            _.each(template2[0], function(template) {
                ich.addTemplate(template.name, template.template);
            });

            updateData(data[0]);
        }
    }).fail(function(error){
        if (error.status === 403) {
            window.location.replace("/hawtio/index.html#/login");
        } else {
            alert("an error happened " + error.statusText);
        }
    });
});