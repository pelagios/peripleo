define(function() {
  
  var SearchAtButton = function(container, eventBroker) {
            
    var element = jQuery('<span><span class="icon">&#xf041;</span> Search at</span>');

    container.hide();
    container.append(element);
    
  };
  
  return SearchAtButton;
  
});
