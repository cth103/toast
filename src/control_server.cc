#include "control_server.h"
#include "state.h"
#include "toast_socket.h"
#include "types.h"
#include "log.h"
#include <iostream>

using std::list;
using std::cout;
using std::string;
using std::shared_ptr;
using std::runtime_error;

ControlServer::ControlServer(State* state, int port, int timeout)
	: Server(port, timeout)
	, _state(state)
{

}

list<string>
ControlServer::zones_from_message(uint8_t*& p)
{
	int N = *p++;
	list<string> z;
	for (int i = 0; i < N; ++i) {
		int const L = *p++;
		string n;
		for (int j = 0; j < L; ++j) {
			n += *p++;
		}
		z.push_back(n);
	}
	return z;
}

void
ControlServer::write(shared_ptr<Socket> socket, uint8_t const * data, int length)
{
	uint8_t len[4];
	len[0] = (length & 0xff000000) >> 24;
	len[1] = (length & 0x00ff0000) >> 16;
	len[2] = (length & 0x0000ff00) >> 8;
	len[3] = (length & 0x000000ff) >> 0;
	socket->write(len, 4);
	socket->write(data, length);
}

void
ControlServer::handle(shared_ptr<Socket> socket)
try
{
	uint8_t len[4];
	if (socket->read(len, 4) != 4) {
		return;
	}

	uint32_t length = (len[0] << 24) | (len[1] << 16) | (len[2] << 8) | len[3];
	LOG("Client sending %1 bytes", length);
	shared_ptr<uint8_t[]> data(new uint8_t[length]);
	if (socket->read(data.get(), length) != static_cast<int>(length)) {
		return;
	}

	uint8_t* p = data.get();
	LOG("Opcode is %1", static_cast<int>(p[0]));
	if (p[0] == OP_SEND_BASIC) {
		auto s = _state->get(false, OP_ALL);
		write(socket, s.first.get(), s.second);
	} else if (p[0] == OP_SEND_ALL) {
		auto s = _state->get(true, OP_ALL);
		write(socket, s.first.get(), s.second);
	} else if (p[0] == OP_HEATING_ENABLED) {
		_state->set_heating_enabled(p[1]);
		auto s = _state->get(false, OP_HEATING_ENABLED);
		write(socket, s.first.get(), s.second);
	} else if (p[0] == OP_ZONE_HEATING_ENABLED) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_zone_heating_enabled(i, *p++);
		}
		auto s = _state->get(false, OP_ZONE_HEATING_ENABLED);
		write(socket, s.first.get(), s.second);
	} else if (p[0] == OP_TARGET) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_target(i, static_cast<float>(p[0] | (p[1] << 8)) / 16);
		}
		auto s = _state->get(false, OP_TARGET);
		write(socket, s.first.get(), s.second);
	}
}
catch (runtime_error& e)
{
	LOG("ControlServer conversation died: %1", e.what());
}
