#include "exception_store.h"
#include <boost/asio.hpp>
#include <boost/signals2.hpp>
#include <thread>

class BroadcastListener : public ExceptionStore
{
public:
	BroadcastListener();
	~BroadcastListener();

	void run();

	boost::signals2::signal<void (std::string, boost::asio::ip::address)> Received;

private:
	void thread();
	void received(int bytes);

	std::thread* _thread;
	boost::asio::ip::udp::socket* _socket;
	char _buffer[64];
	boost::asio::ip::udp::endpoint _send_endpoint;
	boost::asio::io_service _io_service;
};
