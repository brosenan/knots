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
