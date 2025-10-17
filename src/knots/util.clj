(ns knots.util)

(defn- accum' [f init coll]
  (if (empty? coll)
    nil
    (let [next (f (first coll) init)]
      (lazy-seq (cons next (accum' f next (rest coll)))))))

(defn accum [f init coll]
  (if (empty? coll)
    (list init)
    (accum' f init coll)))

(defn- double-deref? [x]
  (and (seq? x)
       (-> x first (= `deref))
       (-> x second seq?)
       (-> x second first (= `deref))))

(defn- calc-compr [bindings expr vars vals input]
  (let [sym-vals (gensym)]
    (if (empty? bindings)
      `(map (fn [~sym-vals]
              (let [~vars ~sym-vals]
                ~expr)) ~input)
      (let [[var val & bindings] bindings]
        (cond
          (nil? val) (throw (Exception. (str "Missing expression for " var " in compr")))
          (= var :when) (calc-compr bindings expr vars vals
                                    `(filter (fn [~vars] ~val) ~input))
          (double-deref? val) (let [val (-> val second second)]
                                (calc-compr bindings expr (conj vars var) (conj vals nil)
                                            `(mapcat (fn [~sym-vals]
                                                       (let [~vars ~sym-vals]
                                                         (map #(conj ~sym-vals %) ~val))) ~input)))
          (not= (.indexOf vars var) -1) (let [pos (.indexOf vars var)
                                              sym-state (gensym)]
                                          (calc-compr bindings expr vars vals
                                                      `(accum (fn [~sym-vals ~sym-state]
                                                                (let [~vars ~sym-vals
                                                                      ~var (nth ~sym-state ~pos)]
                                                                  (assoc ~sym-vals ~pos ~val))) ~vals ~input)))
          :else (calc-compr bindings expr (conj vars var) (conj vals val)
                            `(map (fn [~sym-vals]
                                    (let [~vars ~sym-vals]
                                      (conj ~sym-vals ~val))) ~input)))))))

(defmacro compr [bindings expr]
  (calc-compr bindings expr [] [] `(list [])))