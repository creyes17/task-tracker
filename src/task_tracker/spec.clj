(ns ^{:author "Christopher R Reyes"
      :doc "Optional specifications for use with Clojure 1.9 or later."}
  task-tracker.spec
  (:require [clojure.spec.alpha :as spec]
            [task-tracker.hierarchy :as hierarchy]))

;; Hierarchy Node schema
;; See https://arxiv.org/pdf/0806.3115.pdf
(spec/def ::hierarchy-id int?)
(spec/def ::next-denominator int?)
(spec/def ::next-numerator int?)
(spec/def ::num-children int?)
(spec/def ::this-denominator int?)
(spec/def ::this-numerator int?)
;; Note: I'm marking all of these fields as optional in preparation for
;; spec2 where the specific desired properties are defined for each fn.
(spec/def ::hierarchy-node (spec/keys :opt-un [::hierarchy-id
                                               ::next-denominator
                                               ::next-numerator
                                               ::num-children
                                               ::this-denominator
                                               ::this-numerator]))

;; Task schema
(spec/def ::actual-time-minutes int?)
(spec/def ::estimated-time-minutes int?)
(spec/def ::issue-link string?)
(spec/def ::task-id int?)
;; Note: As in ::hierarchy-node, marking all fields optional.
(spec/def ::task (spec/keys :opt-un [::actual-time-minutes
                                     ::estimated-time-minutes
                                     ::hierarchy-node
                                     ::issue-link
                                     ::task-id]))

;; The function API
; TODO: When I understand spec better
