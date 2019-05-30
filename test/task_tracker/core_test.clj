(ns task-tracker.core-test
  (:require [clojure.test :refer :all]
            [task-tracker.core :refer :all]))

(deftest main-test
  (testing "Has main function"
    (is (some? -main) "Core should define a -main function")))
