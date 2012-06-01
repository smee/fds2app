# fds2app

Experimental implementation of a federated data server. Main idea is: Navigate linked data sources via a uniform interface.

This project contains:

* definition of data nodes, api for traversal/search and interlinking in fds2app.fds
* sample data sources in fds2app.data.events and fds2app.data.stammbaum (incl. interpretation of RDS-PP expressions)
* sample generated data source in fds2app.data.generated
* a small webapplication that shows json representation of data nodes
* a small webapplication that decodes EUMONIS file names (incl. query language)

Please see the complete code documentation at *docs/uberdoc.html*.
## Usage

* Install [leiningen](https://github.com/technomancy/leiningen).
* Run
    `lein deps, run` (default port is 8080)
* Or: Build standalone application with 
    * `lein deps, uberjar`
    * copy `target/fds2app-VERSION-standalone.jar` into the same folder as `sample-data`
    * run `PORT=12345 java -jar fds2app-VERSION-standalone.jar`
* Then open a webbrowser and navigate to either [FDS root node](http://localhost:8080/fds.html) or the [documentation](http://localhost:8080/).

## License

Copyright (C) 2012 Steffen Dienst <sdienst@informatik.uni-leipzig.de>