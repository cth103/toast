#include "util.h"
#include "toast_socket.h"
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

void
put_float(uint8_t*& p, float f)
{
	put_int16(p, static_cast<int16_t>(f * 16));
}

void
put_string(uint8_t*& p, string s)
{
	*p++ = s.length();
	strncpy(reinterpret_cast<char *>(p), s.c_str(), s.length());
	p += s.length();
}

void
put_datum(uint8_t*& p, Datum d)
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
