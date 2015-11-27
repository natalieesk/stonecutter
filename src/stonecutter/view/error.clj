(ns stonecutter.view.error
  (:require [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.routes :as r]))

(defn modify-error-translation-keys [enlive-map error-page-key]
  (html/at enlive-map
           [:body] (html/set-attr :class (str "func--" error-page-key "-page"))
           [:title] (html/set-attr :data-l8n (str "content:" error-page-key "/title"))
           [:.clj--error-page-header] (html/set-attr :data-l8n (str "content:" error-page-key "/page-header"))
           [:.clj--error-page-intro] (html/set-attr :data-l8n (str "content:" error-page-key "/page-intro"))))

(defn set-return-to-link [enlive-m path]
  (html/at enlive-m [:.clj--error-return-home__link] (html/set-attr :href path)))

(defn internal-server-error [request]
  (-> (vh/load-template-with-lang "public/error-500.html" request)
      (set-return-to-link (r/path :index))))

(defn not-found-error [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-404")))

(defn csrf-error [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-csrf")))

(defn forbidden-error [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-forbidden")))

(defn account-nonexistent [request]
  (-> (internal-server-error request) (modify-error-translation-keys "error-account-nonexistent")))
