* [Knots Library](#knots-library)
  * [Representation and Validity](#representation-and-validity)
    * [The Meaning of the Vector](#the-meaning-of-the-vector)
```clojure
(ns knots.core-test
  (:require [midje.sweet :refer [fact =>]]
   [knots.core :refer [check-vec]]))

```
# Knots Library

This library is intended to provide representations and functions for knots,
as their meaning in [knot theory](https://en.wikipedia.org/wiki/Knot_theory).

## Representation and Validity

A knot is represented as a *vector of integers* of size `2*n`, containing
values `1..n` and `-n..-1`, each value appears exactly once in the vector.

For such a vector to be a valid knot, a few conditions need to hold. The
critiria described above are the first ones, and are checked by the function
`check-vec`.

For a valid knot, it returns `nil`. Specifically, the Unknot is represented
by an empty vector.
```clojure
(fact
 (check-vec []) => nil?)

```
For something that is not a proper knot prepresentation, a map with a proper
key is returned, explaining why this is not a knot.

If the vector is of an odd length, `:odd-len` is returned with the length of the
vector.
```clojure
(fact
 (check-vec [1 2 3]) => {:odd-len 3})

```
If the vector contains `0`, `{:contains-zero true}` is returned.
```clojure
(fact
 (check-vec [1 0]) => {:contains-zero true})

```
If the vector contains a number larger than `n` (half of the length) or lower
than `-n`, `:invalid-node-number` is returned, with the bad value.
```clojure
(fact
 (check-vec [1 2]) => {:invalid-node-number 2}
 (check-vec [1 -2]) => {:invalid-node-number -2})

```
Finally, it returns `:repeated` if a value is repeated.
```clojure
(fact
 (check-vec [1 1]) => {:repeated 1})

```
This leaves valid vectors, which include both "proper" knots, e.g., the
[trefoil knot](https://en.wikipedia.org/wiki/Trefoil_knot), but also "quasi-knots"
such as the one obtained from an unknot, when flipping one side of it around,
creating one crossing.
```clojure
(fact
 ;; Trefoil knot
 (check-vec [1 2 3 -1 -2 -3]) => nil?
 ;; Unknot with a twist
 (check-vec [1 -1]) => nil?
 )

```
### The Meaning of the Vector

A vector can be understood as follows:

1. The rope is represented by the positions in the vector, with the end
   wrapping around to the beginning.
2. The crossings are represented by the _absolute values_ of the numbers in
   the vector.
3. The sign represents the position in which the rope is threaded through a
   crossing. A positive value means "above", and a negative value means
   "under". Since each corssing has one piece of rope going above and one
   going under, each of the values appears once.
