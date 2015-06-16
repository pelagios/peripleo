window.Heatmap = function(mapId, places) {
  var awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
    attribution: 'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                 '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'});
  
  this.map = new L.Map(mapId, {
    center: new L.LatLng(41.893588, 12.488022),
    zoom: 3,
    layers: [awmcLayer]
  });

  var latlngs = [];
  $.each(places, function(idx, place) {
    if (place.centroid)
      latlngs.push([place.centroid.lat, place.centroid.lon]);
  });    
  L.heatLayer(latlngs, { max: 1.5, maxZoom: 0, radius: 5, blur: 6 }).addTo(this.map);
};

window.Heatmap.prototype.refresh = function() {
  this.map.invalidateSize();
};
