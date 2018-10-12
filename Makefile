WAKE_PHRASE := hey porcupine

wake-word-engine: wake-word-engine/Porcupine wake-word-engine/wake_phrase.ppn wake-word-engine/jni/libpv_porcupine.so src/java/wakeup/porcupine/Porcupine.class

wake-word-engine/Porcupine:
	mkdir -p wake-word-engine
	cd wake-word-engine && git clone git@github.com:Picovoice/Porcupine.git

wake-word-engine/wake_phrase.ppn: wake-word-engine/Porcupine
	cd wake-word-engine/Porcupine && tools/optimizer/linux/x86_64/pv_porcupine_optimizer -r resources/ -w "$(WAKE_PHRASE)" -p linux -o ../
	mv "wake-word-engine/$(WAKE_PHRASE)_linux.ppn" wake-word-engine/wake_phrase.ppn

src/java/wakeup/porcupine/Porcupine.class wake-word-engine/jni/wakeup_porcupine_Porcupine.h: src/java/wakeup/porcupine/Porcupine.java
	mkdir -p wake-word-engine/jni
	javac -h wake-word-engine/jni src/java/wakeup/porcupine/Porcupine.java

wake-word-engine/jni/libpv_porcupine.so: wake-word-engine/jni/wakeup_porcupine_Porcupine.h src/c/porcupine.c
	gcc -shared -O3 \
		-I/usr/include \
		-I/usr/lib/jvm/default/include \
		-I/usr/lib/jvm/default/include/linux \
		-Iwake-word-engine/Porcupine/include \
		-Iwake-word-engine/jni \
		src/c/porcupine.c \
		wake-word-engine/Porcupine/lib/linux/x86_64/libpv_porcupine.a \
		-o wake-word-engine/jni/libpv_porcupine.so
