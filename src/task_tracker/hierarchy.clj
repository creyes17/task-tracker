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

(ns task-tracker.hierarchy
  (:gen-class))

(defn create-root
  "Creates a new root hierarchy node.
  The resulting hierarchy node. will have `(= (/ :this-numerator :this-denominator) value)`"
  [value]
  {:this-numerator value
   :this-denominator 1
   :next-numerator (inc value)
   :next-denominator 1
   :num-children 0})

(defn get-child-value
  "Calculates the value of the child object.
  For example, can calculate the numerator of the `n`th child based
  on the `parent-value` numerator and `parent-next-sibling-value` numerator."
  [n parent-value parent-next-sibling-value]
  ; See https://arxiv.org/pdf/0806.3115.pdf section 2.1
  (+ parent-value (* n parent-next-sibling-value)))

(defn add-child
  "Adds a child to `hierarchy-node`.
  Returns both the updated :root and the newly created :child."
  [hierarchy-node]
  (let [child-num (inc (get hierarchy-node :num-children 0))]
    {:hierarchy-node (assoc hierarchy-node :num-children child-num)
     :child {:this-numerator (get-child-value child-num
                                              (:this-numerator hierarchy-node)
                                              (:next-numerator hierarchy-node))
             :this-denominator (get-child-value child-num
                                                (:this-denominator hierarchy-node)
                                                (:next-denominator hierarchy-node))
             :next-numerator (get-child-value (inc child-num)
                                              (:this-numerator hierarchy-node)
                                              (:next-numerator hierarchy-node))
             :next-denominator (get-child-value (inc child-num)
                                                (:this-denominator hierarchy-node)
                                                (:next-denominator hierarchy-node))
             :num-children 0}}))
