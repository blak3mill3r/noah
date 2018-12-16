(ns noah.topology
  "Experimental one-way conversions from TopologyDescription instances to Clojure data.
  In principle it should be possible to take such Clojure data and produce a Topology as well...")

;; wall hacks (invoke private instance methods)
;; https://gist.github.com/sunng87/13700d3356d5514d35ad
(defn invoke-private-method [obj fn-name-string & args]
  (let [m (first (filter (fn [x] (.. x getName (equals fn-name-string)))
                         (.. obj getClass getDeclaredMethods)))]
    (. m (setAccessible true))
    (. m (invoke obj args))))

(defn private-field [obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))

(defn node->clj
  [include-raw? n]
  (let [sink?   (isa? (type n) org.apache.kafka.streams.TopologyDescription$Sink)
        source? (isa? (type n) org.apache.kafka.streams.TopologyDescription$Source)]
    (cond-> {:successors   (into #{} (map (memfn name)) (.successors n))
             :predecessors (into #{} (map (memfn name)) (.predecessors n))}
      include-raw? (assoc :obj n)
      source?      (assoc :source (.topics n))
      sink?        (assoc :sink (let [tne (private-field n "topicNameExtractor")]
                                  (if (isa? (type tne) org.apache.kafka.streams.processor.internals.StaticTopicNameExtractor)
                                    (.topic n)
                                    tne))))))

(defn describe
  "Return a representation of a Topology as Clojure data. If `include-raw?` is true then each node-map will also include the Java instance under the key `:obj`"
  ([t] (describe t false))
  ([t include-raw?]
   (into {}
         (map
          (juxt (memfn id)
                (comp #(->> % (into {} (map (juxt (memfn name) (partial node->clj include-raw?)))))
                      (memfn nodes))))
         (.subtopologies (.describe t)))))
