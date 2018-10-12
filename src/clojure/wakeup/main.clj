(ns wakeup.main
  (:import [wakeup.porcupine Porcupine]

           ;; These are required for the microphone input.
           [javax.sound.sampled AudioFormat DataLine TargetDataLine AudioSystem]))

;; Notes on audio formats:
;; Discord provides audio as `48KHz 16bit stereo signed BigEndian PCM`.
;; Porcupine requires `16KHz 16bit mono signed LittleEndian PCM` but in 512 length short-array frames (a short is two bytes).
;; GCP speech recognition requires the same as Porcupine but as byte pairs and without the 512 frames.

(defn init-porcupine []
  (Porcupine. "wake-word-engine/Porcupine/lib/common/porcupine_params.pv"
              "wake-word-engine/wake_phrase.ppn"
              0.5))

;; Adapted from: https://gist.github.com/BurkeB/ebf5f01c0d20ff6b9dc111ac427ddea8
(defn with-microphone [f]
  (let [audio-format (new AudioFormat 16000 16 1 true true)
        info (new javax.sound.sampled.DataLine$Info TargetDataLine audio-format)]

    (when-not (AudioSystem/isLineSupported info)
      (throw (Error. "AudioSystem/isLineSupported returned false")))

    (with-open [line (AudioSystem/getTargetDataLine audio-format)]
      (doto line
        (.open audio-format)
        (.start))

      (f line))))

(defn byte-pair->short [[a b]]
  (bit-or (bit-shift-left a 8) (bit-and b 0xFF)))

(defn bytes->shorts [buf]
  (->> buf
       (partition 2)
       (map byte-pair->short)
       (short-array)))

(defn -main []
  (println "Starting up wake word detector...")
  (let [porcupine (init-porcupine)]
    (with-microphone
      (fn [line]
        (let [size 1024
              buf (byte-array size)]
          (println "Listening...")
          (loop []
            (when (> (.read line buf 0 size) 0)
              (when (.processFrame porcupine (bytes->shorts buf))
                (println "Wake word detected!"))
              (recur))))))))
