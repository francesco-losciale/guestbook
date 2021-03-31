# guestbook

generated using Luminus version "3.57"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run 

Or package jar and run: 
    
    lein uberjar &&
    java -jar target/uberjar/guestbook.jar 

This workaround was needed to avoid an error: https://github.com/luminus-framework/luminus/issues/229


In the repl

    lein repl

## License

Copyright © 2021 FIXME
