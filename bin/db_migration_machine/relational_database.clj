(ns db-migration-machine.relational-database
    "Manages the relational database according to the configuration."
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [db-migration-machine.helper :as h])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- create-db-mssql
  "Returns a map that can be used to connect to MS SQL Server."
  [config]
  {:classname (:sql-driver-class config)
   :subprotocol (:sql-subprotocol config)
   :subname (str "//" (:sql-db-host config) ":" 
                 (:sql-db-port config) ";database=" 
                 (:sql-db-name config) ";user=" 
                 (:sql-db-user config) ";password=" 
                 (:sql-db-password config))})

(defn- create-db-mysql
  "Returns a map that can be used to connect to MySQL."
  [config]
  {:classname (:sql-driver-class config)
   :subprotocol (:sql-subprotocol config)
   :subname (str "//" (:sql-db-host config) ":"
                 (:sql-db-port config) "/"
                 (:sql-db-name config))
   :user (:sql-db-user config)
   :password (:sql-db-password config)
   :naming {:keys str/lower-case
            :fields str/upper-case}})

(defn- create-db-db2
  "Returns a map that can be used to connect to DB2."
  [config]
  {:classname (:sql-driver-class config)
   :subprotocol (:sql-subprotocol config)
   :subname (str "//" (:sql-db-host config) ":"
                 (:sql-db-port config) "/"
                 (:sql-db-name config))
   :user (:sql-db-user config)
   :password (:sql-db-password config)
   :naming {:keys str/lower-case
            :fields str/upper-case}})

(defn- create-db-pg
  "Returns a map that can be used to connect to PostgreSQL."
  [config]
  {:classname (:sql-driver-class config)
   :subprotocol (:sql-subprotocol config)
   :subname (str "//" (:sql-db-host config) ":"
                 (:sql-db-port config) "/"
                 (:sql-db-name config))
   :user (:sql-db-user config)
   :password (:sql-db-password config)
   :naming {:keys str/lower-case
            :fields str/upper-case}})

(defn- create-db-spec
  "Switch to the specific database type."
  [config]
  (case (:sql-db-type config)
    "mssql" (create-db-mssql config)
    "mysql" (create-db-mysql config)
    "db2" (create-db-db2 config)
    "postgres" (create-db-pg config)))

(defn- pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))

(def pooled-db-source
  "Reference to the source database."
  (ref nil))

(def pooled-db-target
  "Reference to the target database."
  (ref nil))

(defn set-pooled-db-source
  [db-source-config]
  (let [db-pool (pool db-source-config)]
    (dosync
      (ref-set pooled-db-source (pool (create-db-spec db-source-config))))))
(defn set-pooled-db-target
  [db-target-config]
  (let [db-pool (pool db-target-config)]
    (dosync
      (ref-set pooled-db-target (pool (create-db-spec db-target-config))))))

(defn db-connection-source [] @pooled-db-source)
(defn db-connection-target [] @pooled-db-target)

(defn get-source-data
  "Queries the source database with the given `sql-statement`."
  [sql-statement]
  (j/query (db-connection-source) [sql-statement] :identifiers h/translate-camel-case))
(defn get-target-data
  "Queries the target database with the given `sql-statement`."
  [sql-statement]
  (j/query (db-connection-target) [sql-statement]))

(defn get-sql-table-data-mssql
  [db-config schema-name table-name]
  (str "select * from " (:sql-db-name db-config) "." schema-name "." table-name ";"))

(defn- get-sql-table-data-pg
  [db-config schema-name table-name]
  (str "select * from '" schema-name "'.'" table-name "'"))

(defn get-sql-table-data
  [db-config schema-name table-name]
  (case (:sql-db-type db-config)
    "mssql" (get-sql-table-data-mssql db-config schema-name table-name)
    "postgres" (get-sql-table-data-pg db-config schema-name table-name)))

(defn get-source-table-data
  "Returns data from the source table."
  [config schema-name table-name]
  (get-source-data (get-sql-table-data (:source config) schema-name table-name)))

(defn get-target-table-data
  "Returns data from the source table."
  [config schema-name table-name]
  (get-target-data (get-sql-table-data (:target config) schema-name table-name)))

(defn- get-table-names-mssql
  "Returns the table names for a MS-SQL database"
  [db-config]
  (str "
select 
  table_catalog, 
  table_schema, 
  table_name 
from " (:sql-db-name db-config)  ".information_schema.tables 
where table_type = 'BASE TABLE';
             "))

(defn- get-table-names-pg
  "Returns the tables names for a PostgreSQL database."
  [db-config]
  "
select 
  table_catalog,
  table_schema, 
  table_name 
from information_schema.tables 
where table_schema <> 'information_schema' 
  and table_schema <> 'pg_catalog';")

(defn- get-table-names
  "Returns the tables names for a database."
  [db-config]
  (case (:sql-db-type db-config)
    "mssql" (get-table-names-mssql db-config)
    "mysql" (get-table-names-pg db-config)
    "db2" (get-table-names-pg db-config)
    "postgres" (get-table-names-pg db-config)))

(defn get-source-table-names
  "Returns the table names for the source database."
  [config]
  (get-source-data (get-table-names (:source config))))

(defn get-target-table-names
  "Returns the table names for the source database."
  [config]
  (get-target-data (get-table-names (:target config))))


(defn get-column-names-mssql
  "Returns the columns of a specific table in a MS-SQL database."
  [db-config table-name]
  (str "
select 
  table_name, 
  column_name 
from " (:sql-db-name db-config) ".information_schema.columns 
where table_name = '" table-name "';
        "))

(defn get-column-names-pg
  "Returns the columns of a specific table in a PostgreSQL database."
  [db-config table-name]
  (str "
select
  table_schema,
  table_name,
  column_name
from information_schema.columns
where table_name = '" (clojure.string/lower-case table-name) "';
        "))

(defn get-column-names
  "Returns the column names for a specific database table."
  [db-config table-name]
    (case (:sql-db-type db-config)
    "mssql" (get-column-names-mssql db-config table-name)
    "mysql" ""
    "db2" ""
    "postgres" (get-column-names-pg db-config table-name)))
  
(defn get-source-column-name 
  "Returns the column names for a specific table in the source database."
  [config table-name]
  (get-source-data (get-column-names (:source config) table-name)))

(defn get-target-column-names
  "Returns the column names for a specific table in the target database."
  [config table-name]
  (get-target-data (get-column-names (:target config) table-name)))

(defn insert-target-data-pg
  [db-config table-name data]
  (apply j/insert! (db-connection-target) table-name data))