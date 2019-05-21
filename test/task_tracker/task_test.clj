(ns task-tracker.task-test
  (:require [clojure.test :refer :all]
            [task-tracker.task :refer :all]))

(deftest test-task-type
  (testing "Defines a Task record type"
    (is (some? map->Task) "task namespace should define a Task record type")))
