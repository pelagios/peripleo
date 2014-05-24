# Pelagios API (v3)

This is the latest incarnation of the Pelagios API, developed during the course of the 
[Pelagios 3 Project](http://pelagios-project.blogspot.co.uk). V3 of the Pelagios API
is work in progress.

## For Developers
 
The primary entities served through the API are __Datasets__, __Items__ and __Places__. The API is read-only,
so HTTP GET requests are the only ones accepted. JSON is currently the only response format. Append __prettyPrint=true__ 
(case sensitive) as query parameter for a pretty-printed JSON response. All paginated responses will return a list
of 20 items per default. Append __offset=XY__ (list offset) and __limit=XY__ (number of items returned) as query
parameters to control pagination. 

API provides the following methods:

    GET     /api-v3/datasets

List all datasets (paginated)

    GET     /api-v3/datasets/:id
    
Get the dataset with the specified ID


    GET     /api-v3/datasets/:id/items
    
List all items contained in this dataset (paginated)

    GET     /api-v3/datasets/:id/places
    
List all places contained in this dataset (paginated)

    GET     /api-v3/items
    
List all items in the system (paginated)

    GET     /api-v3/items/:id
    
Get the item with the specified ID

    GET     /api-v3/items/:id/places
    
List all places that are referenced by this item (paginated)

    GET     /api-v3/items/:id/annotations
    
List the raw annotations on this item (paginated)

    GET     /api-v3/annotations 
    
List all raw annotations in the system (paginated)

    GET     /api-v3/annotations/:id

Get the annotation with the specified ID

    GET     /api-v3/places
    
List all places in the system (paginated)

    GET     /api-v3/places/:uri
    
Get the place with the specified URI (be sure to URL-escape the URI!)
