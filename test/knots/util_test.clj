(ns knots.util-test
  (:require [midje.sweet :refer [fact =>]]
            [knots.util :refer [accum compr]]))

;; ## Accumulation

;; `accum` is similar to `reduce`, only that instead of returning a single value
;; it returns a lazy sequence, containing all intermediate values.

;; If given an empty sequence, it returns a sequence containing only the initial
;; value.
(fact
 (accum (fn [x a]) 42 []) => [42])

;; For the first element in the collection, the function is called with that
;; element and the initial value.
(fact
 (accum (fn [x a]
          (+ (* x 2) a)) 42 [5]) => [52])

;; Additional elements are given to the function with the last value returned by
;; it.
(fact
 (accum (fn [x a]
          (conj a x)) #{} [1 2 3]) => [#{1} #{1 2} #{1 2 3}])

;; ## Comprehension

;; The macro `compr` takes the idea of `let` and `for` and generalizes it to a
;; combination of both, along with reduce operations. It transforms a
;; comprehension into a sequence of `map`, `mapcat`, `filter` and `accum`.

;; Without any bindings, it returns a sequence with a single element, which is
;; the value of the given expression.
(fact
 (compr []
        :foo) => [:foo])

;; Regular bindings are bound to variables which can appear in the body of the
;; `compr`.
(fact
 (compr [a 1]
        {:foo a}) => [{:foo 1}])

;; Bindings are available for subsequence bindings.
(fact
 (compr [a 1
         b (inc a)]
        b) => [2])

;; Double deref (`@@` prefix) on the right-hand side of a binding should be read
;; as "in". This is similar to a `for` macro, where a result is returned for
;; each element in the collection on the right-hand side.
(fact
 (compr [x @@[1 2 3]
         y (inc x)]
        y) => [2 3 4])

;; "in" bindings can depend on other bindings.
(fact
 (compr [x @@[1 2 3]
         y @@(range x)]
        y) => [0 0 1 0 1 2])