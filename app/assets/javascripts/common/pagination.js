require([], function() {
  
  var PageControl = function(element, totalPages, currentPage, pageSize) {
    $(element).twbsPagination({
      totalPages: totalPages,
      startPage: currentPage, 
      first: '&#xf100;',
      prev: '&#xf104;',
      next: '&#xf105;',
      last: '&#xf101;',
        
      onPageClick: function (event, page) {
        var offset = (page - 1) * pageSize;
        // TODO implement
        // location.search = util.buildPageRequestURL(offset, @results.limit);
      }
    });
  };
  
  return PageControl;
  
});
