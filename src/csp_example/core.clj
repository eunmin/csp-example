(ns csp-example.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan put! go <!]]))

(def url1 "http://www.bbc.com/sport/football/25611509")

(def url2 "http://www.bbc.com/sport/olympics/36984887")

(defn- parse-int [s]
  (read-string s))

(defn- content-length [response]
  (parse-int (-> response :headers :content-length)))

(defn- print-result [resp1 resp2]
  (println "sum length:" (+ (content-length resp1) (content-length resp2))))

(defn sync-sum-content-length []
  (let [resp1 @(http/get url1)
        resp2 @(http/get url2)]
    (print-result resp1 resp2)))

(defn async-sum-content-length []
  (http/get url1
    (fn [resp1]
      (http/get url2
        (fn [resp2]
          (print-result resp1 resp2))))))

(defn- http-get [url]
  (let [ch (chan)]
    (http/get url #(put! ch %))
    ch))

(defn csp-sum-content-length []
  (go
    (let [resp1 (<! (http-get url1))
          resp2 (<! (http-get url2))]
      (print-result resp1 resp2))))
