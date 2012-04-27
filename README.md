# fds2app

Experimental implementation of a federated data server. Main idea is: Navigate linked data sources via a uniform interface.

This project contains:

* definition of data nodes, api for traversal/search and interlinking in fds2app.fds
* sample data sources in fds2app.data.events and fds2app.data.stammbaum
* sample generated data source in fds2app.data.generated
* a small webapplication that shows json representation of data nodes
* a small webapplication that decodes EUMONIS file names (incl. query language)

## Usage

To use the SWI-Prolog integration make sure to prepare your system:
- Define an environment variable SWI_HOME_DIR with your local SWI-Prolog path as value
- Add the bin/ path of SWI-Prolog to your PATH variable.

* Install [leiningen](https://github.com/technomancy/leiningen).
* Run
    `lein deps, run`
* Then open a webbrowser and navigate to either [EUMONIS files](http://localhost:8080/sharepoint), [FDS root node](http://localhost:8080/fds) or the [visualization](http://localhost:8080/fds/visualize?max-depth=3).

## License

Copyright (C) 2012 Steffen Dienst <sdienst@informatik.uni-leipzig.de>