#include "broadcast_listener.h"
#include "config.h"
#include "log.h"
#include <boost/asio.hpp>
#include <functional>
#include <iostream>

using std::bind;
using std::cout;
using std::string;
using std::runtime_error;

BroadcastListener::BroadcastListener()
{
}

BroadcastListener::~BroadcastListener()
{
	if (_socket) {
		_socket->close();
	}
	_io_service.stop();
	if (_thread) {
		if (_thread->joinable()) {
			_thread->join();
		}
		delete _thread;
	}
}

void
BroadcastListener::run()
{
	_thread = new std::thread(bind(&BroadcastListener::thread, this));
}

void
BroadcastListener::thread()
try
{
	boost::asio::ip::address address = boost::asio::ip::address_v4::any();
	boost::asio::ip::udp::endpoint listen_endpoint(address, Config::instance()->broadcast_port());

	_socket = new boost::asio::ip::udp::socket(_io_service);
	_socket->open(listen_endpoint.protocol());
	_socket->bind(listen_endpoint);

	_socket->async_receive_from(
		boost::asio::buffer(_buffer, sizeof(_buffer) - 1),
		_send_endpoint,
		bind(&BroadcastListener::received, this, std::placeholders::_2)
		);

	_io_service.run();
}
catch (runtime_error& e)
{
	/* XXX: no point in storing if nobody ever re-throws */
	LOG_NODE("Broadcast listener error: %1", e.what());
	store_current();
}
catch (...)
{
	store_current();
}

void
BroadcastListener::received(int bytes)
{
	_buffer[bytes] = '\0';
	string msg(_buffer);
	if (msg.size() == 26 && msg.substr(0, 13) == "Hello heating") {
		Received(msg.substr(14, 12), _send_endpoint.address());
	}
	_socket->async_receive_from(
		boost::asio::buffer(_buffer, sizeof(_buffer)),
		_send_endpoint, bind(&BroadcastListener::received, this, std::placeholders::_2)
		);
}
