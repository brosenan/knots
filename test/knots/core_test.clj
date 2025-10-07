(ns knots.core-test
  (:require [midje.sweet :refer [fact =>]]
   [knots.core :refer [check-vec check-proper canonicalize all-rotations
                       rotate-reverse all-equivalent index-edges ascending-edges
                       ascending-step sector? all-sectors sector-ascending-edges
                       check-geometric slice try-untwist untwist simplify
                       selector element-candidates knot-candidate
                       index-catalog check-all catalog-consider]]))

;; # Knots Library

;; This library is intended to provide representations and functions for knots,
;; as their meaning in [knot theory](https://en.wikipedia.org/wiki/Knot_theory).

;; ## Representation and Validity

;; A knot is represented as a *vector of integers* of size `2*n`, containing
;; values `1..n` and `-n..-1`, each value appears exactly once in the vector.

;; For such a vector to be a valid knot, a few conditions need to hold. The
;; critiria described above are the first ones, and are checked by the function
;; `check-vec`.

;; For a valid knot, it returns `nil`. Specifically, the Unknot is represented
;; by an empty vector.
(fact
 (check-vec []) => nil?)

;; For something that is not a proper knot prepresentation, a map with a proper
;; key is returned, explaining why this is not a knot.

;; If the vector is of an odd length, `:odd-len` is returned with the length of the
;; vector.
(fact
 (check-vec [1 2 3]) => {:odd-len 3})

;; If the vector contains `0`, `{:contains-zero true}` is returned.
(fact
 (check-vec [1 0]) => {:contains-zero true})

;; If the vector contains a number larger than `n` (half of the length) or lower
;; than `-n`, `:invalid-node-number` is returned, with the bad value.
(fact
 (check-vec [1 2]) => {:invalid-node-number 2}
 (check-vec [1 -2]) => {:invalid-node-number -2})

;; Finally, it returns `:repeated` if a value is repeated.
(fact
 (check-vec [1 1]) => {:repeated 1})

;; This leaves valid vectors, which include both "proper" knots, e.g., the
;; [trefoil knot](https://en.wikipedia.org/wiki/Trefoil_knot), but also "quasi-knots"
;; such as the one obtained from an unknot, when flipping one side of it around,
;; creating one crossing.
(fact
 ;; Trefoil knot
 (check-vec [1 -2 3 -1 2 -3]) => nil?
 ;; Unknot with a twist
 (check-vec [1 -1]) => nil?
 )

;; ### The Meaning of the Vector

;; A vector can be understood as follows:

;; 1. The rope is represented by the positions in the vector, with the end
;;    wrapping around to the beginning.
;; 2. The crossings are represented by the _absolute values_ of the numbers in
;;    the vector.
;; 3. The sign represents the position in which the rope is threaded through a
;;    crossing. A positive value means "above", and a negative value means
;;    "under". Since each corssing has one piece of rope going above and one
;;    going under, each of the values appears once.

;; ### Proper Knots

;; We use the term _proper knot_ to refer to a knot in which the rope alternates
;; between the "above" and "under" position at every crossing. By this, an
;; "improper knot" is a knot that has at least one rope section that goes from
;; "above" to "above", or from "under" to "under",

;; The function `check-proper` returns `nil` for vectors representing proper
;; knots.
(fact
 (check-proper [1 -2 3 -1 2 -3]) => nil
 (check-proper [-1 2 -3 1 -2 3]) => nil)

;; If, however, we swap the positions of crossing `2` in the above trefold from
;; above to under and vice versa, we will get an improper knot, containing four
;; sections that do not alternate. `check-proper` returns them.
(fact
 (check-proper [1 2 3 -1 -2 -3]) => {:above #{[1 2] [2 3]}
                                     :under #{[-1 -2] [-2 -3]}})

;; The segment that wraps around the vector is also checked.
(fact
 (check-proper [-1 -2 3 1 2 -3]) => {:above #{[3 1] [1 2]}
                                     :under #{[-1 -2] [-3 -1]}})

;; ### Canonical Representation

;; While any vector that meets the above criteria represents a knot, we would
;; like to restrict our representation in order to limit, as much as possible,
;; the number of different representations for a given knot.

;; We do not aim at bringing this number down to one due to symmetry between the
;; crossings, but we can bring this number to O(n), if we follow a simple
;; convention, the so called _cannonical representation_.

;; Given a knot, we select a crossing and a direction of the rope going above
;; it. This crossing is given the number 1. As we follow the rope in the
;; selected direction, we give each unique crossing we encounter the lowest
;; absolute value not already allocated.

;; `canonicalize` takes a vector representing a valid (not necessarily proper)
;; knot and turns it into its canonical form.
(fact
 (canonicalize [3 -2 1 -3 2 -1]) => [1 -2 3 -1 2 -3])

;; Since by convention the first element in the vector needs to be positive
;; (passing the crossing from above), when given a negative value as the first
;; element, the vector is rotated one place before moving on.
(fact
 (canonicalize [-1 3 -2 1 -3 2]) => [1 -2 3 -1 2 -3])

;; ### Equivalent Vectors

;; The canonical representation still keeps the choice of the initial crossing
;; and the direction free. As a result, a knot with `n` crossings can be
;; represented using up to `2n` different canonical vectors, one for each
;; initial crossing and direction. It is possible that there are actually way
;; fewer different actual vectors, due to symmetries in the knot. For example,
;; we expect that a trefoil knot has exactly one representation. However, other,
;; less symmetric knots can have more.

;; As a first step towards finding all representations, the function
;; `all-rotations` takes a vector representing a knot and rotates enumerates all
;; the ways it can be rotated in pairs, such that for a proper knot, all
;; rotations start with a positive number.
(fact
 (all-rotations [1 -2 3 -1 2 -3]) => [[1 -2 3 -1 2 -3]
                                      [3 -1 2 -3 1 -2]
                                      [2 -3 1 -2 3 -1]])

;; For choosing the direction, we need to reverse the given vector. However,
;; since for a canonical representation we need to start from above a crossing
;; (a positive number), we first need to rotate the vector one place before
;; reversing it.

;; The function `rotate-reverse` does this. It rotates the given vector one
;; element and then reverses it.
(fact
 (rotate-reverse [1 -2 3 -1 2 -3]) => [1 -3 2 -1 3 -2])

;; Finally, `all-equivalent` takes a vector representing a knot and returns a
;; set of all canonical representations of the same knot, consisting of all
;; choices of initial crossings and directions. The output is a set, so that
;; outputs with the same canonical representations are unified into one, thus
;; minimizing the number of representations.
(fact
 (all-equivalent [1 -2 3 -1 2 -3]) => #{[1 -2 3 -1 2 -3]}
 (all-equivalent [1 -2 3 -4 2 -3 4 -1]) => #{[1 -2 2 -3 4 -1 3 -4]
                                             [1 -2 3 -4 2 -3 4 -1]
                                             [1 -2 3 -4 4 -1 2 -3]
                                             [1 -2 3 -3 4 -1 2 -4]
                                             [1 -2 3 -1 2 -4 4 -3]
                                             [1 -2 3 -1 2 -3 4 -4]
                                             [1 -2 3 -1 4 -4 2 -3]
                                             [1 -1 2 -3 4 -2 3 -4]})

;; ## Knot Geometry

;; One important criterion for a vector to actually represent a knot is for the
;; underlying knot to actually make sense geometrically. That is, we need to
;; make sure that when we route the rope through the required crossing points,
;; we are not getting additional crossings that are not accounted for by the
;; vector.

;; One way of making sure this is indeed the case is by looking at _sectors_,
;; and the sections that make them.

;; Sectors are parts of the 2D plane that are enclosed by rope sections. Because
;; sections divide the plane into sectors, each section should appear at the
;; boundary of exactly two sectors.

;; Sectors can be found by tracing _ascending edges_ of the knot. An _edge_ is a
;; rope segment, considered with a direction. An _ascending edge_ is an edge
;; that goes from an "under" position in one crossing to an "above" position in
;; a neighboring corssing. For example, in the knot `[1 -2 2 -1]`, there are
;; three  ascending nodes: `[-2 1]` (the first two elements, in reverse),
;; `[-2 2]`, and `[-1 1]` (wrapping around the vector).

;; ### Tracing Edges

;; To be able to efficiently trace edges, we need to index them. `index-edges`
;; does just that. It takes a vector and returns a map from its crossing numbers
;; to pairs `[predecessor successor]`, which list the crossing numbers before
;; and after it (with the proper sign assigned).
(fact
 (index-edges [1 -2 3 -1 2 -3]) => {1 [-3 -2]
                                    -2 [1 3]
                                    3 [-2 -1]
                                    -1 [3 2]
                                    2 [-1 -3]
                                    -3 [2 1]})

;; In this index, the ascending edges are represented by the negative keys in
;; the map. Each negative key points to a pair of positive values, each of them
;; corresponds to a different ascendig edge. For example, `-2` points to `[1
;; 3]`, making both `[-2 1]` and `[-2 3]` ascending edges in this knot.

;; The function `ascending-edges` takes an index and enumerates all the
;; ascending edges it represents.
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     ascending-edges) => #{[-2 1] [-2 3] [-1 3] [-1 2] [-3 2] [-3 1]})

;; We define an _ascending path_ as a path along distinct crossings, going only
;; through ascending edges. We start with a negative number (say, -1), and find
;; its two neighbors in the map. We choose one of them (say, 3) and continue the
;; process from its negative (-3). We continue the process until we encounter a
;; crossing we have already seen.

;; `ascending-step` takes an edge index and a partial path, given as a sequence
;; of crossing numbers (absolute values) given in reverse, so that the current
;; location is the first element, and returns a sequence of the (two) immediate
;; continuations of this path.
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     (ascending-step (list 3 1))) => [[2 3 1]
                                     [1 3 1]])

;; The function `sector?` takes a path and returns `nil` if the path is open.
(fact
 (sector? (list 1 2 3)) => nil)

;; If the path ends in a closed loop (i.e., the first element appears earlier in
;; the path), the closed part it returned.
(fact
 (sector? (list 1 2 3 1 2)) => [1 2 3])

;; Since a path around a sector can start at any point, we start the cyclic
;; section from the lowest crossing number, to get a unique sequence for the
;; segment.
(fact
 (sector? (list 2 3 1 2)) => [1 2 3])

;; `all-sectors` takes an edge-index of a knot and returns all the sectors in
;; the knot.
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     all-sectors) => #{[1 2] [1 2 3] [1 3] [1 3 2] [2 3]})

;; The function `sector-ascending-edges` takes a sector sequence and returns the
;; ascending edges that make it. Remember that the crossings in the sector
;; sequence are in reverse order, so for a sequence `... a b ...`, `[-b a]` will
;; be an ascending edge.
(fact
 (sector-ascending-edges [1 2 3 4]) => #{[-2 1] [-3 2] [-4 3] [-1 4]})

;; Now we have all the necessary components for the geometric test. A knot is
;; considered "geometric" if every ascending edge appears in exactly two
;; sectors Because sectors do not have repeating crossings, it's sufficient to
;; check that _a sequence of all ascending edges, making up all sectors_,
;; contains each ascending edge in the knot exactly twice.

;; The function `check-geometric` takes an edge index and returns `nil` for
;; "geometric" knots, such as the trefoil.
(fact
 (-> [1 -2 3 -1 2 -3]
     index-edges
     check-geometric) => nil)

;; For a knot that is not geometric, it returns a map with the edges that do not
;; give a count of 2.
(fact
 (-> [1 -2 3 -4 5 -3 4 -1 2 -5]
     index-edges
     check-geometric) => #{[-5 1] [-5 2] [-4 3] [-4 5] [-3 4] [-3 5] [-2 1] [-2 3] [-1 2] [-1 4]})

;; ## Simplifying a Knot

;; All improper knots and some proper knots can be simplified, i.e., brought to
;; a form with fewer crossings, without the need to cut the rope, i.e., without
;; changing the fundamental nature of the knot.

;; Simplification is typically done by first observing a pattern in the knot,
;; then applying a simplification operation based on this pattern, one that
;; preserves the knot's nature, while removing at least one crossing.

;; ### Untwisting

;; We define a _twist_ in a knot as a crossing that is created by _twisting_ a
;; part of a knot. In our definition, twisting requires that the knot consists
;; of two separate parts, connected by a pair of rope segments. Twisting entails
;; flipping one of the parts, thus twisting the two ropes segments, creating a
;; new crossing.

;; Accordingly, untwisting means idetifying such a crossing, separating between
;; two distinct parts of the knot, and flipping one side to remove it.

;; To enable untwisting, the function `slice` takes a vector and two values on
;; the vector, and returns the sequence of values between them.
(fact
 (slice [1 2 3 4 5 6] 2 5) => [3 4])

;; It treats the vector as if it is closed on itself.
(fact
 (slice [1 2 3 4 5 6] 5 2) => [6 1])

;; The function `try-untwist` takes a vector and a crossing number (absolute
;; value). It then checks if this crossing is a twist.

;; If returns `nil` if the crossing is not a twist.
(fact
 (try-untwist [1 -2 3 -4 5 -5 4 -1 2 -3] 3) => nil)

;; If the selected crossing is a twist, it removes it, flipping one of the
;; parts, rearranging and canonicalizing the result.
(fact
 (try-untwist [1 -2 3 -4 5 -5 4 -1 2 -3] 4) => [1 -2 3 -1 2 -4 4 -3])

;; The function `untwist` takes a vector representing a knot as input and
;; returns `nil` if the vector has no twists.
(fact
 (untwist [1 -2 3 -1 2 -3]) => nil?)

;; However, if the knot has a twist, it will return an untwisted version of it.
(fact
 (untwist [1 -2 3 -4 5 -5 4 -1 2 -3]) => [1 -2 3 -1 2 -4 4 -3])

;; ### Repeated Application

;; The function `simplify` applies the above strategies, one at a time, to
;; simplify a knot until it is no longer possible.
(fact
 (simplify [1 -2 3 -4 5 -5 4 -1 2 -3]) => [1 -2 3 -1 2 -3])

;; ## Knot Discovery

;; An important part of knot theory is discovering and cataloging knots. While a
;; catalog already exists, we would like to have the ability to reproduce it.

;; To do this, we need a way to discover new knots. Overall, the process is
;; roughly the following. We "guess" a vector that is potentially a canonical
;; representation of a knot. We then test that it is indeed a knot, simplify it
;; as much as we can and check that it is not already known. Then, if it meets
;; all our criteria, we add it to our catalog, indexing it using all its
;; equivalent representations.

;; ### Deterministic Selection

;; In order to create the initial "guess", we need a way to make choices that
;; will change from one invocation to the other. However, we would like to keep
;; these selections deterministic so that the process is reproducible.

;; To do so, we define `selector`, a function that takes an integer
;; _seed_, and returns a `select` function. `select` takes a positive integer
;; and returns a number between 0 and that integer, exclusive.
(fact
 (let [select (selector 1234)]
   ;; Since we are dividing by 10 each time, we are getting the digits.
   (select 10) => 4
   (select 10) => 3
   (select 10) => 2
   (select 10) => 1
   ;; Once the value becomes 0, the value is replaced with the original value
   ;; * 31, which in this case is 1234*31=38254
   (select 10) => 4
   (select 10) => 5
   (select 10) => 2
   (select 10) => 8
   (select 10) => 3
   ;; Now the seed is multiplied again: 38254*31=1185874
   (select 10) => 4
   (select 10) => 7
   (select 10) => 8
   (select 10) => 5
   (select 10) => 8))

;; ### Next Element Candidates

;; We build knot candidate step by step, one element at a time. In each
;; iteration we first create a list of candidates and then use a `selector` to
;; select on of them, add it to the vector and repeat.

;; `element-candidates` takes a partial knot vector and a target number of
;; crossings, and returns a vector of candidates for the next element.

;; Since we are targeting a canonical representation, for an empty input vector,
;; the only output candidate will be 1.
(fact
 (element-candidates [] 5) => [1])

;; Given a non-empty initial vector, the candidates will all have a sign
;; opposing the last element, no larger in absolute value than the maximum + 1
;; or the target number of crossings, and with no repeats from the vector
;; itself. Also, to avoid trivial twists, we also disallow the negative of the
;; last element. The candidates are sorted by absolute values.
(fact
 (element-candidates [1] 5) => [-2]
 (element-candidates [1 -2 3] 5) => [-1 -4]
 (element-candidates [1 -2 3 -4] 5) => [2 5]
 (element-candidates [1 -2 3 -4 5] 5) => [-1 -3]
 (element-candidates [1 -2 3 -4 2 -1] 5) => [4 5])

;; From here, all it takes is to run this repeatedly, until there are no options
;; left.

;; `knot-candidate` takes a seed value and a target number of crossings, and
;; returns a vector, which may or may not a valid knot representation. However,
;; if it is, it is guaranteed to be canonical and proper.
(fact
 (knot-candidate 1001 3) => [1 -2 3 -1 2 -3]
 (knot-candidate 1001 4) => [1 -2 3 -4 2 -1 4 -3]
 (knot-candidate 1002 4) => [1 -2 3 -1 4 -3 2 -4]
 (knot-candidate 1001 7) => [1 -2 3 -4 2 -3 5 -6 4 -5 7 -1 6 -7])

;; ### Cataloging Knots

;; A _knot catalog_ is a map from an integer number of crossings to a set of all
;; (known) unique knots with that number of crossings.

;; Because a knot can be represented by up to `2n` equivalent representations,
;; `index-catalog` takes a catalog and indexes it, i.e., returns a map from
;; vectors to vectors, mapping all representations of a knot to its
;; representative in the catalog.
(fact
 (index-catalog {3 #{[1 -2 3 -1 2 -3]}
                 4 #{[1 -2 3 -4 2 -1 4 -3]}}) =>
 {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]
  [1 -2 3 -4 2 -1 4 -3] [1 -2 3 -4 2 -1 4 -3]
  [1 -2 3 -1 4 -3 2 -4] [1 -2 3 -4 2 -1 4 -3]})

;; Before we can catalog a knot candidate, we need to put it through all the
;; [checks defined](#representation-and-validity) [above](#knot-geometry) to
;; make sure this is indeed a valid knot.

;; `check-all` takes a knot vector and returns `nil` if it is a valid knot.
(fact
 (check-all [1 -2 3 -1 2 -3]) => nil?)

;; If this is not a valid knot, it returns a map with the reason.
(fact
 (check-all [1 -2 3]) => {:odd-len 3}
 (check-all [1 0]) => {:contains-zero true}
 (check-all [1 -2]) => {:invalid-node-number -2}
 (check-all [1 1]) => {:repeated 1}
 (check-all [1 2 3 -1 -2 -3]) => {:improper {:above #{[1 2] [2 3]}
                                             :under #{[-1 -2] [-2 -3]}}}
 (check-all [1 -2 3 -4 5 -3 4 -1 2 -5]) =>
 {:geometry #{[-5 1] [-5 2] [-4 3] [-4 5] [-3 4]
              [-3 5] [-2 1] [-2 3] [-1 2] [-1 4]}})

;; ### Updating the Catalog

;; Because a catalog needs to be indexed, and because we do not want to
;; recompute it for every change to the catalog, but rather update it
;; incrementally, we would like to package the catalog along with its index for
;; most catalog-related operations. In fact, since the operations of populating
;; the catalog are often long-running we can use such a packaging to add more
;; statistics and diagnostic information to get more visibility to the process.

;; We therefore use a map with the following keys in catalog-related functions,
;; as both input and output:

;; * `:catalog`: The catalog map.
;; * `:index`: The catalog index (map).
;; * `:stats`: A map containing counts of candidates that did not make the
;;   catalog, keyed by reason.

;; `catalog-consider` takes a map as described above and a candidate knot. It
;; checks the knot, tries to [simplify](#simplifying-a-knot) it, then looks for
;; it in the index. If not found, it adds it to the catalog.
(fact
 (let [cat {:catalog {3 [1 -2 3 -1 2 -3]}
            :index {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]}
            :stats {:odd-len 1}}]
   (catalog-consider cat [1 2 3]) => {:catalog {3 [1 -2 3 -1 2 -3]}
                                      :index {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]}
                                      :stats {:odd-len 2}}
   (catalog-consider cat [1 0]) => {:catalog {3 [1 -2 3 -1 2 -3]}
                                    :index {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]}
                                    :stats {:odd-len 1
                                            :contains-zero 1}}
   (catalog-consider cat [1 -2 3 -1 2 -3]) => {:catalog {3 [1 -2 3 -1 2 -3]}
                                               :index {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]}
                                               :stats {:odd-len 1
                                                       :dup 1}}
   (comment (catalog-consider cat [1 -2 3 -4 2 -1 4 -3]) => {:catalog {3 [1 -2 3 -1 2 -3]
                                                                       4 [1 -2 3 -4 2 -1 4 -3]}
                                                             :index {[1 -2 3 -1 2 -3] [1 -2 3 -1 2 -3]
                                                                     [1 -2 3 -4 2 -1 4 -3] [1 -2 3 -4 2 -1 4 -3]
                                                                     [1 -2 3 -1 4 -3 2 -4] [1 -2 3 -4 2 -1 4 -3]}
                                                             :stats {:odd-len 1}})))