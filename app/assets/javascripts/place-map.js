window.PlaceMap = function(mapId, coord) {
  var awmcLayer = L.tileLayer('http://a.tiles.mapbox.com/v3/isawnyu.map-knmctlkh/{z}/{x}/{y}.png', {
    attribution: 'Tiles and Data &copy; 2013 <a href="http://www.awmc.unc.edu" target="_blank">AWMC</a> ' +
                 '<a href="http://creativecommons.org/licenses/by-nc/3.0/deed.en_US" target="_blank">CC-BY-NC 3.0</a>'});
  
  var map = new L.Map(mapId, {
    center: coord,
    zoom: 7,
    layers: [awmcLayer]
  });
  
  L.marker(coord).addTo(map);
};
