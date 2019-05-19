(ns task-tracker.persistence
  (:require [cheshire.core :as json]
            [clojure.core :refer [select-keys]]
            [clojure.java.jdbc :as jdbc]
            [cognitect.aws.client.api :as aws]))

; TODO: Warn if environment variable not present
; TODO: Look into updating these values periodically and/or in response to a trigger
(def ^:private db-config (let [credentials-secret (System/getenv "C17_TASKTRACKER_POSTGRES_SECRET")
                               secrets-manager-client (aws/client {:api :secretsmanager})
                               secretResponse (aws/invoke secrets-manager-client
                                                          {:op :GetSecretValue
                                                           :request {:SecretId credentials-secret}})
                               secret (json/decode (get secretResponse :SecretString) true)]
                               (assoc (select-keys secret [:dbname :host :password :port])
                                                   :dbtype "postgres"
                                                   :user (get secret :username))))

(defn get-next-root
  "Queries for the next root numerator to insert"
  []
  (get (first (jdbc/query db-config "select
                                       max(hierarchy.next_sibling_numerator) as max
                                     from
                                       hierarchy
                                     where
                                       hierarchy.denominator = 1"))
       :max))
