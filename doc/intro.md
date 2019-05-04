# Hierarchy Nodes

Based on https://arxiv.org/pdf/0806.3115.pdf

You can create a new hierarchy with:

```clojure
(require '[task-tracker.core :as tracker :refer [create-root add-child]])

(let [root (tracker/create-root 3)]
      ; #task_tracker.core.HierarchyNode{:this-numerator 3,
      ;                                  :this-denominator 1,
      ;                                  :next-numerator 4,
      ;                                  :next-denominator 1,
      ;                                  :num-children 0}
  (tracker/add-child root))
; {:root #task_tracker.core.HierarchyNode{:this-numerator 3,
;                                         :this-denominator 1,
;                                         :next-numerator 4,
;                                         :next-denominator 1,
;                                         :num-children 1},
;  :child #task_tracker.core.HierarchyNode{:this-numerator 7,
;                                          :this-denominator 2,
;                                          :next-numerator 11,
;                                          :next-denominator 3,
;                                          :num-children 0}}
```
