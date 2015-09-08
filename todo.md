# TODOs & Future Features

* While in local search, the 'Show all results' hit is confusing. It
  should better convey that this number relates to all results on the current map area,
  e.g. something like 'Total results in map area:'.
* Preview thumbnails are currently not 'local search sensitive'. They definitely MUST
  be restricted to the place while in local search, possibly SHOULD be restricted to
  the selected place (at least at the time of selection, until next map pan).
* API feature request: the /places/{uri} method should have an option for limiting the number
  of datasets listed in the 'referenced_in' field. Presently, this limit is hard-wired to the
  top 10 referencing datasets.
* Re-implement proper display of text snippet previews (plus
  correct behavior of the JSON `snippet` field).
* Revise hull computation (less smoothing, use original geometry
  when hull is only based on a single place).
* Proper handling of dataset objects. (Also think about what to
  do when a dataset is selected from the search result list.)
* Support PeriodO periods.
* Timeslider touch Support.
* Timeslider: 1AD marker overlap issue (when interval boundary
  is close to 1AD).
* Timeslider: general visibility of current interval setting?
* 'You have filters' warning icon (plus button to clear all
  filters at once?)
* Filter editor popup: replace current X icon with checkmark + X
  icon for 'Accept'/'Cancel' functionality.
* Help window (also document search syntax - prefix, fuzzy,
  booleans).
* Proper 404 page.
* Wait spinners all over.
* Clean up custom icons (roll everything into an icon font).
* How to handle objects contained in René's gazetteer?
* __BUG:__ orphaned selection marker.
