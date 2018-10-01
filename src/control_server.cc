#include "control_server.h"
#include "state.h"
#include "toast_socket.h"
#include "types.h"

using std::list;
using std::string;
using std::shared_ptr;

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
ControlServer::handle(shared_ptr<Socket> socket)
{
	uint8_t len[4];
	if (socket->read(len, 4) != 4) {
		return;
	}

	uint32_t length = (len[0] << 24) | (len[1] << 16) | (len[2] << 8) | len[3];
	shared_ptr<uint8_t[]> data(new uint8_t[length]);
	if (socket->read(data.get(), length) != static_cast<int>(length)) {
		return;
	}

	uint8_t* p = data.get();
	if (p[0] == OP_SEND_BASIC) {
		auto s = _state->get(false, OP_ALL);
		socket->write(s.first.get(), s.second);
	} else if (p[0] == OP_SEND_ALL) {
		auto s = _state->get(true, OP_ALL);
		socket->write(s.first.get(), s.second);
	} else if (p[0] == OP_HEATING_ENABLED) {
		_state->set_heating_enabled(p[1]);
		auto s = _state->get(false, OP_HEATING_ENABLED);
		socket->write(s.first.get(), s.second);
	} else if (p[0] == OP_ZONE_HEATING_ENABLED) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_zone_heating_enabled(i, *p++);
		}
		auto s = _state->get(false, OP_ZONE_HEATING_ENABLED);
		socket->write(s.first.get(), s.second);
	} else if (p[0] == OP_TARGET) {
		p++;
		for (auto i: zones_from_message(p)) {
			_state->set_target(i, static_cast<float>(p[0] | (p[1] << 8)) / 16);
		}
		auto s = _state->get(false, OP_TARGET);
		socket->write(s.first.get(), s.second);
	}
}
