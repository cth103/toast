CC = xtensa-lx106-elf-gcc
CFLAGS = -I. -mlongcalls -Iinclude -DICACHE_FLASH
LDLIBS = -nostdlib -Wl,--start-group -lmain -lnet80211 -lwpa -llwip -lpp -lphy -lc -Wl,--end-group -lgcc
LDFLAGS = -Teagle.app.v6.ld

fw-0x00000.bin: fw
	esptool.py elf2image $^

fw: fw.o driver/uart.o driver/ds18b20.o

fw.o: fw.c
driver/uart.o: driver/uart.c
driver/ds18b20.o: driver/ds18b20.c

flash: fw-0x00000.bin
	esptool.py write_flash 0 fw-0x00000.bin 0x10000 fw-0x10000.bin

clean:
	rm -f fw fw.o fw-0x00000.bin fw-0x10000.bin
