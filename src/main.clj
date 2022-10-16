(ns main
  (:require [clojure.pprint :as pprint]
            [cljc.java-time.local-date :as ld]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def ^:dynamic *max-num-pages* 5)
(def ^:dynamic *event-retriever* http/request)

(defn env [s] (System/getenv s))

(def per-page 100)
(def username "anderseknert")
(def token (env "GITHUB_TOKEN"))
(def url (str "https://api.github.com/users/" username "/events?per_page=" per-page))
(def headers
  {"Accept" "application/vnd.github+json"
   "Authorization" (str "Bearer " token)})

(defn get-events
  ([]
   (get-events 1))
  ([page]
   @(*event-retriever*
      {:url (str url "?page=" page) :method :get :headers headers}
      (fn [{:keys [status body error]}]
        (if (or error (not= 200 status))
          (println "error retrieving events:" error)
          (json/parse-string body))))))

(def boring-events #{"CreateEvent" "DeleteEvent" "PushEvent" "WatchEvent"})

(defn boring? [{:strs [action type]}]
  (or (contains? boring-events type)
      (and (= type "PullRequestEvent")
           (not (contains? #{"closed" "opened"} action)))))

(defn format-event [event]
  (let [type (get event "type")]
    (cond-> event
            (= type "IssuesEvent")  (assoc "action" (get-in event ["payload" "action"]))
            :always                 (select-keys ["type" "created_at" "repo" "action"]))))

(defn date-matches? [date event]
  (when-let [created-at (get event "created_at")]
    (str/starts-with? created-at date)))

(defn printr [val]
  (println val)
  val)

(defn process-events [events]
  (let [keywordize (partial cske/transform-keys csk/->kebab-case-keyword)]
    (->> events
         (remove boring?)
         (mapv format-event)
         (mapv keywordize)
         (mapv #(if (:repo %) (assoc % :repo (get-in % [:repo :name])) %)))))

(defn get-events-by-date [date]
  (loop [page 1 events []]
    (let [page-events (get-events page)
          page-date-events (filter (partial date-matches? date) page-events)
          events (concat events page-date-events)]
      (if (or (> page *max-num-pages*)
              (not (-> (last page-events)
                       (date-matches? date))))
        (vec events)
        (recur (inc page) events)))))

(defn pluralize [word value]
  (if (> value 1)
    (str word "s")
    word))

(defn ->string [[type value]]
  (case type
    "IssuesEvent"                   (format "created %d %s"      value (pluralize "issue" value))
    "IssueCommentEvent"             (format "commented on %d %s" value (pluralize "issue" value))
    ;"PullRequestEvent"       ()
    "PullRequestReviewEvent"        (format "reviewed %d %s"     value (pluralize "pull request" value))
    "PullRequestReviewCommentEvent" (format "commented in %d %s" value (pluralize "pull request" value))

    (println "unknown event type" type "with value" value)))

(defn -main []
  (let [today (str (ld/now))]
    (->> (get-events-by-date today)
         process-events
         (map :type)
         frequencies
         (map ->string)
         (str/join "\n")
         println)))
