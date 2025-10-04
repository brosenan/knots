* [Knots Library](#knots-library)
  * [Representation and Validity](#representation-and-validity)
    * [The Meaning of the Vector](#the-meaning-of-the-vector)
    * [Proper Knots](#proper-knots)
    * [Canonical Representation](#canonical-representation)
  * [Knot Geometry](#knot-geometry)
    * [Tracing Edges](#tracing-edges)
```clojure
(ns knots.core-test
  (:require [midje.sweet :refer [fact =>]]
   [knots.core :refer [check-vec check-proper canonicalize index-edges
                       ascending-edges ascending-step sector? all-sectors
                       sector-ascending-edges check-geometric]]))

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
 (check-vec [1 -2 3 -1 2 -3]) => nil?
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

### Proper Knots

We use the term _proper knot_ to refer to a knot in which the rope alternates
between the "above" and "under" position at every crossing. By this, an
"improper knot" is a knot that has at least one rope section that goes from
"above" to "above", or from "under" to "under",

The function `check-proper` returns `nil` for vectors representing proper
knots.
```clojure
(fact
 (check-proper [1 -2 3 -1 2 -3]) => nil
 (check-proper [-1 2 -3 1 -2 3]) => nil)

```
If, however, we swap the positions of crossing `2` in the above trefold from
above to under and vice versa, we will get an improper knot, containing four
sections that do not alternate. `check-proper` returns them.
```clojure
(fact
 (check-proper [1 2 3 -1 -2 -3]) => {:above #{[1 2] [2 3]}
                                     :under #{[-1 -2] [-2 -3]}})

```
The segment that wraps around the vector is also checked.
```clojure
(fact
 (check-proper [-1 -2 3 1 2 -3]) => {:above #{[3 1] [1 2]}
                                     :under #{[-1 -2] [-3 -1]}})

```
### Canonical Representation

While any vector that meets the above criteria represents a knot, we would
like to restrict our representation in order to limit, as much as possible,
the number of different representations for a given knot.

We do not aim at bringing this number down to one due to symmetry between the
crossings, but we can bring this number to O(n), if we follow a simple
convention, the so called _cannonical representation_.

Given a knot, we select a crossing and a direction of the rope going above
it. This crossing is given the number 1. As we follow the rope in the
selected direction, we give each unique crossing we encounter the lowest
absolute value not already allocated.

`canonicalize` takes a vector representing a valid (not necessarily proper)
knot and turns it into its canonical form.
```clojure
(fact
 (canonicalize [3 -2 1 -3 2 -1]) => [1 -2 3 -1 2 -3])

```
## Knot Geometry

One important criterion for a vector to actually represent a knot is for the
underlying knot to actually make sense geometrically. That is, we need to
make sure that when we route the rope through the required crossing points,
we are not getting additional crossings that are not accounted for by the
vector.

One way of making sure this is indeed the case is by looking at _sectors_,
and the sections that make them.

Sectors are parts of the 2D plane that are enclosed by rope sections. Because
sections divide the plane into sectors, each section should appear at the
boundary of exactly two sectors.

Sectors can be found by tracing _ascending edges_ of the knot. An _edge_ is a
rope segment, considered with a direction. An _ascending edge_ is an edge
that goes from an "under" position in one crossing to an "above" position in
a neighboring corssing. For example, in the knot `[1 -2 2 -1]`, there are
three  ascending nodes: `[-2 1]` (the first two elements, in reverse),
`[-2 2]`, and `[-1 1]` (wrapping around the vector).

### Tracing Edges

To be able to efficiently trace edges, we need to index them. `index-edges`
does just that. It takes a vector and returns a map from its crossing numbers
to pairs `[predecessor successor]`, which list the crossing numbers before
and after it (with the proper sign assigned).
```clojure
(fact
 (index-edges [1 -2 3 -1 2 -3]) => {1 [-3 -2]
                                    -2 [1 3]
                                    3 [-2 -1]
                                    -1 [3 2]
                                    2 [-1 -3]
                                    -3 [2 1]})

```
In this index, the ascending edges are represented by the negative keys in
the map. Each negative key points to a pair of positive values, each of them
corresponds to a different ascendig edge. For example, `-2` points to `[1
3]`, making both `[-2 1]` and `[-2 3]` ascending edges in this knot.

The function `ascending-edges` takes an index and enumerates all the
ascending edges it represents.
```clojure
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     ascending-edges) => #{[-2 1] [-2 3] [-1 3] [-1 2] [-3 2] [-3 1]})

```
We define an _ascending path_ as a path along distinct crossings, going only
through ascending edges. We start with a negative number (say, -1), and find
its two neighbors in the map. We choose one of them (say, 3) and continue the
process from its negative (-3). We continue the process until we encounter a
crossing we have already seen.

`ascending-step` takes an edge index and a partial path, given as a sequence
of crossing numbers (absolute values) given in reverse, so that the current
location is the first element, and returns a sequence of the (two) immediate
continuations of this path.
```clojure
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     (ascending-step (list 3 1))) => [[2 3 1]
                                     [1 3 1]])

```
The function `sector?` takes a path and returns `nil` if the path is open.
```clojure
(fact
 (sector? (list 1 2 3)) => nil)

```
If the path ends in a closed loop (i.e., the first element appears earlier in
the path), the closed part it returned.
```clojure
(fact
 (sector? (list 1 2 3 1 2)) => [1 2 3])

```
Since a path around a sector can start at any point, we start the cyclic
section from the lowest crossing number, to get a unique sequence for the
segment.
```clojure
(fact
 (sector? (list 2 3 1 2)) => [1 2 3])

```
`all-sectors` takes an edge-index of a knot and returns all the sectors in
the knot.
```clojure
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     all-sectors) => #{[1 2] [1 2 3] [1 3] [1 3 2] [2 3]})

```
The function `sector-ascending-edges` takes a sector sequence and returns the
ascending edges that make it. Remember that the crossings in the sector
sequence are in reverse order, so for a sequence `... a b ...`, `[-b a]` will
be an ascending edge.
```clojure
(fact
 (sector-ascending-edges [1 2 3 4]) => #{[-2 1] [-3 2] [-4 3] [-1 4]})

```
Now we have all the necessary components for the geometric test. A knot is
considered "geometric" if every ascending edge appears in exactly two
sectors Because sectors do not have repeating crossings, it's sufficient to
check that _a sequence of all ascending edges, making up all sectors_,
contains each ascending edge in the knot exactly twice.

The function `check-geometric` takes an edge index and returns `nil` for
"geometric" knots, such as the trefoil.
```clojure
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     check-geometric) => nil)

```
For a knot that is not geometric, it returns a map with the edges that do not
give a count of 2.
```clojure
(fact
 (-> [1 -2 3 -4 5 -3 4 -1 2 -5]
     index-edges
     check-geometric) => #{[-5 1] [-5 2] [-4 3] [-4 5] [-3 4] [-3 5] [-2 1] [-2 3] [-1 2] [-1 4]})
```

