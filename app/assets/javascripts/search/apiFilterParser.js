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
        
        'type': function(values) {
          
        }
        
      };
  
  return {
    
    parseFacetFilter: function(args) {
      var apiName;
      
      if (args.hasOwnProperty('facetFilter')) {
        var translatedArgs = 
          parse[args.facetFilter.dimension](args.facetFilter.values, args.facetFilter.exclusive);
          
        jQuery.extend(args, translatedArgs);
        delete args.facetFilter;
      }
      
      return args;      
    }
    
  };
  
});
