define([], function() {
      
      /** Helper to properly concatenate two strings, which may both be 'false' **/
  var merge = function(currentFilterString, newFilterString) {
        if (currentFilterString && newFilterString)
          return currentFilterString + ',' + newFilterString;
        else if (currentFilterString)
          return currentFilterString;
        else if (newFilterString)
          return newFilterString
        else
          return false;
      },
  
      parse = {
    
        /** Datasets and gazetteers **/
        source_dataset: function(values, inclusive, currentFilters) {
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
            
          if (inclusive) // In this case, we need to merge with the current filter!
            return { 
              datasets: false, exclude_datasets: merge(currentFilters.exclude_datasets, datasetFilter),
              gazetteers: false , exclude_gazetteers: merge(currentFilters.exclude_gazetteers, gazetteerFilter) };
          else // In this case, we just replace the current filter
            return { 
              datasets: datasetFilter, exclude_datasets: false,
              gazetteers: gazetteerFilter, exclude_gazetteers: false };
        },
        
        /** Object type (place, item, dataset) **/
        type: function(values, inclusive, currentFilters) {
          var typeFilter = (values) ? values.join(',') : false;
          
          if (inclusive) // In this case, we need to merge with the current filter!
            return { object_types: false, exclude_object_types: merge(currentFilters.exclude_object_types, typeFilter) };
          else // In this case, we just replace the current filter
            return { object_types: typeFilter, exclude_object_types: false };
        }
        
      };
  
  return {
    
    parseFacetFilter: function(diff, currentFilters) {
      var clonedDiff = jQuery.extend({}, diff), // clone & leave the original unmutated
          translatedFacets; // Will hold the diff, translated to API terminology
      
      if (clonedDiff.hasOwnProperty('facetFilter')) {
        translatedFacets = 
          parse[clonedDiff.facetFilter.dimension](clonedDiff.facetFilter.values, clonedDiff.facetFilter.inclusive, currentFilters);
          
        jQuery.extend(clonedDiff, translatedFacets);
        delete clonedDiff.facetFilter;
      }
      
      return clonedDiff;      
    }
    
  };
  
});
