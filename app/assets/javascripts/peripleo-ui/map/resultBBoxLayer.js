define(['common/hasEvents', 'peripleo-ui/events/events'], function(HasEvents, Events) {

  var BBOX_STYLE = {
        color: '#a64a40',
        opacity: 0.8,
        fillColor: '#e75444',
        fillOpacity: 0.4,
        weight:1
      };

  var ResultBBoxLayer = function(map, eventBroker) {

    var bboxFeatures = L.featureGroup().addTo(map),

        onSearchResponse = function(results) {
          bboxFeatures.clearLayers();
          onNextPage(results);
        },

        onNextPage = function(results) {
          jQuery.each(results.items, function(idx, item) {
            var bbox = item.geo_bounds,
                marker;

            if (bbox.min_lat === bbox.max_lat && bbox.min_lon === bbox.max_lon)
              marker = L.marker([ bbox.min_lat, bbox.min_lon ]).addTo(bboxFeatures);
            else
              marker = L.rectangle([[ bbox.min_lat, bbox.min_lon ],
                           [ bbox.max_lat, bbox.max_lon ]], BBOX_STYLE).addTo(bboxFeatures);
          });
        };

    eventBroker.addHandler(Events.API_SEARCH_RESPONSE, onSearchResponse);
    eventBroker.addHandler(Events.API_NEXT_PAGE, onNextPage);

  };

  return ResultBBoxLayer;

});
