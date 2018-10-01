#include "toast_socket.h"
#include <iostream>

using std::bind;
using std::cout;
using std::runtime_error;

/** @param timeout Timeout in seconds */
Socket::Socket(int timeout)
	: _deadline(_io_service)
	, _socket(_io_service)
	, _timeout(timeout)
{
	_deadline.expires_at(boost::posix_time::pos_infin);
	check();
}

void
Socket::check()
{
	if (_deadline.expires_at() <= boost::asio::deadline_timer::traits_type::now()) {
		_socket.close();
		_deadline.expires_at(boost::posix_time::pos_infin);
	}

	_deadline.async_wait(bind(&Socket::check, this));
}

/** Blocking connect.
 *  @param endpoint End-point to connect to.
 */
void
Socket::connect(boost::asio::ip::tcp::endpoint endpoint)
{
	_deadline.expires_from_now(boost::posix_time::seconds(_timeout));
	boost::system::error_code ec = boost::asio::error::would_block;
	_socket.async_connect(endpoint, [&](boost::system::error_code const & error){ec = error;});
	do {
		_io_service.run_one();
	} while (ec == boost::asio::error::would_block);

	if (ec) {
		throw runtime_error("error during async_connect");
	}

	if (!_socket.is_open()) {
		throw runtime_error("connect timed out");
	}
}

/** Blocking write.
 *  @param data Buffer to write.
 *  @param size Number of bytes to write.
 */
void
Socket::write(uint8_t const * data, int size)
{
	_deadline.expires_from_now(boost::posix_time::seconds(_timeout));
	boost::system::error_code ec = boost::asio::error::would_block;
	boost::asio::async_write(_socket, boost::asio::buffer(data, size), [&](boost::system::error_code const & error, size_t){ec = error;});

	do {
		_io_service.run_one();
	} while (ec == boost::asio::error::would_block);

	if (ec) {
		throw runtime_error("error during async_write");
	}
}

/** Blocking read.
 *  @param data Buffer to read to.
 *  @param size Maximum number of bytes to read.
 */
int
Socket::read(uint8_t* data, int size)
{
	_deadline.expires_from_now(boost::posix_time::seconds(_timeout));
	boost::system::error_code ec = boost::asio::error::would_block;
	size_t received = 0;
	boost::asio::async_read(_socket, boost::asio::buffer(data, size), [&](const boost::system::error_code& error, size_t bytes) {ec = error; received += bytes;});

	do {
		_io_service.run_one();
	} while (ec == boost::asio::error::would_block);

	if (ec != boost::asio::error::eof) {
		throw runtime_error("error during async_read");
	}

	return received;
}
