# Pelagios API 2.0

This is the latest incarnation of the Pelagios API, developed as part of the 
[Pelagios 3 Project](http://pelagios-project.blogspot.co.uk). The new Pelagios API
is work in progress (which translates as "not everything works just yet").

__Our development instance is online at [http://pelagios.org/api-v3](http://pelagios.org/api-v3) - feel
free to give it a spin.__

## The Basics

Pelagios is a __community network__ with the goal to facilitate __better linking between online
resources documenting the past__, based on the __places they refer to__. From [large numismatic dataset](http://pelagios.org/api-v3/pages/datasets/167d94d26e683d01a9fb3d7450b387ed908f9c70e7b17cf62aae27079184b092) to [personal Flickr photostream](http://pelagios.org/api-v3/pages/datasets/952eb415d77623bff61eccca6c88c7b8ddd9a6967f660775ea72a2ae81f23f56) - each one of our members contributes one piece of the puzzle which Pelagios hopes to combine into a coherent whole.

The purpose of the __Pelagios API__ is to make our network __browse-__ and __searchable__. The 'mental model' behind
our API is simple, and consists of only three types of entities:

* __Items__, such as archaeological artefacts, literary texts or photographs.
* __Places__ to which the items are related, e.g. places mentioned in texts or findspots of artefacts.
* __Datasets__ which contain the items, e.g. a particular museum collection or data corpus published by an institution.

Datasets as well as items can be __hierarchical__. E.g. the [Pelagios 3 dataset](http://pelagios.org/api-v3/pages/datasets/2a10228dff4c608b91a953efff8dafb3f5c433035b3f31e687eec0297d799824)
is sub-divided into a corpus of [Greek](http://pelagios.org/api-v3/pages/datasets/48ea51486cb33aae9e08501825a67fa0ba5770c5732742039e13a91ee75d5620)
and [Latin literary texts](http://pelagios.org/api-v3/pages/datasets/49d46d26fbde0f17cd09f16ff5561d930fd02775160c7ad1cba652ebbf3b2db8).
Likewise, an item such as Herodotus' _The Histories_ can be subdivided into individual _Books_.

## Response Format

The API returns responses in JSON format. [CORS](http://de.wikipedia.org/wiki/Cross-Origin_Resource_Sharing) and
[JSONP](http://en.wikipedia.org/wiki/JSONP) are both supported. Most responses are __paginated__, i.e. you will get back only one "page" of search results:

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
[http://pelagios.org/api-v3/search?query=gold+AND+coin](http://pelagios.org/api-v3/search?query=gold+AND+coin&prettyprint=true)

Results 21 - 40:
[http://pelagios.org/api-v3/search?query=gold+AND+coin&offset=20](http://pelagios.org/api-v3/search?query=gold+AND+coin&offset=20&prettyprint=true)

## Pretty Printing

Per default, the API will return a compact, unformatted JSON response. You can force a more human-readable response by
appending a `prettyprint=true` parameter. Example:

[http://pelagios.org/api-v3/search?query=bronze&prettyprint=true](http://pelagios.org/api-v3/search?query=bronze&prettyprint=true)

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
    "minLon" : 23.7195,
    "maxLon" : 44.0,
    "minLat" : 37.5197,
    "maxLat" : 45.5
  }
}
```

The `identifier`, `title` and `object_type` labels are always present. Depending on the object, the record can also 
include a short textual `description`, the bounds of the object in space and time (`geo_bounds` and `temporal_bounds`,
respectively), and lists of URLs to `images` and `thumbnails`. You can retrieve more information about an 
object (such as all places related to it, or information about sub-items) via the __REST-style methods__ (see below), 
using the object's `identifier` as a key.

The base URL for search is http://pelagios.org/api-v3/search, followed by any of these 
the filter parameters:

#### query 

A keyword query. Returns exact matches only (i.e. no fuzzy search). Supports AND and OR operators, and trailing
asterisk for prefix queries. Examples:

[http://pelagios.org/api-v3/search?query=gold+AND+coin](http://pelagios.org/api-v3/search?query=gold+AND+coin&prettyprint=true)

[http://pelagios.org/api-v3/search?query=athen*](http://pelagios.org/api-v3/search?query=athen*&prettyprint=true)

#### type

Restrict the results to `place`, `dataset` or `item`. Examples:

[http://pelagios.org/api-v3/search?query=bronze&type=place](http://pelagios.org/api-v3/search?query=bronze&type=place&prettyprint=true)

#### dataset

Restrict results to one specific dataset. E.g. find everything for 'netherlands' in the [Following Hadrian](http://pelagios.org/api-v3/pages/datasets/ca22250344a3b20d3a79f33c39e703a7f2d9899bd3e3cf6057cd80530f0944e2)
photo collection:

[http://pelagios.org/api-v3/search?query=netherlands&dataset=ca222503...](http://pelagios.org/api-v3/search?query=netherlands&dataset=ca22250344a3b20d3a79f33c39e703a7f2d9899bd3e3cf6057cd80530f0944e2&prettyprint=true)

#### places

Restrict to one or more places. Places are identified by a comma-separated list of gazetteer URIs. (URIs need to be
URL-escaped!). If more than one place is specified, they are logically combined to an AND query. That means the search
will return items related to __all__ of the places in the list. E.g. find everything that refers to both Rome AND Syria:

[http://pelagios.org/api-v3/search?places=http:%2F%2Fpleiades.stoa.org%2Fplaces%2F981550,htt...](http://pelagios.org/api-v3/search?places=http:%2F%2Fpleiades.stoa.org%2Fplaces%2F981550,http:%2F%2Fpleiades.stoa.org%2Fplaces%2F423025&prettyprint=true)

#### bbox

Restrict to a geographic bounding box. The bounding box must be specified as a comma-separated list
of decimal (WGS-84 coordinate) numbers, according to the format `bbox={minLon},{maxLon},{minLat},{maxLat}`. Example:

[http://pelagios.org/api-v3/search?bbox=23.716,23.7266,37.97,37.978&from=100&to=200](http://pelagios.org/api-v3/search?bbox=23.716,23.7266,37.97,37.978&from=100&to=200&prettyprint=true)

#### lat, lon, radius

Alternatively, you can restrict to a geographic area by specifying a center `lat`, `lon` coordinate for your 
search, and a `radius` (in km). If you omit the radius, it will default to 10km. _Note: if you specify both a
`bbox` parameter and a coordinate, coordinate and radius will be ignored, and the bounding box will take precedence._

[http://pelagios.org/api-v3/search?query=athens&type=place&lat=37.97&lon=23.72&radius=3](http://pelagios.org/api-v3/search?query=athens&type=place&prettyprint=true&lat=37.97&lon=23.72&radius=3)

#### from, to

Restrict the results to a specific time interval. Both parameters take an integer number, which is interpreted as year. (Use negative
numbers for BC years.) If you are interested in one specific year only, use the same value for `from` and `to`. 
Note: items in Pelagios that are __not dated will not appear in the results__. Examples:

[http://pelagios.org/api-v3/search?query=coin&from=-600&to=-500](http://pelagios.org/api-v3/search?query=coin&from=-600&to=-500&prettyprint=true)

[http://pelagios.org/api-v3/search?from=2014&to=2014](http://pelagios.org/api-v3/search?from=2014&to=2014&prettyprint=true)


## REST-Style Access Methods

The API also provides 'REST-style' access to the data via the following methods:

* [/api-v3/datasets](http://pelagios.org/api-v3/datasets?prettyprint=true) - list all datasets
* [/api-v3/datasets/:id](http://pelagios.org/api-v3/datasets/867fa38bcdbeb4aad94f4362d56329066b0c5914a58a011f6f223003eb4cf947?prettyprint=true) - get the dataset with the specified ID
* [/api-v3/datasets/:id/items](http://pelagios.org/api-v3/datasets/867fa38bcdbeb4aad94f4362d56329066b0c5914a58a011f6f223003eb4cf947/items?prettyprint=true) - list all items contained in this dataset
* [/api-v3/datasets/:id/places](http://pelagios.org/api-v3/datasets/867fa38bcdbeb4aad94f4362d56329066b0c5914a58a011f6f223003eb4cf947/places?prettyprint=true) - list all places referenced in this dataset *)
* [/api-v3/datasets/:id/time](http://pelagios.org/api-v3/datasets/867fa38bcdbeb4aad94f4362d56329066b0c5914a58a011f6f223003eb4cf947/time?prettyprint=true) - get the 'temporal profile' of a dataset **)
* [/api-v3/items/:id](http://pelagios.org/api-v3/items/1e664de13efffa06f4448046fcc246bf91c79e42766da820d17451f7ffb7f3aa?prettyprint=true) - get metdata for the item with the specified ID
* [/api-v3/items/:id/items](http://pelagios.org/api-v3/items/1e664de13efffa06f4448046fcc246bf91c79e42766da820d17451f7ffb7f3aa/items?prettyPrint=true) - list sub-items to this item
* [/api-v3/items/:id/places](http://pelagios.org/api-v3/items/1e664de13efffa06f4448046fcc246bf91c79e42766da820d17451f7ffb7f3aa/places?prettyprint=true) - list all places that are referenced by this item
* [/api-v3/places/:uri](http://pelagios.org/api-v3/places/http:%2F%2Fpleiades.stoa.org%2Fplaces%2F423025?prettyprint=true) - get information about the place with the (URL-escaped!) URI

*) Append `verbose=false` as query parameter to receive a less verbose response. This response will have additional performance benefits and load faster than
the full response. Usually, you will only need this if you retrieve many places in one request, by setting a high page size `limit`.

**) The temporal profile is an aggregation of the date information of all items contained in the dataset. It consists of a start and end year for the
dataset, and a 'histogram' that plots the number of items over the start-to-end time interval. 

## License

The code for the Pelagios API is licensed under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl.html).
