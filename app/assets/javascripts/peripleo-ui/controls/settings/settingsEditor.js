define(['peripleo-ui/events/events'], function(Events) {

  var SettingsEditor = function(eventBroker) {

    var element = jQuery(
          '<div class="clicktrap">' +
          '  <div class="modal-editor" id="settings-editor">' +
          '   <span class="close icon">&#xf057;</span>' +

          '   <ul id="baselayers"></ul>' +

          '   <ul id="misc-settings">' +
          '     <li class="simplify-polys">' +
          '       <span class="icon activate-bbox-mode">&#xf096;</span>' +
          '       <span class="label">Simplify polygon geometry to bounding boxes</span>' +
          '     </li>' +

          '     <li class="heatmap">' +
          '       <span class="icon activate-heatmap">&#xf096;</span>' +
          '       <span class="label">Show distribution of search results as heatmap <em>(experimental)</em></span>' +
          '     </li>' +

          '     <li class="sample-raster">' +
          '       <span class="icon activate-sample-raster">&#xf096;</span>' +
          '       <span class="label">Show sample raster overlay <em>(demo mode)</em></span>' +
          '     </li>' +
          '   </ul>' +
          '  </div>' +
          '</div>'
        ),

        btnClose = element.find('.close'),

        baseLayers = element.find('#baselayers'),

        btnActivateBBoxMode = element.find('.activate-bbox-mode'),
        btnActivateHeatmap = element.find('.activate-heatmap'),
        btnActivateSampleRaster = element.find('.activate-sample-raster'),

        show = function() {
          var template =
                '<li class="baselayer" >' +
                  '<div class="map-thumb-container"><img class="map-thumb"></div>' +
                  '<h2></h2>' +
                  '<p></p>' +
                '</li>';

          baseLayers.empty();
          element.show();

          jQuery.getJSON('/peripleo/baselayers', function(data) {
            jQuery.each(data, function(idx, layer) {
              var li = jQuery(template);

              li.data('name', layer.name);
              li.find('img').attr('src', layer.thumbnail);
              li.find('h2').html(layer.title);
              li.find('p').html(layer.description);

              baseLayers.append(li);
            });
          });
        },

        close = function() {
          element.hide();
        },

        toggleButton = function(btn, evt) {
          var wasEnabled = btn.hasClass('enabled');
          if (wasEnabled)
            btn.html('&#xf096;');
          else
            btn.html('&#xf046;');
          btn.toggleClass('enabled');
          eventBroker.fireEvent(evt, { enabled: !wasEnabled });
        };

    element.hide();
    jQuery(document.body).append(element);

    btnClose.click(close);

    element.on('click', 'li.baselayer', function(e) {
      var target = jQuery(e.target),
          li = target.closest('li'),
          layerName = li.data('name');

      eventBroker.fireEvent(Events.CHANGE_LAYER, layerName);
      close();
    });

    btnActivateBBoxMode.click(function() {
      toggleButton(btnActivateBBoxMode, Events.TOGGLE_BBOX_MODE);
    });

    btnActivateHeatmap.click(function() {
      toggleButton(btnActivateHeatmap, Events.TOGGLE_HEATMAP);
    });

    btnActivateSampleRaster.click(function() {
      toggleButton(btnActivateSampleRaster, Events.TOGGLE_SAMPLE_RASTER);
    });

    eventBroker.addHandler(Events.EDIT_MAP_SETTINGS, show);

  };

  return SettingsEditor;

});
