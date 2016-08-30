(ns re-frame-boiler.components.idresolver.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [re-frame-boiler.components.idresolver.events]
            [re-frame-boiler.components.idresolver.subs]))

(def ex "CG9151, FBgn0000099, CG3629, TfIIB, Mad, CG1775, CG2262, TWIST_DROME, tinman, runt, E2f, CG8817, FBgn0010433, CG9786, CG1034, ftz, FBgn0024250, FBgn0001251, tll, CG1374, CG33473, ato, so, CG16738, tramtrack,  CG2328, gt")

(def separators (set ".,; "))

(defn splitter
  "Splits a string on any one of a set of strings."
  [string]
  (->> (clojure.string/split string (re-pattern (str "[" (reduce str separators) "\\r?\\n]")))
       (remove nil?)
       (remove #(= "" %))))

(defn has-separator?
  "Returns true if a string contains any one of a set of strings."
  [str]
  (some? (some separators str)))

(defn controls []
  (let [results (subscribe [:idresolver/results])]
    (fn []
      [:div.btn-toolbar
       [:button.btn.btn-primary.btn-raised
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (dispatch [:idresolver/clear]))} "Clear"]
       [:button.btn.btn-success.btn-raised
        {:class    (if (nil? @results) "disabled")
         :on-click (fn [] (if (some? @results) (dispatch [:idresolver/analyse])))} "Analyse"]])))

(defn input-box []
  (let [val (reagent/atom nil)]
    (fn []
      [:input.freeform
       {:type        "text"
        :placeholder "Identifiers..."
        :value       @val
        :on-change   (fn [e]
                       (let [input (.. e -target -value)]
                         (if (has-separator? input)
                           (do
                             (reset! val "")
                             (dispatch [:idresolver/resolve (splitter input)]))
                           (reset! val input))))}])))


(defn input-item-duplicate []
  (fn [[oid data]]
    [:span.dropdown
     [:span.dropdown-toggle
      {:type        "button"
       :data-toggle "dropdown"}
      (:input data)
      [:span.caret]]
     (into [:ul.dropdown-menu]
           (map (fn [result]
                  [:li
                   {:on-click (fn [] (dispatch [:idresolver/resolve-duplicate
                                                (:input data)
                                                result]))}
                   [:a (-> result :summary :symbol)]]) (:matches data)))]))

(defn input-item [i]
  (let [result (subscribe [:idresolver/results-item (:input i)])]
    (fn [i]
      (let [class (if (empty? @result)
                    "inactive"
                    (name (:status (second (first @result)))))]
        [:div.id-resolver-item {:class class}
         (case (:status (second (first @result)))
           :MATCH [:i.fa.fa-check.fa-1x.fa-fw]
           :UNRESOLVED [:i.fa.fa-times]
           :DUPLICATE [:i.fa.fa-clone]
           :TYPE_CONVERTED [:i.fa.fa-random]
           :OTHER [:i.fa.fa-exclamation]
           [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw])
         [:span.pad-left-5

          (if (= :DUPLICATE (:status (second (first @result))))
            [input-item-duplicate (first @result)]
            (:input i))]]))))

(defn input-items []
  (let [bank (subscribe [:idresolver/bank])]
    (fn []
      (into [:div.input-items]
            (map (fn [i]
                   ^{:key (:input i)} [input-item i]) (reverse @bank))))))

(defn input-div []
  (fn []
    [:div.panel.panel-default
     [:div.panel-body
      [:div.idresolver.form-control
       [input-items]
       [input-box]]]]))

(defn stats []
  (let [bank       (subscribe [:idresolver/bank])
        no-matches (subscribe [:idresolver/results-no-matches])
        matches    (subscribe [:idresolver/results-matches])
        duplicates (subscribe [:idresolver/results-duplicates])
        other      (subscribe [:idresolver/results-other])]
    (fn []
      [:div.panel.panel-default
       [:div.panel-body
        [:div.row
         [:div.col-md-4 [:h4
                         (str "Total Identifiers: " (count @bank))]]
         [:div.col-md-2 [:h4
                         [:i.fa.fa-check.fa-1x.fa-fw]
                         (str "Matches: " (count @matches))]]
         [:div.col-md-2 [:h4
                         [:i.fa.fa-clone]
                         (str "Duplicates: " (count @duplicates))]]
         [:div.col-md-2 [:h4
                         [:i.fa.fa-times]
                         (str "Not Found: " (count @no-matches))]]
         [:div.col-md-2 [:h4
                         [:i.fa.fa-exclamation]
                         (str "Other: " (count @other))]]]
        [:div [controls]]]]

      #_[:div
         [:ul
          [:li (str "entered" (count @bank))]
          [:li (str "matches" (count @matches))]
          [:li (str "no matches" (count @no-matches))]
          [:li (str "duplicates" (count @duplicates))]]]

      )))

(defn results []
  (let [results (subscribe [:idresolver/results])]
    (fn []
      [:div (json-html/edn->hiccup @results)])))

(defn spinner []
  (let [resolving? (subscribe [:idresolver/resolving?])]
    (fn []
      (if @resolving?
        [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
        [:i.fa.fa-check]))))

(defn main []
  (fn []
    [:div.container
     [:h1 "List Upload"]
     [:a {:on-click (fn [] (dispatch [:idresolver/resolve (splitter ex)]))} "Example"]
     [input-div]
     [stats]
     ;[results]
     ]))