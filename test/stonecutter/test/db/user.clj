(ns stonecutter.test.db.user
  (:require [midje.sweet :refer :all]
            [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.db.client :as client]
            [stonecutter.db.storage :as s]
            [stonecutter.db.user :as user]))

(facts "about storage of users - user storage journey"
       ;; These facts do not mock the database
       (s/setup-in-memory-stores!)
       (fact "can store a user"
             (user/store-user! "email@server.com" "password")
             => (contains {:login "email@server.com"
                           :name nil
                           :url nil}))

       (fact "can authenticate a user"
             (user/authenticate-and-retrieve-user "email@server.com" "password")
             => (contains {:login "email@server.com"
                           :name nil
                           :url nil}))

       (fact "can retrieve a user"
             (user/retrieve-user "email@server.com")
             => (contains {:login "email@server.com"
                           :name nil
                           :url nil}))

       (fact "can add authorised client for user"
             (user/add-authorised-client-for-user! "email@server.com" "a-client-id")
             => (contains {:login "email@server.com"
                           :name nil
                           :url nil
                           :authorised-clients ["a-client-id"]}))

       (fact "can remove authorised client for user"
             (user/remove-authorised-client-for-user! "email@server.com" "a-client-id")
             => (contains {:login "email@server.com"
                           :name nil
                           :url nil
                           :authorised-clients []}))

       (fact "can change user's password"
             (user/change-password! "email@server.com" "new-password")
             (user/authenticate-and-retrieve-user "email@server.com" "password") => nil
             (user/authenticate-and-retrieve-user "email@server.com" "new-password")
             => (contains {:login "email@server.com" :name nil :url nil}))


       (fact "can delete a user"
             (user/delete-user! "email@server.com") => {}
             (user/authenticate-and-retrieve-user "email@server.com" "password") => nil))

(facts "about is-duplicate-user?"
       (fact "unique email in not a duplicate"
             (user/is-duplicate-user? "unique@email.com") => false
             (provided
               (user/retrieve-user "unique@email.com") => nil))

       (fact "duplicate email is a duplicate"
             (user/is-duplicate-user? "valid@email.com") => true
             (provided
               (user/retrieve-user "valid@email.com") => ...a-user...))

       (fact "the email is always lower-cased"
             (user/is-duplicate-user? "VALID@EMAIL.COM") => true
             (provided
               (user/retrieve-user "valid@email.com") => ...a-user...)))

(fact "about creating a user record"
      (let [id-gen (constantly "id")]
        (fact "a uuid is added"
              (user/create-user id-gen "email" "password") => {:login "email" :password "encrypted_password" :uid "id" :name nil :url nil}
              (provided (cl-user/bcrypt "password") => "encrypted_password"))
        (fact "email is lower-cased"
              (user/create-user id-gen "EMAIL" "password") => (contains {:login "email"}))))

(facts "about storing users"
       (fact "users are stored in the user-store"
             (user/store-user! "email@server.com" "password") => {...a-user-key... ...a-user-value...}
             (provided
               (user/create-user s/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user...) => {...a-user-key... ...a-user-value...}))

       (fact "password is removed before returning user"
             (-> (user/store-user! "email@server.com" "password")
                 :password) => nil
             (provided
               (user/create-user s/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user...) => {:password "hashedAndSaltedPassword"})))

(facts "about authenticating and retrieving users"
       (fact "with valid credentials"
             (user/authenticate-and-retrieve-user "email@server.com" "password") => {...a-user-key... ...a-user-value...}
             (provided
               (cl-user/authenticate-user "email@server.com" "password") => {...a-user-key... ...a-user-value...}))

       (fact "password is removed before returning user"
             (-> (user/authenticate-and-retrieve-user "email@server.com" "password")
                 :password) => nil
             (provided
               (cl-user/authenticate-user "email@server.com" "password") => {:password "hashedAndSaltedPassword"}))

       (fact "with invalid credentials returns nil"
             (user/authenticate-and-retrieve-user "invalid@credentials.com" "password") => nil
             (provided
               (cl-user/authenticate-user "invalid@credentials.com" "password") => nil)))

(fact "can retrieve user without authentication"
      (user/retrieve-user "email@server.com") => ...a-user...
      (provided
        (cl-user/fetch-user "email@server.com") => ...a-user...))

(fact "can retrieve user using auth-code"
      (let [auth-code-record (cl-auth-code/create-auth-code ...client... ...user... ...redirect-uri...)]
        (user/retrieve-user-with-auth-code "code") => ...user...
        (provided
          (cl-auth-code/fetch-auth-code "code") => auth-code-record)))

(facts "about adding client ids to users with add-client-id"
       (fact "returns a function which adds client-id to a user's authorised clients"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key "some-value"}]
               (add-client-id-function user) => {:some-key "some-value" :authorised-clients [client-id]}))

       (fact "does not add duplicates"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key "some-value"}]
               (-> user
                   add-client-id-function
                   add-client-id-function) => {:some-key "some-value" :authorised-clients [client-id]}))

       (fact "removes any duplicates"
             (let [client-id "client-id"
                   add-client-id-function (user/add-client-id client-id)
                   user {:some-key "some-value"
                         :authorised-clients [client-id client-id]}]
               (-> user
                   add-client-id-function
                   add-client-id-function) => {:some-key "some-value"
                                               :authorised-clients [client-id]})))

(facts "about removing client ids from users with remove-client-id"
       (fact "returns a function which removes client-id from a user's authorised clients"
             (let [client-id "client-id"
                   user {:some-key           "some-value"
                         :authorised-clients [client-id "another-client-id"]}
                   remove-client-id-function (user/remove-client-id client-id)]
               (remove-client-id-function user) => {:some-key "some-value"
                                                    :authorised-clients ["another-client-id"]})))

(facts "about is-authorised-client-for-user?"
       (fact "returns true if client-id is in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...email... ...client-id...) => true
             (provided
               (user/retrieve-user ...email...) => {:authorised-clients [...client-id...]}))

       (fact "returns false if client-id is in not in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...email... ...client-id...) => false
             (provided
               (user/retrieve-user ...email...) => {:authorised-clients [...a-different-client-id...]})))

(facts "about changing password"
       (fact "update-password returns a function that hashes and updates the user's password"
            (let [password "new-raw-password"
                  update-password-function (user/update-password password)
                  user {:password "current-hashed-password" :some-key "some-value"}]
              (update-password-function user) => {:password "new-hashed-password" :some-key "some-value"}
              (provided
                (cl-user/bcrypt "new-raw-password") => "new-hashed-password"))))
