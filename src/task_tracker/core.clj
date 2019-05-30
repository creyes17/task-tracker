(ns task-tracker.core
  (:gen-class)
  (:require [task-tracker.hierarchy :as hierarchy]
            [task-tracker.persistence :as persistence]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [db-config (persistence/get-config
                    (persistence/get-secret-from-aws
                      (persistence/get-credentials-secret)))
        this-numerator (persistence/get-next-root db-config)
        next-root (hierarchy/create-root this-numerator)
        task {:hierarchy-node next-root
              :issue-link (str "Hello " this-numerator " World")
              :estimated-time-minutes 30}]
    (persistence/save-task db-config task (System/getenv "USER"))))
