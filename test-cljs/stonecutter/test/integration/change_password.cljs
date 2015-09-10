(ns stonecutter.test.integration.change-password
  (:require [cemerick.cljs.test :as t]
            [stonecutter.renderer.change-password :as cp-render]
            [stonecutter.test.change-password :as cp-test]
            [dommy.core :as dommy]
            [stonecutter.change-password :as cp])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing run-tests]]
                   [dommy.core :refer [sel1 sel]]
                   [stonecutter.test.macros :refer [load-template]]))

(defn setup-page! [html]
  (dommy/set-html! (sel1 :html) html))

(def change-password-template (load-template "public/change-password.html"))

(def default-state {:current-password {:value "" :error nil} :new-password {:value "" :error nil :tick nil}})

; {:current-password {:value "" :error nil} :new-password {:value "" :error nil :tick nil}}
; {:current-password {:value "" :error :blank} :new-password {:value "" :error nil :tick nil}}
; {:current-password {:value "" :error :too-short} :new-password {:value "" :error nil :tick nil}}
; {:current-password {:value "" :error nil} :new-password {:value "" :error :blank :tick nil}}


(deftest render!
         (setup-page! change-password-template)

         (testing "current password rendering"
                  (testing "error blank, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:current-password :error] :blank)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/current-password-field cp-test/field-invalid-class)
                           (cp-test/has-message-on-selector cp-test/form-row-current-password-error-class (get-in cp/error-to-message [:current-password :blank])))

                  (testing "error too-short, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:current-password :error] :too-short)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/current-password-field cp-test/field-invalid-class)
                           (cp-test/has-message-on-selector cp-test/form-row-current-password-error-class (get-in cp/error-to-message [:current-password :too-short])))

                  (testing "no error, gives no validation classes and no error message"
                           (cp-render/render! default-state)
                           (cp-test/test-field-doesnt-have-class cp-test/current-password-field cp-test/field-invalid-class)
                           (cp-test/test-field-doesnt-have-class cp-test/current-password-field cp-test/field-valid-class)
                           (cp-test/has-no-message-on-selector cp-test/form-row-current-password-error-class)))

         (testing "new password rendering"
                  (testing "error blank, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :blank)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (cp-test/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :blank])))

                  (testing "error too-short, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :too-short)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (cp-test/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :too-short])))

                  (testing "error unchaged, gives it an invalid class and error message"
                           (let [state (assoc-in default-state [:new-password :error] :unchanged)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/new-password-field cp-test/field-invalid-class)
                           (cp-test/has-message-on-selector cp-test/form-row-new-password-error-class (get-in cp/error-to-message [:new-password :unchanged])))

                  (testing "no error, gives no validation classes and no error message"
                           (let [state (assoc-in default-state [:new-password :tick] true)]
                             (cp-render/render! state)
                             (cp-test/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-invalid-class)
                             (cp-test/has-no-message-on-selector cp-test/form-row-new-password-error-class)))

                  (testing "tick true gives it a valid class"
                           (let [state (assoc-in default-state [:new-password :tick] true)]
                             (cp-render/render! state))
                           (cp-test/test-field-has-class cp-test/new-password-field cp-test/field-valid-class)
                           (cp-test/test-field-doesnt-have-class cp-test/new-password-field cp-test/field-invalid-class))))