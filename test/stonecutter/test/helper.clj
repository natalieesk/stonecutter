(ns stonecutter.test.helper
  (:require [midje.sweet :refer :all]
            [ring.util.response :as response]
            [stonecutter.helper :as h]
            [stonecutter.helper :as helper]
            [net.cgrand.enlive-html :as html]))

(fact "disabling caching should add the correct headers"
      (let [r (-> (response/response "a-response") h/disable-caching)]
        (get-in r [:headers "Pragma"]) => "no-cache"
        (get-in r [:headers "Cache-Control"]) => "no-cache, no-store, must-revalidate"
        (get-in r [:headers "Expires"]) => "0"))

(defn get-link-href [enlive-m]
  (-> enlive-m (html/select [:link]) first :attrs :href))

(defn get-response-enlive-m [response]
  (-> response :body html/html-snippet))

(fact "Enlive response injects the app name anywhere where class is clj--app-name"
      (let [html "<html><body><h1 class=\"clj--app-name\"></h1><span class=\"clj--app-name\"></span></body></html>"
            enlive-m (html/html-snippet html)]
        (-> (helper/enlive-response enlive-m {:config-m {:app-name "My App"}}) get-response-enlive-m
            (html/select [:h1]) first html/text) => "My App"
        (-> (helper/enlive-response enlive-m {:config-m {:app-name "My App"}}) get-response-enlive-m
            (html/select [:span]) first html/text) => "My App"))

(fact "Enlive response prepends a slash to the hrefs of any stylesheets"
      (let [html "<link href=\"stylesheets/application.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\"/>"
            enlive-m (html/html-snippet html)]
        (-> (helper/enlive-response enlive-m {}) get-response-enlive-m
            (html/select [:link]) first :attrs :href) => "/stylesheets/application.css"))
