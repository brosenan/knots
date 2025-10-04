(ns knots.core)

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
    (prn counts)
    (if (empty? violations)
      nil
      (set violations))))