ROOT=./ESP8266_NONOS_SDK/bin

# Ground GPIO0 then pull RST low before flashing

esptool -v -cb 115200 \
        -ca 0x00000 -cf $ROOT/at/noboot/eagle.flash.bin \
        -ca 0x10000 -cf $ROOT/at/noboot/eagle.irom0text.bin \
        -ca 0x7e000 -cf $ROOT/blank.bin \
        -ca 0xfc000 -cf $ROOT/esp_init_data_default.bin \
        -ca 0xfe000 -cf $ROOT/blank.bin \

