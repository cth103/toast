#include "util.h"
#include "toast_socket.h"
#include "config.h"
#ifdef TOAST_HAVE_WIRINGPI
#include <wiringPi.h>
#endif
#include <cstring>

using std::string;
using std::shared_ptr;
using std::pair;

void
put_int16(uint8_t*& p, int16_t v)
{
	*p++ = v & 0xff;
	*p++ = (v & 0xff00) >> 8;
}

int16_t
get_int16(uint8_t*& p)
{
	int16_t v = *p++;
	v |= (*p++) << 8;
	return v;
}

void
put_float(uint8_t*& p, float f)
{
	put_int16(p, static_cast<int16_t>(f * 16));
}

float
get_float(uint8_t*& p)
{
	return get_int16(p) / 16.0;
}

void
put_string(uint8_t*& p, string s)
{
	*p++ = s.length();
	strncpy(reinterpret_cast<char *>(p), s.c_str(), s.length());
	p += s.length();
}

string
get_string(uint8_t*& p)
{
	int const N = *p++;
	string s;
	for (int i = 0; i < N; ++i) {
		s += *p++;
	}
	return s;
}

int64_t
get_int64(uint8_t*& p)
{
	int64_t o = *p++;
	o |= (*p++ << 8);
	o |= (*p++ << 16);
	o |= (*p++ << 24);
	o |= ((int64_t) *p++) << 32;
	o |= ((int64_t) *p++) << 40;
	o |= ((int64_t) *p++) << 48;
	o |= ((int64_t) *p++) << 56;
	return o;
}

void
put_int64(uint8_t*& p, int64_t v)
{
	*p++ = v & 0xff;
	*p++ = (v & 0xff00) >> 8;
	*p++ = (v & 0xff0000) >> 16;
	*p++ = (v & 0xff000000) >> 24;
	*p++ = (v & 0xff00000000) >> 32;
	*p++ = (v & 0xff0000000000) >> 40;
	*p++ = (v & 0xff000000000000) >> 48;
	*p++ = (v & 0xff00000000000000) >> 56;
}

void
put_datum(uint8_t*& p, Datum d)
{
	put_int64(p, d.time());
	put_float(p, d.value());
}

void
write_with_length(shared_ptr<Socket> socket, uint8_t const * data, int length)
{
	uint8_t len[4];
	len[0] = (length & 0xff000000) >> 24;
	len[1] = (length & 0x00ff0000) >> 16;
	len[2] = (length & 0x0000ff00) >> 8;
	len[3] = (length & 0x000000ff) >> 0;
	socket->write(len, 4);
	socket->write(data, length);
}

pair<shared_ptr<uint8_t[]>, uint32_t>
read_with_length(shared_ptr<Socket> socket)
{
	uint8_t len[4];
	if (socket->read(len, 4) != 4) {
		return make_pair(shared_ptr<uint8_t[]>(), 0);
	}

	uint32_t length = (len[0] << 24) | (len[1] << 16) | (len[2] << 8) | len[3];
	shared_ptr<uint8_t[]> data(new uint8_t[length]);
	if (socket->read(data.get(), length) != static_cast<int>(length)) {
		return make_pair(shared_ptr<uint8_t[]>(), 0);
	}

	return make_pair(data, length);
}

#ifdef TOAST_HAVE_WIRINGPI
void
set_boiler_on(bool s)
{
	digitalWrite(Config::instance()->boiler_gpio(), s ? HIGH : LOW);
}
#else
void
set_boiler_on(bool)
{

}
#endif

struct tm
now()
{
	time_t const t = time(0);
	return *localtime(&t);
}
