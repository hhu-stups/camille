# Camille

The Camille Editor for Rodin.

[![Build Status](https://gitlab.cs.uni-duesseldorf.de/general/stups/camille/badges/develop/pipeline.svg)](https://gitlab.cs.uni-duesseldorf.de/general/stups/camille)

## Installing

In Rodin, go to Help > Install New Software... and select the Camille update site.
The update site should be preconfigured in Rodin - if not, add it using the URL https://stups.hhu-hosting.de/rodin/camille/release/.

## Building Camille

Building the project requires Java 11 or later (tested with Java 17) and Maven 3.6.3 or later (tested with Maven 3.9).
(At runtime, the plugin is still compatible with Java 8.)

```sh
$ cd org.eventb.texteditor.parent
$ mvn clean verify 
```
  
This will produce a p2 repository (update site) in org.eventb.texteditor.repository/target/repository

We autmatically produce nightly builds that can be installed using the repository located at https://stups.hhu-hosting.de/rodin/camille/develop/.

## Contributing/Bugs

Pull requests are very welcome. Suggestions for new extensions and known bugs are tracked on [GitHub](https://github.com/hhu-stups/camille/issues).
