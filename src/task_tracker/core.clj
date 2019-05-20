(ns task-tracker.core
  (:gen-class)
  (:require [task-tracker.persistence :as persistence]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (persistence/get-next-root)))
