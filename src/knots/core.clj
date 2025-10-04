(ns knots.core
  (:require [clojure.set :as set]))

(defn check-vec
  [v]
  (let [l (count v)]
    (cond
      (= (mod l 2) 1) {:odd-len l}
      (contains? (set v) 0) {:contains-zero true}
      :else (let [n (/ l 2)
                  invalid-numbers (->> v (filter #(> (abs %) n)))
                  repeated (loop [v v
                                  seen #{}]
                             (let [[n & v] v]
                               (if (contains? seen n)
                                 n
                                 (recur v (conj seen n)))))]
              (cond
                (seq invalid-numbers) {:invalid-node-number (first invalid-numbers)}
                (not (nil? repeated)) {:repeated repeated})))))

(defn- improper-sections [v above under]
  (if (<= (count v) 1)
    [above under]
    (let [[a b & v] v]
      (if (< (* a b) 0)
        (recur (concat [b] v) above under)
        (if (> (+ a b) 0)
          (recur (concat [b] v) (conj above [a b]) under)
          (recur (concat [b] v) above (conj under [a b])))))))

(defn check-proper [v]
  (let [[above under] (improper-sections (concat v [(first v)]) #{} #{})]
    (if (and (empty? above)
             (empty? under))
      nil
      {:above above
       :under under})))

(defn- sign [n]
  (if (< n 0)
    -1 1))

(defn canonicalize [v]
  (if (< (first v) 0)
    (recur (concat (rest v) [(first v)]))
    (loop [v v
           c []
           m {}
           next 1]
      (let [[n & v] v]
        (if (nil? n)
          c
          (let [absn (abs n)
                absn' (m absn)]
            (if (nil? absn')
              (let [absn' next
                    next (inc next)
                    m (assoc m absn absn')]
                (recur v (conj c (* absn' (sign n))) m next))
              (recur v (conj c (* absn' (sign n))) m next))))))))

(defn all-rotations [v]
  (for [i (range 0 (count v) 2)]
    (vec (concat (drop i v) (take i v)))))

(defn rotate-reverse [v]
  (-> (concat (rest v) [(first v)]) reverse vec))

(defn all-equivalent [v]
  (->> [v (rotate-reverse v)]
       (mapcat all-rotations)
       (map canonicalize)
       set))

(defn index-edges [v]
  (let [v (concat [(last v)] v [(first v)])]
    (loop [v v
           res {}]
      (let [[b c a & v] v]
        (if (nil? a)
          res
          (recur (concat [c] [a] v) (assoc res c [b a])))))))

(defn ascending-edges [index]
  (set (for [[neg pair] index
             :when (< neg 0)
             pos pair]
         [neg pos])))

(defn ascending-step [edge-index path]
  (let [nexts (edge-index (- 0 (first path)))]
    (for [next nexts]
      (conj path next))))

(defn- rotate-to-start-from-min [path]
  (let [m (apply min path)
        mi (.indexOf path m)]
    (concat (drop mi path) (take mi path))))

(defn sector? [path]
  (when (empty? path)
    (throw (.Exception "empty path")))
  (let [[h & t] path]
    (if (nil? t)
      nil
      (let [i (.indexOf t h)]
        (if (>= i 0)
          (->> path (take (inc i)) rotate-to-start-from-min)
          nil)))))

(defn all-sectors [edge-index]
  (loop [paths [(list 1)]
         res #{}]
    (if (empty? paths)
      res
      (let [[path & paths] paths
            sector (sector? path)]
        (if (not (nil? sector))
          (recur paths (conj res sector))
          (let [conts (ascending-step edge-index path)]
            (recur (concat paths conts) res)))))))

(defn sector-ascending-edges [sector]
  (loop [ns (concat sector [(first sector)])
         res #{}]
    (if (< (count ns) 2)
      res
      (let [[n1 n2 & ns] ns]
        (recur (concat [n2] ns) (conj res [(- 0 n2) n1]))))))

(defn check-geometric [index]
  (let [aes (ascending-edges index)
        secs (all-sectors index)
        secaes (mapcat sector-ascending-edges secs)
        counts (frequencies secaes)
        violations (->> aes (filter #(not= (counts %) 2)))]
    (if (empty? violations)
      nil
      (set violations))))

(defn slice [v a b]
  (let [v (vec (concat v v))
        ai (inc (.indexOf v a))
        v (drop ai v)
        bi (.indexOf v b)]
    (->> v (take bi) vec)))

(defn try-untwist [v n]
  (let [s1 (slice v n (- n))
        c1 (->> s1 (map abs) set)
        s2 (slice v (- n) n)
        c2 (->> s2 (map abs) set)]
    (if (empty? (set/intersection c1 c2))
      (-> (concat s1 (reverse s2)) vec canonicalize)
      nil)))

(defn untwist [v]
  (let [n (/ (count v) 2)]
    (some identity (for [i (range 1 (inc n))]
                     (try-untwist v i)))))

(defn simplify [v]
  (loop [v v]
    (let [v' (untwist v)]
      (if (nil? v')
        v
        (recur v')))))
