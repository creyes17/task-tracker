(ns task-tracker.core
  (:gen-class))

; TODO: Move this to a separate library
(defrecord HierarchyNode [this-numerator
                          this-denominator
                          next-numerator
                          next-denominator
                          num-children])

(defn create-root
  "Creates a new root `task-tracker.core/HierarchyNode`.
  The resulting `task-tracker.core/HierarchyNode` will have `(= (/ :this-numerator :this-denominator) value)`"
  [value]
  (map->HierarchyNode {:this-numerator value
                       :this-denominator 1
                       :next-numerator (inc value)
                       :next-denominator 1
                       :num-children 0}))

(defn get-child-value
  "Calculates the value of the child object.
  For example, can calculate the numerator of the `n`th child based
  on the `parent-value` numerator and `parent-next-sibling-value` numerator."
  [n parent-value parent-next-sibling-value]
  ; See https://arxiv.org/pdf/0806.3115.pdf section 2.1
  (+ parent-value (* n parent-next-sibling-value)))

(defn add-child
  "Adds a child to `root`.
  Returns both the updated :root and the newly created :child."
  [root]
  (let [child-num (inc (:num-children root))]
    {:root (assoc root :num-children child-num)
     :child (map->HierarchyNode {:this-numerator (get-child-value child-num
                                                                  (:this-numerator root)
                                                                  (:next-numerator root))
                                 :this-denominator (get-child-value child-num
                                                                  (:this-denominator root)
                                                                  (:next-denominator root))
                                 :next-numerator (get-child-value (inc child-num)
                                                                  (:this-numerator root)
                                                                  (:next-numerator root))
                                 :next-denominator (get-child-value (inc child-num)
                                                                  (:this-denominator root)
                                                                  (:next-denominator root))
                                 :num-children 0})}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
