(defproject db-migration-machine "0.1.0-SNAPSHOT"
  :description "Helps to migrate databases from one system to another."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["resources/sqljdbc4.jar" "resources/sqljdbc.jar"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [net.sourceforge.jtds/jtds "1.3.1"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [org.postgresql/postgresql "9.4-1206-jdbc4"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/core.cache "0.6.4"]
                 [commons-codec "1.8"]
                 [camel-snake-kebab "0.3.2"]
                 [com.taoensso/timbre "3.4.0"]]
  :plugins [[codox "0.8.12"]]
  :main ^:skip-aot db-migration-machine.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
