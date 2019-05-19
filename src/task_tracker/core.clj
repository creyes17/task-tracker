(ns task-tracker.core
  (:require [task-tracker.persistence :as persistence])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (persistence/get-next-root)))
