(ns csp-example.core
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan put! go <! <!!]]))

(def url1 "http://www.bbc.com/sport/football/25611509")

(def url2 "http://www.bbc.com/sport/olympics/36984887")

(defn- print-thread-name []
  (println (.getName (Thread/currentThread))))

(defn- parse-int [s]
  (read-string s))

(defn- content-length [response]
  (parse-int (-> response :headers :content-length)))

;; 시나리오 :
;;   url1 응답 본문 크기가 10 이상이면 url2 본문 크기를 더해서 출력한다.
;;   10 이하이면 url1 응답 본문 크기를 출력한다.

;; 1. 동기 버전 - 직관적이지만 비동기 처리보다 상대적으로 비효율적으로 자원을 활용한다.
(defn- sync-http-get [url]
  (print-thread-name)
  @(http/get url1))

(defn sync-sum-content-length []
  (println "result:"
    (let [resp1 (sync-http-get url1)
          resp1-len (content-length resp1)]
      (if (> resp1-len 10)
        (+ resp1-len (content-length (sync-http-get url2)))
        resp1-len))))

;; 2. 콜백을 이용한 비동기 버전 - 콜백 때문에 흐름이 눈에 들어오지 않는다.
(defn async-sum-content-length []
  (http/get url1
    (fn [resp1]
      (print-thread-name)
      (let [resp1-len (content-length resp1)]
        (if (> resp1-len 10)
          (http/get url2
            (fn [resp2]
              (print-thread-name)
              (println "result:" (+ resp1-len (content-length resp2)))))
          (println "result:" resp1-len))))))

;; 3. CSP를 이용한 비동기 버전 - 직관적인 흐름(마치 동기적으로 처리되는 형태)과 비동기적 장점을 모두 가진다.
(defn- csp-http-get [url]
  (let [ch (chan)]
    (print-thread-name)
    (http/get url #(put! ch %))
    ch))

(defn csp-sum-content-length []
  (println "result:"
    (<!!
      (go
        (let [resp1 (<! (csp-http-get url1))
              resp1-len (content-length resp1)]
          (if (> resp1-len 10)
            (+ resp1-len (content-length (<! (csp-http-get url2))))
            resp1-len))))))
