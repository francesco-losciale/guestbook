# guestbook

# TODO

- revisit chapter 5



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


Compile cljs

    lein cljsbuild once

Or 

    lein cljsbuild auto


To run shadow-cljs

    npx shadow-cljs watch app

Test using the REPL from the terminal

    npx shadow-cljs cljs-repl app
    cljs.user=> (js/alert "Hello from shadow-cljs")

Or in Intellij (by default it opens the clj interpreter, you need to switch to cljs)

    (shadow.cljs.devtools.api/repl :app)
    cljs.user=> (js/alert "Hello from shadow-cljs")
    :cljs/quit


## License

Copyright © 2021 FIXME
