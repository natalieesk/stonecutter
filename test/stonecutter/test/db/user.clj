(ns stonecutter.test.db.user
  (:require [midje.sweet :refer :all]
            [clauth.user :as cl-user]
            [clauth.auth-code :as cl-auth-code]
            [stonecutter.db.mongo :as m]
            [stonecutter.util.uuid :as uuid]
            [stonecutter.config :as config]
            [stonecutter.db.user :as user]))

(def user-store (m/create-memory-store))

(facts "about storage of users - user storage journey"
       (fact "can store a user"
             (user/store-user! user-store "email@server.com" "password")
             => (just {:login      "email@server.com"
                       :name       nil
                       :url        nil
                       :confirmed? false
                       :uid        anything
                       :role       (:default config/roles)}))

       (fact "can authenticate a user"
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password")
             => (contains {:login "email@server.com"
                           :name  nil
                           :url   nil}))

       (fact "can retrieve a user"
             (user/retrieve-user user-store "email@server.com")
             => (contains {:login "email@server.com"
                           :name  nil
                           :url   nil}))

       (fact "can confirm user's account"
             (let [confirmed-user-record (->> (user/store-user! user-store "email@server.com" "password")
                                              (user/confirm-email! user-store))]
               confirmed-user-record =not=> (contains {:confirmation-id anything})
               confirmed-user-record => (contains {:confirmed? true})))

       (fact "can add authorised client for user"
             (user/add-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :name               nil
                           :url                nil
                           :authorised-clients ["a-client-id"]}))

       (fact "can remove authorised client for user"
             (user/remove-authorised-client-for-user! user-store "email@server.com" "a-client-id")
             => (contains {:login              "email@server.com"
                           :name               nil
                           :url                nil
                           :authorised-clients []}))

       (fact "can change user's password"
             (user/change-password! user-store "email@server.com" "new-password")
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password") => nil
             (user/authenticate-and-retrieve-user user-store "email@server.com" "new-password")
             => (contains {:login "email@server.com" :name nil :url nil}))


       (fact "can delete a user"
             (user/delete-user! user-store "email@server.com") => {}
             (user/authenticate-and-retrieve-user user-store "email@server.com" "password") => nil))

(facts "about is-duplicate-user?"
       (fact "unique email in not a duplicate"
             (user/is-duplicate-user? ...user-store... "unique@email.com") => false
             (provided
               (user/retrieve-user ...user-store... "unique@email.com") => nil))

       (fact "duplicate email is a duplicate"
             (user/is-duplicate-user? ...user-store... "valid@email.com") => true
             (provided
               (user/retrieve-user ...user-store... "valid@email.com") => ...a-user...))

       (fact "the email is always lower-cased"
             (user/is-duplicate-user? ...user-store... "VALID@EMAIL.COM") => true
             (provided
               (user/retrieve-user ...user-store... "valid@email.com") => ...a-user...)))

(fact "about creating a user record"
      (let [id-gen (constantly "id")]
        (fact "a uuid is added"
              (user/create-user id-gen "email" "password") => {:login "email" :password "encrypted_password" :uid "id" :name nil :url nil :confirmed? false :role (:default config/roles)}
              (provided (cl-user/bcrypt "password") => "encrypted_password"))
        (fact "email is lower-cased"
              (user/create-user id-gen "EMAIL" "password") => (contains {:login "email"}))))

(facts "about storing users"
       (fact "users are stored in the user-store"
             (user/store-user! ...user-store... "email@server.com" "password") => {...a-user-key... ...a-user-value...}
             (provided
               (user/create-user uuid/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user-store... ...user...) => {...a-user-key... ...a-user-value...}))

       (fact "password is removed before returning user"
             (-> (user/store-user! ...user-store... "email@server.com" "password")
                 :password) => nil
             (provided
               (user/create-user uuid/uuid "email@server.com" "password") => ...user...
               (cl-user/store-user ...user-store... ...user...) => {:password "hashedAndSaltedPassword"})))

(facts "about authenticating and retrieving users"
       (fact "with valid credentials"
             (user/authenticate-and-retrieve-user ...user-store... "email@server.com" "password") => {...a-user-key... ...a-user-value...}
             (provided
               (cl-user/authenticate-user ...user-store... "email@server.com" "password") => {...a-user-key... ...a-user-value...}))

       (fact "password is removed before returning user"
             (-> (user/authenticate-and-retrieve-user ...user-store... "email@server.com" "password")
                 :password) => nil
             (provided
               (cl-user/authenticate-user ...user-store... "email@server.com" "password") => {:password "hashedAndSaltedPassword"}))

       (fact "with invalid credentials returns nil"
             (user/authenticate-and-retrieve-user ...user-store... "invalid@credentials.com" "password") => nil
             (provided
               (cl-user/authenticate-user ...user-store... "invalid@credentials.com" "password") => nil)))

(fact "can retrieve user without authentication"
      (user/retrieve-user ...user-store... "email@server.com") => ...a-user...
      (provided
        (cl-user/fetch-user ...user-store... "email@server.com") => ...a-user...))

(fact "can retrieve user using auth-code"
      (let [auth-code-store (m/create-memory-store)
            auth-code-record (cl-auth-code/create-auth-code auth-code-store ...client... ...user... ...redirect-uri...)]
        (user/retrieve-user-with-auth-code auth-code-store "code") => ...user...
        (provided
          (cl-auth-code/fetch-auth-code auth-code-store "code") => auth-code-record)))

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
                   user {:some-key           "some-value"
                         :authorised-clients [client-id client-id]}]
               (-> user
                   add-client-id-function
                   add-client-id-function) => {:some-key           "some-value"
                                               :authorised-clients [client-id]})))

(facts "about removing client ids from users with remove-client-id"
       (fact "returns a function which removes client-id from a user's authorised clients"
             (let [client-id "client-id"
                   user {:some-key           "some-value"
                         :authorised-clients [client-id "another-client-id"]}
                   remove-client-id-function (user/remove-client-id client-id)]
               (remove-client-id-function user) => {:some-key           "some-value"
                                                    :authorised-clients ["another-client-id"]})))

(facts "about is-authorised-client-for-user?"
       (fact "returns true if client-id is in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => true
             (provided
               (user/retrieve-user ...user-store... ...email...) => {:authorised-clients [...client-id...]}))

       (fact "returns false if client-id is in not in the users authorised-clients list"
             (user/is-authorised-client-for-user? ...user-store... ...email... ...client-id...) => false
             (provided
               (user/retrieve-user ...user-store... ...email...) => {:authorised-clients [...a-different-client-id...]})))

(facts "about changing password"
       (fact "update-password returns a function that hashes and updates the user's password"
             (let [password "new-raw-password"
                   update-password-function (user/update-password password)
                   user {:password "current-hashed-password" :some-key "some-value"}]
               (update-password-function user) => {:password "new-hashed-password" :some-key "some-value"}
               (provided
                 (cl-user/bcrypt "new-raw-password") => "new-hashed-password"))))

(facts "about admins"
       (fact "creating an admin user includes the role admin"
             (let [email "email@admin.com"
                   password "stubpassword"
                   hashed-password "ABE1234SJD1234"
                   id "random-uuid-1234"
                   id-gen (constantly id)]

               (user/create-admin id-gen email password) => {:login      email
                                                             :password   hashed-password
                                                             :confirmed? false
                                                             :uid id
                                                             :role (:admin config/roles)}
               (provided
                 (cl-user/new-user email password) => {:login email :password hashed-password})))

       (let [admin-login "admin@email.com"
             password "password456"
             hashed-password "PA134SN"]
         (fact "storing an admin calls create admin user"
               (against-background
                 (user/create-admin anything admin-login password) => {:login admin-login :password hashed-password :role (:admin config/roles)})

               (user/store-admin! user-store admin-login password) 
               (user/retrieve-user user-store admin-login ) => {:login admin-login 
                                                                :password hashed-password
                                                                :role (:admin config/roles)})))
