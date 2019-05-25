(ns task-tracker.persistence-test
  (:require [clojure.test :refer :all]
            [task-tracker.persistence :as persistence]))

(deftest filter-nil-values-test
  (testing "Edge cases for filter-nil-values"
    (is (= (@#'persistence/filter-nil-values {}) {})
        "filter-nil-values should leave an empty map alone")
    (let [only-1-nil {:a nil}
          only-2-nils {:duck nil :goose nil}]
      (is (= (@#'persistence/filter-nil-values only-1-nil) {})
          "fitler-nil-values should return an empty map if only value is nil")
      (is (= (@#'persistence/filter-nil-values only-2-nils) {})
          "filter-nil-values should return an empty map if all values are nil"))
    (let [only-value {:a "duck"}
          all-values {:duck "duck" :goose "!!!"}]
      (is (= (@#'persistence/filter-nil-values only-value) only-value)
          "filter-nil-values should return a map unchanged with no nil values"))
    (let [happy "clojure"
          some-values {:happy happy :sad nil}
          some-truths {:learning true :bored false :tired nil}]
      (is (= (@#'persistence/filter-nil-values some-values) {:happy happy})
          "filter-nil-values should return a map with only non-nil values")
      (is (= (@#'persistence/filter-nil-values some-truths) {:learning true
                                                             :bored false})))))

(deftest get-config-test
  (testing "Can get the config from a secret JSON dict"
    (let [secret {}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          "Configuration should be for a postgres database, even from empty secret"))
    (let [secret {:dbname "somedatabase"
                  :host "https://u.r.l"
                  :password "s0Secr3t"
                  :port "1234"
                  :username "security"
                  :ignore-me "Woo!"}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          "Configuration should be for a postgres database")
      (doseq [property [:dbname :host :password :port]]
        (is (= (get config property) (get secret property))
            (str "Should preserve the " property " key")))
      (is (= (:user config) (:username secret))
          "Should rename :username to :user")
      (is (not (contains? config :ignore-me))
          "Should ignore irrelevant keys"))
    (let [secret {:dbtype "mysql"}
          config (persistence/get-config secret)]
      (is (= (:dbtype config) "postgres")
          "Should always have config for postgres database, ignoring secret :dbtype"))))
