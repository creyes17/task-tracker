(ns task-tracker.persistence
  (:require [cheshire.core :as json]
            [clojure.core :refer [select-keys]]
            [clojure.java.jdbc :as jdbc]
            [cognitect.aws.client.api :as aws]))

(defn debug-postgres-connection
  "Just runs a simple query to confirm we can connect to postgres"
  []
  ; Get credentials
  (let [credentials-secret (System/getenv "C17_TASKTRACKER_POSTGRES_SECRET")
        secrets-manager-client (aws/client {:api :secretsmanager})
        secretResponse (aws/invoke secrets-manager-client
                                   {:op :GetSecretValue
                                    :request {:SecretId credentials-secret}})
        secret (json/decode (get secretResponse :SecretString) true)
        database (assoc (select-keys secret [:dbname :host :password :port])
                        :dbtype "postgres"
                        :user (get secret :username))]
    (jdbc/query database "select * from hierarchy")))
