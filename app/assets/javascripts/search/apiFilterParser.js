define([], function() {
  
  var parse = {
    
        /** Datasets and gazetteers **/
        source_dataset: function(values, exclusive) {
          var datasetIDs = [], datasetFilter,
              gazetteerIDs = [], gazetteerFilter;
          
          if (values) {    
            jQuery.each(values, function(idx, value) {
              if (value.indexOf('gazetteer:') === 0)
                gazetteerIDs.push(value.substring(10));
              else
                datasetIDs.push(value.substring(value.indexOf('#') + 1));
            });
          }
          
          datasetFilter = (datasetIDs.length === 0) ? false : datasetIDs.join(',');
          gazetteerFilter = (gazetteerIDs.length === 0) ? false : gazetteerIDs.join(',');
            
          if (exclusive)
            return { 
              datasets: datasetFilter, exclude_datasets: false,
              gazetteers: gazetteerFilter, exclude_gazetteers: false }
          else 
            return { 
              datasets: false, exclude_datasets: datasetFilter,
              gazetteers: false , exclude_gazetteers: gazetteerFilter }             
        },
        
        /** Object type (place, item, dataset) **/
        type: function(values, exclusive) {
          var typeFilter = (values) ? values.join(',') : false;
          
          if (exclusive)
            return { object_types: typeFilter, exclude_object_types: false };
          else
            return { object_types: false, exclude_object_types: typeFilter };
        }
        
      };
  
  return {
    
    parseFacetFilter: function(args) {
      var newArgs = jQuery.extend({}, args), // clone & leave the original unmutated
          translatedArgs;
      
      if (args.hasOwnProperty('facetFilter')) {
        translatedArgs = 
          parse[args.facetFilter.dimension](args.facetFilter.values, args.facetFilter.exclusive);
          
        jQuery.extend(newArgs, translatedArgs);
        delete newArgs.facetFilter;
      }
      
      return newArgs;      
    }
    
  };
  
});
