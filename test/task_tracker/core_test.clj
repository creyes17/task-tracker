(ns task-tracker.core-test
  (:require [clojure.test :refer :all]
            [task-tracker.core :refer :all]))

(deftest test-create-root
  (testing "Can create root"
    (doseq [value [1 2]]
      (let [root (create-root value)]
        (is (= (:this-numerator root) value) "Numerator should be the supplied value")
        (is (= (:this-denominator root) 1) "Denominator for root tasks is always 1")
        (is (= (:next-numerator root) (inc value)) "Numerator for next root Task should be incremented")
        (is (= (:next-denominator root) 1) "Denominator for root tasks is always 1")
        (is (= (:num-children root) 0) "Root tasks start with no children")))))


(deftest test-add-child
  (testing "TODO: add test for add child"
    (let [root (create-root 1)]
      (is (not (nil? (add-child root)))))))
