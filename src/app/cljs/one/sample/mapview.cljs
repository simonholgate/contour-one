(ns one.sample.mapview
  (:require [one.sample.util :as u]
            [goog.events :as events]
            [goog.style :as style]))

(def iucn-root "http://184.73.201.235/blue")
(def species-range-root "https://eighty.cartodb.com/tiles/mol_cody")
(def cell-towers-root "https://sciencehackday-10.cartodb.com/tiles/tower_locations")
(def tide-gauges-root "https://simonholgate.cartodb.com/tiles/tide_gauges")
(def ulr5-root "https://simonholgate.cartodb.com/tiles/ulr5")

;; The forma root is odd. the suffix is repeated twice, for
;; one. The "071" is the time period of the particular tileset. We'll
;; make this dynamic next!
(def forma-root "http://formatiles.s3.amazonaws.com/tiles/forma071/forma071")


(defn carto-tiler-fn
  "Takes a root path (without trailing slash) and returns a function
  meant for the :getTileUrl option in a Google map overlay.

  The -> is called the threading macro; it actually rearranges its
  code forms before they hit the compiler. See here for examples:

  http://clojuredocs.org/clojure_core/clojure.core/-%3E"
  [root]
  {:pre [(not= "/" (last root))]} ;; contracts ftw!
  (fn [coord zoom]
    (-> (u/pathify root zoom (. coord -x) (. coord -y))
        (str ".png"))))

(def cell-towers-tile-url
  (carto-tiler-fn cell-towers-root))

(def tide-gauges-tile-url
  (carto-tiler-fn tide-gauges-root))

(def ulr5-tile-url
  (carto-tiler-fn ulr5-root))

(def species-range-tile-url
  (carto-tiler-fn species-range-root))

(defn iucn-tile-url
  "IUCN tiles don't use a .png extension, for whatever reason."
  [coord zoom]
  (u/pathify iucn-root zoom (. coord -x) (. coord -y)))


(defn forma-tile-url
  "Wacky shit with inversion of the y coordinate, etc"
  [coord zoom]
  (let [bound (dec (Math/pow 2 zoom))]
    (-> (u/pathify forma-root
                   zoom
                   (Math/abs (. coord -x))
                   (- bound (. coord -y)))
        (str ".png"))))

(def overlay-defaults
  "default overlay options; we're assuming that the tilesize is
  standard, etc."
  {:minZ 3
   :maxZ 10
   :tileSize (google.maps.Size. 256 256)})

(defn mk-overlay
  "Returns a Google Maps overlay with the supplied name,
  url-generating function and opacity."
  [name-str url-func opacity]
  (let [opts (u/clj->js
              (merge overlay-defaults
                     {:name name-str
                      :opacity opacity
                      :getTileUrl url-func}))]
    (google.maps.ImageMapType. opts)))

(def map-opts
  "Default initial map options."
  {:zoom 3
   :mapTypeId google.maps.MapTypeId.TERRAIN
   :center (google.maps.LatLng. (+  53 (/ 25 60)), 3)
   :styles [{:stylers [{:visibility "on"}
                       {:lightness 80}]}]})

(defn init-map  [element overlays]
  (let [options (u/clj->js map-opts)
        map (google.maps.Map. element options)
        overlayMapTypes (. map -overlayMapTypes)]
    (doseq [layer overlays]
      (.push overlayMapTypes layer))
    
    (.enableKeyDragZoom map)
;;    (let [dz (.getDragZoomObject map)]
;;      (events/listen dz "dragend" (js/alert "Dragged!"))
;;    )
    map))


(def *map*
  "Dynamic variable holding our map element, set to an initial value
   of nil. We don't really need to bind this to anything, but it helps
   to have a reference to it from the callback for later coding."
  nil)

(defn map-load []
  (set! *map* (init-map
               (goog.dom/getElement "map_canvas")
               [;;(mk-overlay "species-range" species-range-tile-url 0.5)
                ;;(mk-overlay "forma" forma-tile-url 1)
                ;;(mk-overlay "iucn" iucn-tile-url 0.6)
                ;;(mk-overlay "cell-towers" cell-towers-tile-url 1)
                (mk-overlay "tide-gauges" tide-gauges-tile-url 1)
                (mk-overlay "ulr-vertical-velocities" ulr5-tile-url 1)
                ])))

;; (defn addDragListener [map]
;;   (let [dz (.getDragZoomObject map)]
;;     (doto dz
;;       (GEvent/addListener 'dragend' ))))

;; Having modified the keydragzoom we need to bind it so we can query
;; the returned results from the database. How? jayq?
;;(events/listen (.getDragZoomObject *map*) "dragend" (js/alert "Dragged!"))

;;
;;(events/listen js/window "load" map-load)


;;(map-load)
;;(def dz (.getDragZoomObject *map*))
;;(google.maps.event/addListener dz "dragend" (fn [bnds] (.log js/console (pr-str bnds))))