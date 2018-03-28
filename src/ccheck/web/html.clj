(ns ccheck.web.html
  (:require
    [hiccup.page :refer [html5]]
    [ccheck.build :refer [build]]))
;


(defn head-meta [req _]
  (list
    [:title "NEM Info"]
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
;    [:meta {:name "description" :content ds}]
    ; [:meta {:property "og:site_name" :content "NEM Info"}]
    ; [:meta {:property "og:type"   :content "article"}]
    ; [:meta {:property "og:locale" :content "ru_RU"}]
    ; [:meta {:property "og:image"  :content og-img}]
    ; [:meta {:property "og:title"  :content og-tit}]
    ; [:meta {:property "og:url"    :content og-url}]
    ; [:meta {:property "og:description" :content ds}]
    [:link {:rel "shortcut icon" :href "//nem.io/favicon.ico"}]))
;

(defn head-bootstrap []
  (list
    [:link 
      { :rel "stylesheet" 
        :href "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css" 
        :integrity "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm" 
        :crossorigin "anonymous"}]
    [:script 
      { :src "https://code.jquery.com/jquery-3.2.1.slim.min.js" 
        :integrity "sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN" 
        :crossorigin "anonymous"}]
    [:script
      { :src "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js" 
        :integrity "sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q" 
        :crossorigin "anonymous"
        :defer true}]
    [:script
      { :src "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js" 
        :integrity "sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl" 
        :crossorigin "anonymous"
        :defer true}]))
;


(defn head [req {:keys [css js] :as params}]
  [:head
    (head-meta req params)
    (head-bootstrap)

    [:link {:rel "stylesheet" :href "/css/style.css"}]]) 
      
    ; (map #(css-link (inc-pfx %))
    ;   (concat ["css/main.css"] css))
    ; (map #(script (inc-pfx %))
    ;   (concat ["js/mlib.js" "js/site.js"] js))])
;


(defn render [req params & content]
  (html5
    (head req params)
    "\n"
    [:body
      [:div.container
        content
        [:div 
          [:small "" (:name build) " v" (:version build) " - " (:commit build)]]]]))
;


(defn html5-resp [content]
  {:status 200
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body (html5 content)})
;

;;.
