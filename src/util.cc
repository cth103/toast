#include "util.h"
#include <cstring>

using std::string;

void
put_int16(uint8_t*& p, int16_t v)
{
	*p++ = v & 0xff;
	*p++ = (v & 0xff00) >> 8;
}

void
put_float(uint8_t*& p, float f)
{
	put_int16(p, static_cast<int16_t>(f * 16));
}

void
put(uint8_t*& p, string s)
{
	*p++ = s.length();
	strncpy(reinterpret_cast<char *>(p), s.c_str(), s.length());
	p += s.length();
}

void
put(uint8_t*& p, Datum d)
{
	*p++ = d.time() & 0xff;
	*p++ = (d.time() & 0xff00) >> 8;
	*p++ = (d.time() & 0xff0000) >> 16;
	*p++ = (d.time() & 0xff000000) >> 24;
	*p++ = (d.time() & 0xff00000000) >> 32;
	*p++ = (d.time() & 0xff0000000000) >> 40;
	*p++ = (d.time() & 0xff000000000000) >> 48;
	*p++ = (d.time() & 0xff00000000000000) >> 56;
	put_float(p, d.value());
}
