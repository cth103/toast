#include "datum.h"
#include <string>
#include <memory>
#include <inttypes.h>

class Socket;

void put_int16(uint8_t*& p, int16_t v);
void put_float(uint8_t*& p, float f);
float get_float(uint8_t*& p);
void put_string(uint8_t*& p, std::string s);
std::string get_string(uint8_t*& p);
void put_datum(uint8_t*& p, Datum d);
int64_t get_int64(uint8_t*& p);
void put_int64(uint8_t*& p, int64_t v);
void write_with_length(std::shared_ptr<Socket> socket, uint8_t const * data, int length);
std::pair<std::shared_ptr<uint8_t[]>, uint32_t> read_with_length(std::shared_ptr<Socket> socket);

void set_boiler_on(bool s);
