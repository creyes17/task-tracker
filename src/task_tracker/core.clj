(ns task-tracker.core
  (:gen-class))

(defrecord Task [this-numerator
                 this-denominator
                 next-numerator
                 next-denominator
                 num-children])

(defn create-root
  "Creates a new root `task-tracker.core/Task`.
  The resulting `task-tracker.core/Task` will have `(= (/ :this-numerator :this-denominator) value)`"
  [value]
  (map->Task {:this-numerator value
              :this-denominator 1
              :next-numerator (inc value)
              :next-denominator 1
              :num-children 0}))

(defn add-child
  "Adds a child to `root`."
  [root]
  (println "TODO: Implement")
  {:root root :child nil})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
