CC = xtensa-lx106-elf-gcc
CFLAGS = -I. -mlongcalls -Iinclude -DICACHE_FLASH
LDLIBS = -nostdlib -Wl,--start-group -lmain -lnet80211 -lwpa -llwip -lpp -lphy -lc -Wl,--end-group -lgcc
LDFLAGS = -Teagle.app.v6.ld

fw-0x00000.bin: fw
	esptool.py elf2image $^

fw: fw.o driver/uart.o driver/ds18b20.o driver/dhtxx_lib.o driver/gpiolib.o driver/i2c_master.o

fw.o: fw.c
driver/uart.o: driver/uart.c
driver/ds18b20.o: driver/ds18b20.c
driver/dhtxx_lib.o: driver/dhtxx_lib.c
driver/gpiolib.o: driver/gpiolib.c
driver/i2c_master.o: driver/i2c_master.c

flash: fw-0x00000.bin
	esptool.py write_flash 0 fw-0x00000.bin 0x10000 fw-0x10000.bin

clean:
	rm -f fw fw.o fw-0x00000.bin fw-0x10000.bin driver/uart.o driver/ds18b20.o fw.o driver/dhtxx_lib.o driver/gpiolib.o driver/i2c_master.o
