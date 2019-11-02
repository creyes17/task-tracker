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
  dev.chrisreyes.task-tracker.core
  "Here we coordinate between the different layers of the app.
  Eventually, this namespace will be responsible for allowing
  end-users to interact with the system by creating, reading,
  updating, and deleting tasks."
  (:gen-class)
  (:require [clojure.data.json :as json]
            [compojure.core]
            [dev.chrisreyes.task-tracker.hierarchy :as hierarchy]
            [dev.chrisreyes.task-tracker.persistence :as persistence]
            [org.httpkit.server :refer [run-server]]))


(defn- db-config
  "Gets the default db-config"
  []
  (persistence/get-config
    (persistence/get-secret-from-aws
      (persistence/get-credentials-secret))))

(defn- add-subtasks
  "Adds n children tasks to a parent ::task"
  ([root-task]
   (add-subtasks root-task 3))
  ([root-task n]
   (reduce (fn [root i] (let [child-hierarchy-data (hierarchy/add-child
                                                    (:hierarchy-node root))
                              child-hierarchy (:child child-hierarchy-data)
                              updated-root-hierarchy (:hierarchy-node child-hierarchy-data)
                              child-task {:hierarchy-node child-hierarchy
                                          :issue-link (str (inc i)
                                                           "th Child of "
                                                           n
                                                           " for task "
                                                           (:task-id root))
                                          :estimated-time-minutes (* 10 (inc i))}]
                          (persistence/save-task (db-config) child-task (System/getenv "USER"))
                          (assoc root :hierarchy-node updated-root-hierarchy)))
           root-task
           (range n))))

(defn- add-nested-subtasks
  "Adds n successive subtasks to a ::task such that each subtask is a parent of the next subtask added"
  ([root-task]
   (add-nested-subtasks root-task 3))
  ([root-task n]
   (reduce (fn [root i] (let [child-hierarchy-data (hierarchy/add-child
                                                    (:hierarchy-node root))
                              child-hierarchy (:child child-hierarchy-data)
                              child-task {:hierarchy-node child-hierarchy
                                          :issue-link (str (inc i)
                                                           "th Recursive Child of "
                                                           n
                                                           " for task "
                                                           (:task-id root-task))
                                          :estimated-time-minutes (* 10 (inc i))}]
                          (persistence/save-task (db-config) child-task (System/getenv "USER"))))
           root-task
           (range n))))

(defn- create-next-root-task
  "Creates and returns a new root-level ::task"
  []
  (let [config (db-config)
        this-numerator (persistence/get-next-root config)
        next-root (hierarchy/create-root this-numerator)
        task {:hierarchy-node next-root
              :issue-link (str "Hello " this-numerator " World")
              :estimated-time-minutes 30}]
    (persistence/save-task config task (System/getenv "USER"))))

(defn- create-task-with-subtasks
  "Creates a single ::task with n children"
  ([]
   (create-task-with-subtasks 3))
  ([n]
   (let [root-task (create-next-root-task)]
     (add-subtasks root-task n))))

(compojure.core/defroutes backend-api
  (compojure.core/GET "/.internal/is_healthy" [] "true\n"))

(defn -main
  "Starts a backend webserver on port 5000 to handle API requests for working with tasks"
  [& args]
  ; Note: Chose port 5000 arbitrarily based on a tutorial I was
  ;       following. It shouldn't really matter assuming we host this in
  ;       docker-compose because we can remap the port to anything.
  (run-server backend-api {:port 5000}))
