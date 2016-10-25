DEVICE := attiny85

install:	fw.hex
		avrdude -c usbtiny -p attiny85 -C avrdude.conf -U flash:w:$<

clean:;		rm -f fw.hex fw.elf

fw.hex:		fw.elf
		avr-objcopy -R .eeprom -O ihex $< $@
		avr-size --format=avr --mcu=$(DEVICE) $<

fw.elf:		fw.c
		avr-gcc -Wall -Os -o $@ $< -mmcu=$(DEVICE) -DF_CPU=8000000
