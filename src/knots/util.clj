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

(defn- calc-compr [bindings expr vars output]
  (let [sym-vals (gensym)]
    (if (empty? bindings)
      `(map (fn [~sym-vals]
              (let [~vars ~sym-vals]
                ~expr)) ~output)
      (let [[var val & bindings] bindings]
        (cond
          (nil? val) (throw (Exception. (str "Missing expression for " var " in compr")))
          (double-deref? val) (let [val (-> val second second)]
                                (calc-compr bindings expr (conj vars var)
                                            `(mapcat (fn [~sym-vals]
                                                       (map #(conj ~sym-vals %) ~val)) ~output)))
          :else (calc-compr bindings expr (conj vars var)
                            `(map (fn [~sym-vals]
                                    (let [~vars ~sym-vals]
                                      (conj ~sym-vals ~val))) ~output)))))))

(defmacro compr [bindings expr]
  (calc-compr bindings expr [] `(list [])))