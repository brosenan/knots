(ns knots.util-test
  (:require [midje.sweet :refer [fact =>]]
            [knots.util :refer [accum until compr]]))

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

;; ## Sequence Termination

;; The function `until` takes a predicate function and a collection. and returns
;; a lazy collection. Given an empty sequence, it returns the same empty
;; sequence.
(fact
 (until even? []) => [])

;; For a non-empty sequence, the output will end on the first element to which
;; the predicate returns `true`.
(fact
 (until even? [1 2 3]) => [1 2])

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

;; ### Ranging over Collections

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
        [x y]) => [[1 0] [2 0] [2 1] [3 0] [3 1] [3 2]])

;; ### Conditions

;; If the binding variable is replaced with `:when`, the term on the right hand
;; side is taken as a predicate for filtering results.
(fact
 (compr [x @@[1 2 3]
         :when (= (mod x 2) 1)]
        x) => [1 3])

;; The keyword `:until` causes the resulting sequence to end on the first
;; element for which the expression on the right-hand side is true.
(fact
 (compr [x @@[1 2 3]
         :until (even? x)]
        x) => [1 2])

;; ### Reductions

;; If a binding variable appears more than once in the bindings list, this is
;; interpreted as a stateful update. The initial binding defines the initial
;; state, and every other appearance is an update to that state.

;; In the following example we sum the elements of a vector by defining a `sum`
;; binding, which is initialized to `0`, then we iterate over a vector and add
;; each value to `sum`.
(fact
 (compr [sum 0
         x @@[1 2 3]
         sum (+ sum x)]
        sum) => [1 3 6])