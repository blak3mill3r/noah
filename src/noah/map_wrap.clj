(ns noah.map-wrap
  "And IF you go in, should you turn left or right...
   or right-and-three-quarters? Or, maybe, not quite?
   Or go around back and sneak in from behind?
   Simple it's not, I'm afraid you will find,
   for a mind-maker-upper to make up his mind.

     from \"Oh, the Places You'll Go!\" by Dr. Seuss

   This namespace allows Clojure maps to go places where various configuration classes from Kafka Streams go.
   There's a lot of room for improvement, not everything has a conversion-fn."
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [com.rpl.specter :refer :all]
            [clojure.reflect :as ref]
            [clojure.string :as str]
            [noah.serdes]
            [noah.javanation :refer [conversion-fn]])
  (:import [org.apache.kafka.common.serialization Serdes Serde Deserializer Serializer]
           [org.apache.kafka.streams.kstream Consumed Produced Grouped Materialized]))

(declare produced consumed serialized materialized)

;;; Implementation details
(defn- withy-name->noah-keyword
  "Like withCachingEnabled -> :noah.core/caching-enabled"
  [sym]
  (keyword "noah.core" (str/replace-first (->kebab-case (name sym)) "with-" "")))

(defn- map->materialized-as [m]
  ;; TODO
  ;; WindowBytesStoreSupplier
  ;; KeyValueBytesStoreSupplier
  ;; SessionBytesStoreSupplier
  (:noah.core/store-name m))

(defn- withy-named [m] (re-find #"^with.+" (name (:name m))))

(defn- reflect-withy-methods [klass]
  (->> klass ref/reflect (select [:members ALL withy-named]) (map (juxt :name :parameter-types)) set))

(defmacro defoptionclasswrapper [class-sym & [construct-method]]
  (let [fn-name (->kebab-case class-sym)
        withy-methods (->> class-sym resolve reflect-withy-methods)
        construct-form (condp = construct-method
                         nil '(with nil nil)
                         'as `(as (map->materialized-as ~'o)))]
    `(defn ~fn-name [~'o]
       (cond (instance? ~class-sym ~'o) ~'o
             (map? ~'o) (cond-> (.. ~class-sym ~construct-form)
                          ~@(into [] cat
                                  (for [[m [p :as ps]] withy-methods :let [sym (withy-name->noah-keyword m)]]
                                    `[(get ~'o ~sym)
                                      (~(symbol (str "."(name m)))
                                       ~@(when (not-empty ps) (assert (= 1 (count ps)))
                                               (assert (some? (conversion-fn (resolve p))) (str p " needs to be defined for wrapping with noah"))
                                               [`(~(conversion-fn (resolve p)) (get ~'o ~sym))]))])))
             true (throw (ex-info ~(str "Need a map or an instance of " (.getName (ns-resolve *ns* class-sym))) {:got ~'o})))) ))

(defn map->properties [m] (->> m (reduce (fn [p [k v]] (.put p k v) p) (java.util.Properties.))))

(def materialized-store-types
  (into {}
        (select [:members ALL (selected? :flags ALL #{:enum})
                 (collect-one
                  :name (view (comp #(keyword "noah.store-type" (str %)) ->kebab-case)))
                 (view (comp symbol (partial str/join "/") (juxt :declaring-class :name)))]
                (ref/reflect org.apache.kafka.streams.kstream.Materialized$StoreType))))

;;; Wrapper definitions

;; Java classes suck at representing info
;; but the ones in the Kafka Streams API are on the less sucky side... they do at least have some consistency across classes
;; all of these have in common .withValueSerde and .withKeySerde
;; all but Materialized have a common constructor (with nil nil)
;; they have withThis and withThat instance methods which distinguish them
;; these will just be more qualified keys in our maps...
(defoptionclasswrapper Consumed)
(defoptionclasswrapper Produced)
(defoptionclasswrapper Grouped)
(defoptionclasswrapper Materialized as)

