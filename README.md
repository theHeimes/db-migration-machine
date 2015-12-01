# db-migration-machine

Helps to load data from one database to another with jdbc.

Iterates over a target schema and loads appropriate data from a source schema.

Relies on convention over configuration, i.e. there's no option to suppress tables.

Custom functions and all database specifics are ignored. 
The use-case I had in mind was proof-of-concepts for new databases.

You might download the jdbc-driver from Microsoft by yourself or try the jTDS driver.
In general jTDS works better, but for some use-cases the Microsoft driver has its 
advantages.

## Installation

Download from https://github.com/theHeimes/db-migration-machine

## Usage

Main class not implemented, you can run the function migration/migrate in lein.

    $ java -jar db-migration-machine-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0.
