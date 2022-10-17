(ns whathaveidone.github-test
  (:require [clojure.test :refer [deftest testing is]]
            [whathaveidone.github :as gh]))

(deftest get-events-test
  (testing "simple get events"
    (let [events [{"created_at" "2022-10-10T07:24:24Z"}]]
      (with-redefs [gh/*event-retriever* (constantly (future events))]
        (is (= (gh/get-events 1) events))))))

(deftest get-events-by-date-test
  (testing "single page"
    (let [events [{"created_at" "2022-10-10T07:24:24Z"}
                  {"created_at" "2022-11-10T07:24:24Z"}]]
      (with-redefs [gh/*event-retriever* (constantly (future events))]
        (is (= (gh/get-events-by-date "2022-10-10")
               [{"created_at" "2022-10-10T07:24:24Z"}])))))

  (testing "pagination"
    (let [events [[{"created_at" "2022-10-10T07:24:24Z"} {"created_at" "2022-10-10T07:25:24Z"}]
                  [{"created_at" "2022-10-10T07:26:24Z"} {"created_at" "2022-10-10T07:27:24Z"}]
                  [{"created_at" "2022-10-10T07:28:24Z"} {"created_at" "2022-10-11T07:25:24Z"}] ; next date
                  [{"created_at" "2022-10-11T07:24:24Z"} {"created_at" "2022-10-11T07:25:24Z"}]]]
      (with-redefs [gh/get-events (fn [page] (get events (dec page)))]
        (is (= (gh/get-events-by-date "2022-10-10")
               [{"created_at" "2022-10-10T07:24:24Z"}
                {"created_at" "2022-10-10T07:25:24Z"}
                {"created_at" "2022-10-10T07:26:24Z"}
                {"created_at" "2022-10-10T07:27:24Z"}
                {"created_at" "2022-10-10T07:28:24Z"}]))))))
