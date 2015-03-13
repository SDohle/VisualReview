;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Copyright 2015 Xebia B.V.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;  http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns com.xebia.visualreview.image
  (:require [taoensso.timbre :as timbre]
            [com.xebia.visualreview.io :as io]
            [com.xebia.visualreview.persistence :as p]
            [com.xebia.visualreview.service-util :as sutil])
  (:import (java.util Calendar)))

(defn insert-image!
  "Stores an image. Returns the image ID.
  Throws a service-exception when the image could not be stored."
  [conn file]
  {:pre [conn file]}
  (sutil/assume (.canRead file) (str "Cannot store image: file cannot be read") "screenshot-cannot-store-on-fs-cannot-read")
  (let [now (Calendar/getInstance)
       directory (str (.get now Calendar/YEAR) "/" (.get now Calendar/MONTH) "/" (.get now Calendar/DAY_OF_MONTH) "/" (.get now Calendar/HOUR_OF_DAY))
       image-id (sutil/attempt (p/insert-image! conn directory) "Could not record new image in the database: %s" "img-cannot-store-on-db")]
    (sutil/attempt (io/store-image! file directory image-id) (str "Could not store image with id " image-id " on filesystem: %s") "img-cannot-store-on-fs")
    (timbre/debug (str "created image with id " image-id))
  image-id))

(defn get-image-path
  [conn image-id]
  (p/get-image-path conn image-id))
