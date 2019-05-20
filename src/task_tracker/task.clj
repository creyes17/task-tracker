(ns task-tracker.task
  (:gen-class))

; TODO: Create clojure.spec
; https://clojure.org/about/spec
(defrecord Task [task-id
                 hierarchy-node
                 issue-link
                 estimated-time-minutes
                 actual-time-minutes])
