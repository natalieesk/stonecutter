(defproject stonecutter "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :min-lein-version "2.0.0"
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [ring/ring-defaults "0.1.2"]
                           [ring/ring-jetty-adapter "1.4.0-RC1"]
                           [scenic "0.2.3"]
                           [enlive "1.1.5"]
                           [hiccup "1.0.5"]
                           [clauth "1.0.0-rc17"]
                           ;[traduki "0.1.0"]
                           [clj-yaml "0.4.0"]
                           [environ "1.0.0"]
                           ]
            :plugins [[lein-ring "0.8.13"]
                      [lein-midje "3.1.3"]]
            :ring {:handler stonecutter.handler/app}
            :main stonecutter.handler
            :profiles {:dev {:dependencies   [[ring-mock "0.1.5"]
                                              [midje "1.6.3"]]
                             :resource-paths ["resources" "test-resources"]}})
