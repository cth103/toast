ARDUINO=/opt/arduino-1.6.12

BUILD=/tmp/arduino_build_$$
mkdir -p $BUILD

$ARDUINO/arduino-builder -compile -logger=machine -hardware $ARDUINO/hardware -hardware $HOME/.arduino15/packages \
			 -tools $ARDUINO/tools-builder -tools $ARDUINO/hardware/tools/avr -tools $HOME/.arduino15/packages \
			 -built-in-libraries $ARDUINO/libraries -libraries $HOME/Arduino/libraries \
			 -fqbn=adafruit:avr:trinket3 -ide-version=10612 -build-path $BUILD \
			 -warnings=none -prefs=build.warn_data_percentage=75 -prefs=runtime.tools.avrdude.path=$ARDUINDO/hardware/tools/avr \
			 -prefs=runtime.tools.avr-gcc.path=$ARDUINO/hardware/tools/avr -verbose \
			 fw.ino

mv $BUILD/fw.ino.hex .
