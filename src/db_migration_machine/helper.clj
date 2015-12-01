(ns db-migration-machine.helper
  (:require [clojure.edn :as edn]))

(defn load-config
  "Given a filename, load & return a config file" 
  [filename]
  (edn/read-string (slurp filename)))

(defn translate-camel-case
  "Converts a camel case identifier into an underscore identifier."
  [camel-case]
  (let [charseq (seq camel-case)]
    (loop [s charseq
           last-was-lowercase false
           result-seq []]
      (if (< (count s) 1)
        (clojure.string/join "" result-seq)
        (recur (rest s)
               (java.lang.Character/isLowerCase (first s))
               (conj result-seq 
                     (when (and (java.lang.Character/isUpperCase (first s))
                                last-was-lowercase)
                       "_")
                     (clojure.string/lower-case (str (first s)))))))))