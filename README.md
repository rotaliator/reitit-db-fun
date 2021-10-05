# reitit-db-fun
Reitit with multiple databases

## running server

`clojure -M:run`

## uberjar compilation

`clojure -X:uberjar`

## Frontend

installing shadow-cljs

`npm install -g shadow-cljs`

### install dependencies

`npm install`

### watch dev build

`clojure -M:cljs watch app`

if shadow-cljs dependency is set in `deps.edn`
(and cljs dependencies like rum or reagent)

or

`shadow-cljs watch app`

if shadow-cljs is installed using npm and cljs dependencies ale set in `shadow-cljs.edn`

### build frontend for production

`clojure -M:cljs release app`

or

`shadow-cljs release app`

or

`clojure -T:build:cljs`


### build frontend and uberjar

`clojure -T:build all`
