(ns task-tracker.task-test
  (:require [clojure.test :refer :all]
            [task-tracker.task :refer :all]))

(deftest task-type-test
  (testing "Defines a Task record type"
    (is (some? map->Task) "task namespace should define a Task record type")))

(deftest create-task-test
  (testing "Can create a task with defaults"
    (let [issue-link "Some issue link"
          hierarchy-node {}
          task (create-task issue-link hierarchy-node)]
      (is (= (:hierarchy-node task) hierarchy-node)
          "Should have created a task with the given hierarchy node")
      (is (= (:issue-link task) issue-link)
          "Should have used the given issue link when creating the task")
      (is (some #(= (:estimated-time-minutes task) %)
                [0 nil])
          "Should use a zero-like default value for estimated time in minutes")
      (is (some #(= (:actual-time-minutes task) %)
                [0 nil])
          "Should use a zero-like default value for actual time in minutes")))
  (testing "Can create a task with estimated time"
    (let [issue-link "http://something.somewhere"
          hierarchy-node {:hierarchy-id 3}
          estimated-time 45
          task (create-task issue-link hierarchy-node estimated-time)]
      (is (= (:hierarchy-node task) hierarchy-node)
          "Should have created a task with the given hierarchy node")
      (is (= (:issue-link task) issue-link)
          "Should have used the given issue link when creating the task")
      (is (= (:estimated-time-minutes task) estimated-time)
          "Should use the given value for estimated time in minutes")
      (is (some #(= (:actual-time-minutes task) %)
                [0 nil])
          "Should use a zero-like default value for actual time in minutes")))
  (testing "Can create a task with estimated and actual times"
    (let [issue-link "https://taiga.io/1234"
          hierarchy-node {:this-numerator 4
                          :this-denominator 1
                          :next-numerator 5
                          :next-denominator 1}
          estimated-time 45
          actual-time 182
          task (create-task issue-link
                            hierarchy-node
                            estimated-time
                            actual-time)]
      (is (= (:hierarchy-node task) hierarchy-node)
          "Should have created a task with the given hierarchy node")
      (is (= (:issue-link task) issue-link)
          "Should have used the given issue link when creating the task")
      (is (= (:estimated-time-minutes task) estimated-time)
          "Should use the given value for estimated time in minutes")
      (is (= (:actual-time-minutes task) actual-time)
          "Should use the given value for actual time in minutes"))))
