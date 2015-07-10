define(['common/formatting', 'peripleo-ui/events/events', 'peripleo-ui/controls/filter/facetFilterParser'], function(Formatting, Events, FacetFilterParser) {
  
  var OPACITY_UNSELECTED = 0.4;

  var FilterEditor = function(eventBroker) {
    
    var element = jQuery(
          '<div class="clicktrap">' +
          '  <div class="modal-editor" id="filter-editor">' +
          '   <span class="close icon">&#xf057;</span>' +
          '   <div class="buttons">' +
          '     <span class="btn select-all"><span class="icon">&#xf046;</span> <span class="label">Select all</span></span>' +
          '     <span class="btn select-none"><span class="icon">&#xf096;</span> <span class="label">Select none</span></span>' +
          '   </div>' +
          '   <ul class="chart large"></ul>' +
          '  </div>' +
          '</div>'
        ),
        
        otherTemplate = 
          '<li class="selected other">' +
          '  <span class="icon selection-toggle">&#xf046;</span>' +
          '  <div class="label-other">Other</div>' +
          '</li>',
        
        btnSelectAll = element.find('.select-all'),
        btnSelectNone = element.find('.select-none'),
        btnClose = element.find('.close'),
        
        list = element.find('.chart'),
        
        dimension,
        
        selectAll = function() {
          getSelectedValues();
          select(list.find('li').not('.other'));
          selectOther();   
        },
        
        selectNone = function() {
          deselect(list.find('li').not('.other'));
          deselectOther();   
        },
        
        select = function(element) {
          element.addClass('selected');
          element.find('.meter').css('opacity', 1);
          element.find('.icon').html('&#xf046;');       
        },
        
        deselect = function(element) {
          element.removeClass('selected');
          element.find('.meter').css('opacity', OPACITY_UNSELECTED);
          element.find('.icon').html('&#xf096;');       
        },
        
        toggle = function(element) {
          if (element.hasClass('selected'))
            deselect(element);
          else
            select(element);
        },

        selectOther = function() {
          var li = element.find('.other');
          li.addClass('selected');
          li.find('.label-other').css('opacity', 1);
          li.find('.icon').html('&#xf046;');
        },
        
        deselectOther = function() {
          var li = element.find('.other');
          li.removeClass('selected');
          li.find('.label-other').css('opacity', OPACITY_UNSELECTED);
          li.find('.icon').html('&#xf096;');
        },
        
        toggleOther = function() {
          if (element.find('.other').hasClass('selected'))
            deselectOther();
          else
            selectOther();
        },
        
        clearList = function() {
          list.removeClass();
          list.empty();
        },
        
        getSelectedValues = function() {
          return jQuery.map(list.find('li.selected').not('.other'), function(li) {
            return jQuery(li).attr('data-value');
          });
        },
        
        getUnselectedValues = function() {
          return jQuery.map(list.find('li').not('.selected').not('.other'), function(li) {
            return jQuery(li).attr('data-value');
          });
        },
        
        isOtherSet = function() {
          return element.find('.other').hasClass('selected');
        },
        
        sortFacetValues = function(a,b) { return b.count - a.count },
        
        createList = function(facetValues) {
          var facets = facetValues.facets,
              maxCount = (facets.length > 0) ? facets.slice().sort(sortFacetValues)[0].count : 0;
          
          dimension = facetValues.dimension;
              
          clearList();
          
          list.addClass('chart large ' + dimension);
 
          jQuery.each(facets, function(idx, val) {
            var label = Formatting.formatFacetLabel(val.label),
                tooltip = Formatting.formatNumber(val.count) + ' Results',
                percentage = 100 * val.count / maxCount,
                li = Formatting.createMeter(label, tooltip, percentage);
               
            li.addClass('selected');
            li.attr('data-value', val.label);
            li.prepend('<span class="icon selection-toggle">&#xf046;</span>');
            li.click(function() { toggle(li); });
            list.append(li);
          });
          
          list.append(otherTemplate);
          element.show();  
        },
        
        close = function() {
          // 'Inclusive' or 'exclusive' filtering? I.e. does the user want to 
          // see selected categories AND others not in the list (inclusive)? Or
          // restrict to ONLY the selected ones (exclusive)?
          var inclusiveFiltering = isOtherSet(),
              facets, searchParams;
          
          if (inclusiveFiltering)
            facets = getUnselectedValues(); // Inclusive: supress unselected items 
          else
            facets = getSelectedValues(); // Exclusive: show only selected items
            
          // Sanitize 0-length filter value array to 'false'
          if (facets.length === 0)
            facets = false;
          
          searchParams = FacetFilterParser.parse(dimension, facets, inclusiveFiltering);
                    
          eventBroker.fireEvent(Events.FILTER_SETTINGS_CHANGED, { dimension: dimension, filters: searchParams }); 
          eventBroker.fireEvent(Events.SEARCH_CHANGED, searchParams);
            
          element.hide();
        };
      
    element.hide();
    jQuery(document.body).append(element);

    btnSelectAll.click(selectAll);
    btnSelectNone.click(selectNone);
    btnClose.click(close);    
    
    list.on('click', '.other', toggleOther);
    
    eventBroker.addHandler(Events.EDIT_FILTER_SETTINGS, createList);
  
  };
  
  return FilterEditor;
    
});
