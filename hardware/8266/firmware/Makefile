ARDUINO = /opt/arduino-1.6.12
SSID = my_wireless_ssid
PASSWORD = my_wireless_password

AVR_BIN = $(ARDUINO)/hardware/tools/avr/bin
AVR_GCC = $(AVR_BIN)/avr-gcc
AVR_GXX = $(AVR_BIN)/avr-g++
AVR_AR = $(AVR_BIN)/avr-gcc-ar
AVR_OBJCOPY = $(AVR_BIN)/avr-objcopy
AVRDUDE = $(AVR_BIN)/avrdude

AVR_FLAGS = -g -Os -w -std=gnu++11 -fpermissive -fno-exceptions -ffunction-sections -fdata-sections -fno-threadsafe-statics
AVR_FLAGS += -MMD -flto -mmcu=attiny85 -DF_CPU=8000000L -DARDUINO=10612 -DARDUINO_AVR_TRINKET3 -DARDUINO_ARCH_AVR -DSSID="\"$(SSID)\"" -DPASS="\"$(PASSWORD)\""
AVR_INCLUDES = -I$(ARDUINO)/hardware/arduino/avr/cores/arduino -I$(ARDUINO)/hardware/arduino/avr/libraries/SoftwareSerial/src -Itiny8 -IOneWire -IDallasTemperature

CORE_OBJS =  wiring_pulse.S.o WInterrupts.o hooks.o wiring.o wiring_analog.o wiring_digital.o wiring_pulse.o
CORE_OBJS += wiring_shift.o CDC.o HardwareSerial.o HardwareSerial0.o HardwareSerial1.o HardwareSerial2.o HardwareSerial3.o
CORE_OBJS += IPAddress.o PluggableUSB.o Print.o Stream.o Tone.o USBCore.o WMath.o WString.o abi.o main.o new.o

fw.ino.hex:	fw.ino.elf
	$(AVR_OBJCOPY) -O ihex -R .eeprom $< $@

install:	fw.ino.hex
		$(AVRDUDE) -c usbtiny -p attiny85 -C avrdude.conf -U flash:w:fw.ino.hex

clean:;	rm -f *.o *.a *.d *.hex *.elf

fw.ino.elf:	fw.o SoftwareSerial.o DallasTemperature.o OneWire.o core.a
	$(AVR_GCC) -w -Os -flto -fuse-linker-plugin -Wl,--gc-sections -mmcu=attiny85 -o $@ $^

VPATH = $(ARDUINO)/hardware/arduino/avr/cores/arduino/ $(ARDUINO)/hardware/arduino/avr/libraries/SoftwareSerial/src OneWire DallasTemperature

%.o:	%.cpp
	$(AVR_GXX) -c $(AVR_FLAGS) $(AVR_INCLUDES) -o $@ $<

%.o:	%.c
	$(AVR_GCC) -c $(AVR_FLAGS) $(AVR_INCLUDES) -o $@ $<

%.S.o:	%.S
	$(AVR_GCC) -c $(AVR_FLAGS) -x assembler-with-cpp -o $@ $<

core.a: $(CORE_OBJS)
	$(AVR_AR) rcs $@ $(CORE_OBJS)
