#include "datum.h"
#include <string>
#include <inttypes.h>

void put_int16(uint8_t*& p, int16_t v);
void put_float(uint8_t*& p, float f);
void put_string(uint8_t*& p, std::string s);
void put_datum(uint8_t*& p, Datum d);
