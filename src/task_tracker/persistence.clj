(ns task-tracker.persistence
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [map-invert rename-keys]]
            [cognitect.aws.client.api :as aws]
            [task-tracker.hierarchy]
            [task-tracker.task])
  (:import task_tracker.task.Task
           task_tracker.hierarchy.Node))


(defn- filter-nil-values
  "Returns a map with only the key/value pairs of the original map where (some? value)"
  [original]
  (into {} (filter #(some? (second %)) original)))

(def db-schema
  "The mapping from record properties to database column names"
  {:hierarchy {:hierarchy-id :hierarchy_id
               :this-numerator :numerator
               :this-denominator :denominator
               :next-numerator :next_sibling_numerator
               :next-denominator :next_sibling_denominator}
   :task {:task-id :task_id
          :issue-link :issue_link
          :estimated-time-minutes :estimated_time_minutes
          :actual-time-minutes :actual_time_minutes}})

(defprotocol DataTransferObject
  "A data structure that can prepare itself for insertion into persistent storage"
  (to-db-row [this] "Returns a map matching the database schema for this type"))

(extend-protocol DataTransferObject
  task_tracker.task.Task
  (to-db-row
    [this]
    (filter-nil-values
      (assoc (rename-keys (select-keys this (keys (:task db-schema)))
                          (:task db-schema))
             :hierarchy_id (:hierarchy-id (:hierarchy-node this)))))
  task_tracker.hierarchy.Node
  (to-db-row
    [this]
    (filter-nil-values (rename-keys (select-keys this (keys (:hierarchy db-schema)))
                                    (:hierarchy db-schema)))))

(defmulti from-db-row
  "Converts a database row into the appropriate DataTransferObject"
  (fn [db-row] (if (some? (:task_id db-row)) :task
                 (if (some? (:hierarchy_id db-row)) :hierarchy))))

(defmethod from-db-row :task
  [db-row]
  (task-tracker.task/map->Task (assoc
                    (rename-keys db-row (map-invert (:task db-schema)))
                    :hierarchy-node (from-db-row (dissoc db-row :task_id)))))

(defmethod from-db-row :hierarchy
  [db-row]
  (task-tracker.hierarchy/map->Node (rename-keys db-row (map-invert (:hierarchy db-schema)))))

; TODO: Warn if environment variable not present
; TODO: Look into updating these values periodically and/or in response to a trigger
;       Actually, just make this a function and let the calling context choose when to update cache
(def ^:private db-config (let [credentials-secret (System/getenv "C17_TASKTRACKER_POSTGRES_SECRET")
                               secrets-manager-client (aws/client {:api :secretsmanager})
                               secretResponse (aws/invoke secrets-manager-client
                                                          {:op :GetSecretValue
                                                           :request {:SecretId credentials-secret}})
                               secret (json/decode (:SecretString secretResponse) true)]
                               (assoc (select-keys secret [:dbname :host :password :port])
                                                   :dbtype "postgres"
                                                   :user (:username secret))))

(defn create-task
  "Inserts a new task into the database"
  [new-task]
  (jdbc/db-transaction*
    db-config
    #(let
       [hierarchy-node (:hierarchy-node new-task)
        hierarchy-id (or (:hierarchy-id hierarchy-node)
                         (:hierarchy_id
                           (first (jdbc/query % ["select
                                                     hierarchy.hierarchy_id
                                                  from
                                                     hierarchy
                                                  where
                                                     hierarchy.numerator = ?
                                                     and hierarchy.denominator = ?"
                                                  (:numerator hierarchy-node)
                                                  (:denominator hierarchy-node)])))
                         (:hierarchy_id (jdbc/insert! % (to-db-row hierarchy-node))))]
       {:hierarchy (assoc hierarchy-node :hierarchy-id hierarchy-id)
        :task (from-db-row (jdbc/insert! % (to-db-row new-task)))})))

(defn get-next-root
  "Queries for the next root numerator to insert"
  []
  (:max (first (jdbc/query db-config "select
                                       max(hierarchy.next_sibling_numerator) as max
                                     from
                                       hierarchy
                                     where
                                       hierarchy.denominator = 1"))))
