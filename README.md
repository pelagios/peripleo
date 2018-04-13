__UPDATE: this version of Peripleo is deprecated. To visit the new, updated version, visit__

__http://peripleo.pelagios.org__

__or__

__http://github.com/pelagios/peripleo2__ (source code)

__The updated version features a partially backwards-compatible API. This means that most of the
documentation below remains valid.__

__Links and documentation text have been updated to reflect this.__



# Peripleo

Peripleo (Greek for "to sail", "to swim around") is a search & browsing engine for data in the [Pelagios network](http://pelagios-project.blogspot.co.uk). Peripleo is work in progress.

## Background - the Pelagios Initiative

Pelagios (Greek for 'of the Sea') is a __community network__ that facilitates __linking of online resources that document the past__, based on the __places they refer to__. From the [large epigraphic database](http://pelagios.org/peripleo/pages/datasets/21b2d56d90bd192834aea9d8ad9d61b21a94d85f15f7cab1c458d4eebf599b73) to the [personal Flickr photostream](http://pelagios.org/peripleo/pages/datasets/ca22250344a3b20d3a79f33c39e703a7f2d9899bd3e3cf6057cd80530f0944e2), each of our partner datasets represents one piece of the puzzle. Pelagios combines these pieces into a coherent whole that enables connection, exchange and discovery - just as the Mediterranean Sea did for the Ancient World.

## Peripleo

An overview of Peripleo & what you can do with it is available at the [Pelagios project blog](http://pelagios-project.blogspot.co.uk/2015/07/peripleo-sneak-preview.html).

## Peripleo API

The __Peripleo API__ provides machine access to our data. The 'mental model' behind Peripleo is simple! It consists of only three types of entities:

* __Items__ such as archaeological artefacts, literary texts, photographs, etc.
* __Places__ to which these items are related, e.g. places mentioned in a text or the findspot of an artefact
* __Datasets__ which are collections of items, e.g. a particular museum collection or data corpus published by an institution

Datasets as well as items can be __hierarchical__. (E.g. the Pelagios 3 dataset is sub-divided into a corpus of
Greek and Latin literary texts. Likewise, an item such as Herodotus' _The Histories_ can be subdivided into individual _Books_.)

## Response Format

The API returns responses in JSON format. [CORS](http://de.wikipedia.org/wiki/Cross-Origin_Resource_Sharing) and
[JSONP](http://en.wikipedia.org/wiki/JSONP) are both supported. Most responses are __paginated__, i.e. you will get back only one "page" of results:

```json
{
  "total" : 112,
  "limit" : 20,
  "items" : [
    ...
  ]
}
```

You can traverse pages using an `offset` and `limit` (= page size) parameter. If `limit` is omitted, it will
default to a page size of 20. Example:

Results 1 - 20:
[http://pelagios.org/peripleo/search?query=gold](http://pelagios.org/peripleo/search?query=gold&prettyprint=true)

Results 21 - 40:
[http://pelagios.org/peripleo/search?query=gold&offset=20](http://pelagios.org/peripleo/search?query=gold&offset=20&prettyprint=true)

## Pretty Printing

Per default, the API will return a compact, unformatted JSON response. You can force a more human-readable response by
appending a `prettyprint=true` parameter. Example:

[http://pelagios.org/peripleo/search?query=bronze&prettyprint=true](http://pelagios.org/peripleo/search?query=bronze&prettyprint=true)

## Searching the API

The main feature you'll probably want to use is __search__. You can search the API by __keyword__,
__place__ (gazetteer URIs), __space__ (geographic area), __time interval__, __dataset__,
__object type__ (i.e. _place_, _item_ or _dataset_) - or any combination of those. A typical search result
record looks like this:

```json
{
  "identifier" : "bb4e2f4b0bc7f4d6c065cb5167f4d3f831ccf795af0204f2647f8ec1bbcabcba",
  "title" : "Periplus of the Euxine Sea",
  "object_type" : "Item",
  "temporal_bounds" : {
    "start" : 130,
    "end" : 130
  },
  "geo_bounds" : {
    "min_lon" : 23.7195,
    "max_lon" : 44.0,
    "min_lat" : 37.5197,
    "max_lat" : 45.5
  }
}
```

The `identifier`, `title` and `object_type` labels are always present. Depending on the object, the record can also
include a short textual `description`, the bounds of the object in space and time (`geo_bounds` and `temporal_bounds`, respectively) and a lists of URLs to `depictions` (images).

In case the result represents and item (rather than a place), the record can also include a `homepage` URL for the item, and the `dataset_path`, i.e. the hierarchy of datasets the item is in. In case the result reprents a place, the record will include a list of variant `names` and, potentially, a list of `matches` - additional identifier URIs, under the same place is registered in other gazetteers.

You can retrieve more information about a record (such as all places related to an item, or information about sub-items) through __REST-style access__ (see below), using the record's `identifier` as a key.

The base URL for search is http://pelagios.org/peripleo/search, followed by any of these the filter parameters:

#### query

A keyword query. Per default, only exact matches are returned. Supports logical AND and OR operators, and trailing asterisk for prefix queries. (Note that logical operators need to be uppercase!) If you want to run a fuzzy search, which will also match similar - but not necessarily identical - terms, append a '~' to your query term. Examples:

[http://pelagios.org/peripleo/search?query=gold+AND+coin](http://pelagios.org/peripleo/search?query=gold+AND+coin&prettyprint=true)

[http://pelagios.org/peripleo/search?query=athen*](http://pelagios.org/peripleo/search?query=athen*&prettyprint=true)

[http://pelagios.org/peripleo/search?query=bvrdigala~](http://pelagios.org/peripleo/search?query=bvrdigala~&prettyprint=true)

#### types

Restrict the results to `place`, `dataset` or `item`. Allows multiple values, as comma-separated list. Examples:

[http://pelagios.org/peripleo/search?query=coin&types=place](http://pelagios.org/peripleo/search?query=coin&types=place&prettyprint=true)

#### datasets

Restrict results to one or more specific datasets. (Separate multiple dataset IDs by comma.) E.g. find everything for 'mausoleum' in the [Following Hadrian](http://pelagios.org/peripleo/pages/datasets/ca22250344a3b20d3a79f33c39e703a7f2d9899bd3e3cf6057cd80530f0944e2)
photo collection:

[http://pelagios.org/peripleo/search?query=mausoleum&datasets=ca222503...](http://pelagios.org/peripleo/search?query=mausoleum&datasets=ca22250344a3b20d3a79f33c39e703a7f2d9899bd3e3cf6057cd80530f0944e2&prettyprint=true)

#### places

Restrict to one or more places. Places are identified by a comma-separated list of gazetteer URIs. (URIs need to be
URL-escaped!). If more than one place is specified, they are logically combined to an AND query. That means the search
will return items related to __all__ of the places in the list. E.g. find everything that refers to both Rome AND Syria:

[http://pelagios.org/peripleo/search?places=http:%2F%2Fpleiades.stoa.org%2Fplaces%2F981550,htt...](http://pelagios.org/peripleo/search?places=http:%2F%2Fpleiades.stoa.org%2Fplaces%2F981550,http:%2F%2Fpleiades.stoa.org%2Fplaces%2F423025&prettyprint=true)

#### bbox

Restrict to a geographic bounding box. The bounding box must be specified as a comma-separated list
of decimal (WGS-84 datum) numbers, according to the format `bbox={minLon},{maxLon},{minLat},{maxLat}`. Example:

[http://pelagios.org/peripleo/search?bbox=23.716,23.7266,37.97,37.978](http://pelagios.org/peripleo/search?bbox=23.716,23.7266,37.97,37.978&prettyprint=true)

#### lat, lon, radius

Alternatively, you can restrict to a geographic area by specifying a center `lat`, `lon` coordinate for your
search, and a `radius` (in km). If you omit the radius, it will default to 10km. _Note: if you specify both a
`bbox` parameter and a coordinate, coordinate and radius will be ignored, and the bounding box will take precedence._

[http://pelagios.org/peripleo/search?query=athens&type=place&lat=37.97&lon=23.72&radius=3](http://pelagios.org/peripleo/search?query=athens&type=place&prettyprint=true&lat=37.97&lon=23.72&radius=3)

#### from, to

Restrict the results to a specific time interval. Both parameters take an integer number, which is interpreted as year. (Use negative
numbers for BC years.) If you are interested in one specific year only, use the same value for `from` and `to`.
Note: items in Pelagios that are __not dated will not appear in the results__. Examples:

[http://pelagios.org/peripleo/search?query=coin&from=-600&to=-500&types=item](http://pelagios.org/peripleo/search?query=coin&from=-600&to=-500&types=item&prettyprint=true)

[http://pelagios.org/peripleo/search?from=2014&to=2014](http://pelagios.org/peripleo/search?from=2014&to=2014&prettyprint=true)


## REST-Style Access

The API provides 'REST-style' access to entity metadata via the following URL paths:

* [/peripleo/datasets](http://pelagios.org/peripleo/datasets?prettyprint=true) - list all datasets
* [/peripleo/datasets/{id}](http://pelagios.org/peripleo/datasets/21b2d56d90bd192834aea9d8ad9d61b21a94d85f15f7cab1c458d4eebf599b73?prettyprint=true) - get the dataset with the specified ID
* [/peripleo/datasets/{id}/items](http://pelagios.org/peripleo/datasets/21b2d56d90bd192834aea9d8ad9d61b21a94d85f15f7cab1c458d4eebf599b73/items?prettyprint=true) - list all items contained in this dataset
* [/peripleo/datasets/{id}/places](http://pelagios.org/peripleo/datasets/21b2d56d90bd192834aea9d8ad9d61b21a94d85f15f7cab1c458d4eebf599b73/places?prettyprint=true) - list all places related to the items in this dataset *)
* [/peripleo/datasets/{id}/time](http://pelagios.org/peripleo/datasets/21b2d56d90bd192834aea9d8ad9d61b21a94d85f15f7cab1c458d4eebf599b73/time?prettyprint=true) - get the 'temporal profile' of the dataset **)
* [/peripleo/items/{id}](http://pelagios.org/peripleo/items/8ea5bcf2e508289842118a279f60197daf0f5b34b9293aab0593ac4b3d9d1a9f?prettyprint=true) - get the item with the specified ID
* [/peripleo/items/{id}/items](http://pelagios.org/peripleo/items/8ea5bcf2e508289842118a279f60197daf0f5b34b9293aab0593ac4b3d9d1a9f/items?prettyPrint=true) - list sub-items to this item
* [/peripleo/items/{id}/places](http://pelagios.org/peripleo/items/8ea5bcf2e508289842118a279f60197daf0f5b34b9293aab0593ac4b3d9d1a9f/places?prettyprint=true) - list all places related to this item
* [/peripleo/places/{uri}](http://pelagios.org/peripleo/places/http:%2F%2Fpleiades.stoa.org%2Fplaces%2F423025?prettyprint=true) - get information about the place with the (URL-escaped!) URI

*) Append `verbose=false` as query parameter to receive a less verbose response. This response will have additional performance benefits and load faster than
the full response. Usually, you will only need this if you retrieve many places in one request, by setting a high page size `limit`.

**) The temporal profile is an aggregation of the date information of all items contained in the dataset. It consists of a start and end year for the
dataset, and a 'histogram' that plots the number of items over the start-to-end time interval.

## License

The code for the Peripleo is licensed under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl.html).
