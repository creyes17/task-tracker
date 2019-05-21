(ns task-tracker.core
  (:gen-class)
  (:require [task-tracker.hierarchy :as hierarchy]
            [task-tracker.persistence :as persistence]
            [task-tracker.task :as task]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [this-numerator (persistence/get-next-root)
        next-root (hierarchy/create-root this-numerator)
        task (task/map->Task {:hierarchy-node next-root
                              :issue-link (str "Hello " this-numerator " World")
                              :estimated-time-minutes 30})]
    (persistence/create-task task)))
