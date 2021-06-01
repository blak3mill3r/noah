(ns hooks.noah
  (:refer-clojure :exclude [with-bindings])
  (:require
   [clj-kondo.hooks-api :as api]))

(defn- with-bindings
  [bindings body]
  (api/list-node
   (list*
    (api/token-node 'let)
    bindings
    body)))

(defn deftransformer
  [{:keys [node]}]
  (let [[name & more] (rest (:children node))
        [_docstring store-names & more]
        (if (api/string-node? (first more))
          more
          (cons nil more))
        [schedules more] (loop [schedules '()
                                more more]
                           (if (and (api/keyword-node? (first more))
                                    (= (api/sexpr (first more)) :schedule))
                             (recur (concat schedules (map (partial nth more) (range 1 4)))
                                    (nthrest more 4))
                             [schedules more]))
        [init more] (if (and (api/keyword-node? (first more))
                             (= (api/sexpr (first more)) :init))
                      [(second more) (nthrest more 2)]
                      [(api/token-node nil) more])
        [close [params & body]] (if (and (api/keyword-node? (first more))
                                         (= (api/sexpr (first more)) :close))
                                  [(second more) (nthrest more 2)]
                                  [(api/token-node nil) more])
        with-stores (fn [& body]
                      (with-bindings
                        (api/vector-node
                         (interleave (:children store-names) (repeat (api/token-node nil))))
                        body))
        new-node (api/list-node
                  (list
                   (api/token-node 'def)
                   name
                   (api/list-node
                    (list
                     (api/token-node 'do)
                     init
                     close
                     (apply with-stores
                            schedules
                            (api/list-node
                             (list*
                              (api/token-node 'fn)
                              (api/token-node (symbol (str (api/sexpr name) "-transform")))
                              params
                              body)))
                     schedules
                     (api/token-node nil)))))]
    {:node new-node}))
