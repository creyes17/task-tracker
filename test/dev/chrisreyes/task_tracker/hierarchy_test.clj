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

(ns
  ^{:author "Christopher R. Reyes"}
  dev.chrisreyes.task-tracker.hierarchy-test
  (:require [clojure.test :refer :all]
            [dev.chrisreyes.task-tracker.hierarchy :refer :all]))

(deftest create-root-test
  (testing "Can create root"
    (doseq [value [1 2]]
      (let [root (create-root value)]
        (is (= (:this-numerator root) value)
            "Numerator should be the supplied value")
        (is (= (:this-denominator root) 1)
            "Denominator for root tasks is always 1")
        (is (= (:next-numerator root) (inc value))
            "Numerator for next root Task should be incremented")
        (is (= (:next-denominator root) 1)
            "Denominator for root tasks is always 1")
        (is (= (:num-children root) 0) "Root tasks start with no children")))))

(deftest get-child-value-test
  (testing "Can calculate child value"
    ; See https://arxiv.org/pdf/0806.3115.pdf section 2.1
    ; nvc = nvp + c * snvp
    ; dvc = dvp + c * sdvp
    ; For c = 1, vp = 1, svp = 2:
    ; 1 + (1 * 2) = 3
    (is (= (get-child-value 1 1 2) 3))
    ; For c = 2, vp = 1, svp = 2:
    ; 1 + (2 * 2) = 5
    (is (= (get-child-value 2 1 2) 5))
    ; For c = 1, vp = 2, svp = 3:
    ; 2 + (1 * 3) = 5
    (is (= (get-child-value 1 2 3) 5))
    ; For c = 2, vp = 2, svp = 3:
    ; 2 + (2 * 3) = 8
    (is (= (get-child-value 2 2 3) 8))))

(deftest add-child-test
  (testing "Can add a child to a root"
    (let [root {:this-numerator 1
                :this-denominator 1
                :next-numerator 2
                :next-denominator 1}
          result (add-child root)
          updated-root (:hierarchy-node result)
          child (:child result)]
      (is (some? updated-root) "Should have returned the updated root")
      (is (some? child) "Should have created a child node")
      (doseq [prop [:this-numerator
                    :this-denominator
                    :next-numerator
                    :next-denominator]]
        (is (= (get root prop)
               (get updated-root prop))
            (str "Should not have altered root " prop " property")))
      (is (= (:num-children updated-root) 1)
          "Should have added one child")
      (is (= (:this-numerator child)
             (get-child-value (:num-children updated-root)
                              (:this-numerator updated-root)
                              (:next-numerator updated-root)))
          "Should have set numerator value based on get child value")
      (is (= (:next-numerator child)
             (get-child-value (inc (:num-children updated-root))
                              (:this-numerator updated-root)
                              (:next-numerator updated-root)))
          "Should have set next numerator value based on get child value")
      (is (= (:this-denominator child)
             (get-child-value (:num-children updated-root)
                              (:this-denominator updated-root)
                              (:next-denominator updated-root)))
          "Should have set denominator value based on get child value")
      (is (= (:next-denominator child)
             (get-child-value (inc (:num-children updated-root))
                              (:this-denominator updated-root)
                              (:next-denominator updated-root)))
          "Should have set next denominator value based on get child value")
      (is (= (:num-children child) 0)
          "Child nodes should be created without children of their own"))))
