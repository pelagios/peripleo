# Pelagios API (v3)

This is the latest incarnation of the Pelagios API, developed during the course of the 
[Pelagios 3 Project](http://pelagios-project.blogspot.co.uk). Version 3 of the Pelagios API
is work in progress (which translates as "not everything works just yet").

__A development version of the API (which includes a small sample dataset to play with) is 
available at [http://pelagios.org/api-v3/pages/datasets](http://pelagios.org/api-v3/pages/datasets).__

## Using the API

The primary entities served through the API are __Datasets__, __Items__ and __Places__. The API is read-only,
so HTTP GET requests are the only ones accepted. JSON is currently the only response format. Append __prettyprint=true__ 
as query parameter for a pretty-printed JSON response. All paginated responses will return a list
of 20 items per default. Append __offset=XY__ (list offset) and __limit=XY__ (number of items returned) as query
parameters to control pagination. The API supports CORS and JSONP ([example](http://pelagios.org/api-v3/datasets?callback=myHandler)).

The API provides the following methods:

* __[/api-v3/datasets](http://pelagios.org/api-v3/datasets?prettyprint=true)__ List all datasets (paginated).
* __[/api-v3/datasets/:id](http://pelagios.org/api-v3/datasets/174524047516a97f0ba45d4af5e485dd?prettyprint=true)__  Get the dataset with the specified ID.
* __[/api-v3/datasets/:id/items](http://pelagios.org/api-v3/datasets/174524047516a97f0ba45d4af5e485dd/items?prettyprint=true)__ List all items contained in this dataset (paginated).
* __[/api-v3/datasets/:id/places](http://pelagios.org/api-v3/datasets/174524047516a97f0ba45d4af5e485dd/places?prettyprint=true)__ List all places contained in this dataset (paginated). Append 'verbose=false' as query parameter to receive a less verbose response, which has additional performance benefits (i.e. it will load faster than the full response)
* __[/api-v3/items/:id](http://pelagios.org/api-v3/items/bae00692d9a64e3e947bd5819102d01d?prettyprint=true)__ Get the item with the specified ID
* __[/api-v3/items/:id/items](http://pelagios.org/api-v3/items/2dbf2df9a073bd29710abdf3a4330996/items?prettyPrint=true)__ List sub-items to this item (paginated)
* __[/api-v3/items/:id/places](http://pelagios.org/api-v3/items/bae00692d9a64e3e947bd5819102d01d/places?prettyprint=true)__ List all places that are referenced by this item (paginated)
* __[/api-v3/items/:id/annotations](http://pelagios.org/api-v3/items/bae00692d9a64e3e947bd5819102d01d/annotations?prettyprint=true)__ List the raw annotations on this item (paginated)
* __[/api-v3/annotations/:id](http://pelagios.org/api-v3/annotations/39dae3ef-b40e-4535-99df-f02c5e507659)__ Get the annotation with the specified ID
* __[/api-v3/places/:uri](http://pelagios.org/api-v3/places/http%3A%2F%2Fpleiades.stoa.org%2Fplaces%2F658381?prettyprint=true)__ Get the place with the specified URI (be sure to URL-escape the URI!)

## HTML Views

The API will also provide 'human-readable output' - i.e. HTML pages with the same information that
appears in the JSON responses. This work is just getting started. See

[http://pelagios.org/api-v3/pages/datasets/42362e18923860b43ed4c048f16274f7](http://pelagios.org/api-v3/pages/datasets/42362e18923860b43ed4c048f16274f7)

for an example.

## License

The Pelagios API is licensed under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl.html).
