;; Copyright (C) 2019  Christopher R. Reyes
;;
;; This file is part of Task Tracker.
;;
;; Task Tracker is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; Task Tracker is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with Task Tracker.  If not, see <https://www.gnu.org/licenses/>.

(ns
  ^{:author "Christopher R. Reyes"}
  dev.chrisreyes.task-tracker.persistence
  "These are functions for working with a postgres database.
  They provide CRUD operations for our model types in
  persistent storage."
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [map-invert rename-keys]]
            [cognitect.aws.client.api :as aws]))

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

(defn task->db-row
  "Returns a map matching the database schema for a task"
  [task]
  (filter-nil-values
    (assoc
      (rename-keys (select-keys task (keys (:task db-schema)))
                   (:task db-schema))
      :hierarchy_id (:hierarchy-id (:hierarchy-node task)))))

(defn hierarchy-node->db-row
  "Returns a map matching the database schema for a hierarchy node"
  [hierarchy-node]
  (filter-nil-values
    (rename-keys (select-keys hierarchy-node (keys (:hierarchy db-schema)))
                 (:hierarchy db-schema))))

(defn db-row->hierarchy-node
  "Given a database row representing a hierarchy node,
  return the canonical hierarchy node representation"
  [db-row]
  (rename-keys db-row
               (assoc (map-invert (:hierarchy db-schema))
                      :num_children :num-children)))

(defn db-row->task
  "Given a database row representing a task,
  return the canonical task representation"
  [db-row]
  (assoc (rename-keys db-row (map-invert (:task db-schema)))
         :hierarchy-node (db-row->hierarchy-node db-row)))

(defn get-credentials-secret
  "Gets the secret name to use to retrieve credentials from AWS Secrets Manager"
  []
  (System/getenv "C17_TASKTRACKER_POSTGRES_SECRET"))

(defn get-secret-from-aws
  "Given a secret name, returns that secret as a map from AWS Secrets Manager"
  [secret-name]
  (let [secrets-manager-client (aws/client {:api :secretsmanager})
        secretResponse (aws/invoke secrets-manager-client
                                   {:op :GetSecretValue
                                    :request {:SecretId secret-name}})]
       (json/decode (:SecretString secretResponse)
                    ; The "true" argument here converts string keys to :keys
                    true)))

(defn get-config
  "Gets the configuration from the JSON secret
  It should have the following keys:
    :dbname   - The name of the database within postgres
    :host     - The URL of the machine hosting the postgres instance
    :password - The password for the :username of the database
    :port     - The port number on the host machine to use to connect
    :username - The log-in name for a user of the database
  All other keys will be ignored"
  [secret]
  (assoc (select-keys secret [:dbname :host :password :port])
         :dbtype "postgres"
         :user (:username secret)))

(defn get-hierarchy-id-from-db
  "Given a hierarchy/Node, search the database for the ID of
  a saved hierarchy with the same numerator/denominator."
  [db-config hierarchy-node]
  (:hierarchy_id (first (jdbc/query db-config ["select
                                                  hierarchy.hierarchy_id
                                                from
                                                  hierarchy
                                                where
                                                  hierarchy.numerator = ?
                                                  and hierarchy.denominator = ?"
                                               (:numerator hierarchy-node)
                                               (:denominator hierarchy-node)]))))

(defn insert-hierarchy-node
  "Saves a hierarchy node to the database, returning the ID"
  [db-config hierarchy-node]
  (:hierarchy_id (first (jdbc/insert! db-config
                                      :hierarchy
                                      (hierarchy-node->db-row hierarchy-node)))))

(defn get-or-create-hierarchy-id
  "Gets the ID from a hierarchy-node. If the node has never been
  saved to the database, saves it first returning the new ID."
  [db-config hierarchy-node]
  (or (:hierarchy-id hierarchy-node)
      (get-hierarchy-id-from-db db-config hierarchy-node)
      (insert-hierarchy-node db-config hierarchy-node)))

(defn insert-task
  "Inserts the task to the database, returning the updated task"
  [db-config task username]
  (db-row->task
    (first (jdbc/insert! db-config
                         :task (assoc (task->db-row task)
                                      :created_by username
                                      :last_modified_by username)))))

(defn save-task
  "Saves a new task to the database. The task must have a hierarchy-node."
  [db-config task username]
  (jdbc/db-transaction*
    db-config
    (fn [transaction-config]
      (let [hierarchy-node (:hierarchy-node task)
            hierarchy-id (get-or-create-hierarchy-id transaction-config hierarchy-node)
            updated-hierarchy-node (assoc hierarchy-node :hierarchy-id hierarchy-id)
            updated-task (assoc task :hierarchy-node updated-hierarchy-node)]
         (assoc (insert-task transaction-config updated-task username)
                :hierarchy-node updated-hierarchy-node)))))

(defn get-next-root
  "Queries for the next root numerator to insert"
  [db-config]
  (:max (first (jdbc/query db-config "select
                                        coalesce(
                                          max(
                                            hierarchy.next_sibling_numerator),
                                          1) as max
                                      from
                                        hierarchy
                                      where
                                        hierarchy.denominator = 1"))))

(defn get-all-roots
  "Finds all of the root-level ::task objects from the database."
  [db-config]
  (map db-row->task
       (jdbc/query db-config "select
                                task.actual_time_minutes,
                                task.estimated_time_minutes,
                                task.issue_link,
                                task.task_id,
                                hierarchy.hierarchy_id,
                                hierarchy.denominator,
                                hierarchy.next_sibling_denominator,
                                hierarchy.next_sibling_numerator,
                                hierarchy.numerator,
                                coalesce(count(subhierarchy.hierarchy_id), 0) as num_children,
                                coalesce(sum(subtask.estimated_time_minutes), 0) as children_estimated_time_minutes,
                                coalesce(sum(subtask.actual_time_minutes), 0) as children_actual_time_minutes
                              from
                                hierarchy
                                join task
                                  on hierarchy.hierarchy_id = task.hierarchy_id
                                left outer join hierarchy subhierarchy
                                  on is_subtask(hierarchy, subhierarchy)
                                left outer join task subtask
                                  on subtask.hierarchy_id = subhierarchy.hierarchy_id
                              where
                                hierarchy.denominator = 1
                              group by
                                task.actual_time_minutes,
                                task.estimated_time_minutes,
                                task.issue_link,
                                task.task_id,
                                hierarchy.hierarchy_id,
                                hierarchy.denominator,
                                hierarchy.next_sibling_denominator,
                                hierarchy.next_sibling_numerator,
                                hierarchy.numerator")))

(defn load-task-by-id
  "Loads a specific ::task object by ID from the database"
  [db-config task-id]
  (map db-row->task
       (jdbc/query db-config ["select
                                 task.actual_time_minutes,
                                 task.estimated_time_minutes,
                                 task.issue_link,
                                 task.task_id,
                                 hierarchy.hierarchy_id,
                                 hierarchy.denominator,
                                 hierarchy.next_sibling_denominator,
                                 hierarchy.next_sibling_numerator,
                                 hierarchy.numerator,
                                 coalesce(count(subhierarchy.hierarchy_id), 0) as num_children,
                                 coalesce(sum(subtask.estimated_time_minutes), 0) as children_estimated_time_minutes,
                                 coalesce(sum(subtask.actual_time_minutes), 0) as children_actual_time_minutes
                               from
                                 hierarchy
                                 join task
                                   on hierarchy.hierarchy_id = task.hierarchy_id
                                 left outer join hierarchy subhierarchy
                                   on is_subtask(hierarchy, subhierarchy)
                                 left outer join task subtask
                                   on subtask.hierarchy_id = subhierarchy.hierarchy_id
                               where
                                 task.task_id = ?
                               group by
                                 task.actual_time_minutes,
                                 task.estimated_time_minutes,
                                 task.issue_link,
                                 task.task_id,
                                 hierarchy.hierarchy_id,
                                 hierarchy.denominator,
                                 hierarchy.next_sibling_denominator,
                                 hierarchy.next_sibling_numerator,
                                 hierarchy.numerator"
                              task-id])))

(defn remove-task
  "Deletes a particular task by task ID, returning the number of rows deleted."
  [db-config task-id]
  (jdbc/delete! db-config
                :task
                ["task_id = ?", task-id]))

(defn remove-subtasks
  "Deletes all subtasks of a particular task, returning the number of subtasks deleted."
  [db-config parent-task-id]
  (jdbc/execute! db-config
                 ["delete
                   from
                     task
                   where
                     task.task_id in (
                       select
                         subtask.task_id
                       from
                         task parent
                         join hierarchy
                           on parent.hierarchy_id = hierarchy.hierarchy_id
                         left outer join hierarchy subtask_hierarchy
                           on is_subtask(hierarchy, subtask_hierarchy)
                         join task subtask
                           on subtask.hierarchy_id = subtask_hierarchy.hierarchy_id
                       where
                         parent.task_id = ?)"
                  parent-task-id]))
