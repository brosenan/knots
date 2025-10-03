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
