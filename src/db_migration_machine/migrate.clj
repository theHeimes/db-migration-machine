(ns db-migration-machine.migrate
  (:require [db-migration-machine.relational-database :as db]
            [camel-snake-kebab.core :as cs]))

; Starting with a list of tables as target
; Get data from the source table
; Keep only columns that are in the target table
; Insert the remaining source data into the target table
(defn migrate
  [config]
  (doseq [target-table (db/get-target-table-names config)]
    (let [target-keys (map keyword (map :column_name 
                                        (db/get-target-column-names config 
                                                                    (:table_name target-table))))
          source-data (db/get-source-table-data config
                                                (cs/->camelCase (:table_schema target-table))
                                                (cs/->camelCase (:table_name target-table)))
          insert-data (map #(select-keys % target-keys) source-data)]
      (prn (str "insert " (count insert-data) " records into "
                (keyword (str (:table_schema target-table) 
                                           "." 
                                           (:table_name target-table)))))
      (when (> (count insert-data) 0)
        (db/insert-target-data-pg (:target config) 
                                  (keyword (str (:table_schema target-table) 
                                             "." 
                                             (:table_name target-table)))
                                  insert-data)))))