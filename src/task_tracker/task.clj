(ns task-tracker.task
  (:gen-class))

; TODO: get rid of these record types in favor of plain maps
(defrecord Task [task-id
                 hierarchy-node
                 issue-link
                 estimated-time-minutes
                 actual-time-minutes])

; TODO: Create clojure.spec
; https://clojure.org/about/spec
(defn create-task
  "Creates a new Task."
  ([issue-link hierarchy-node]
   (create-task issue-link hierarchy-node nil))
  ([issue-link hierarchy-node estimated-time]
   (create-task issue-link hierarchy-node estimated-time nil))
  ([issue-link hierarchy-node estimated-time actual-time]
   (map->Task {:actual-time-minutes actual-time
               :estimated-time-minutes estimated-time
               :hierarchy-node hierarchy-node
               :issue-link issue-link})))
