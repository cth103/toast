/*
    Copyright (C) 2018 Carl Hetherington <cth@carlh.net>

    This file is part of toast.

    toast is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    toast is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with toast.  If not, see <http://www.gnu.org/licenses/>.

*/

/** @file  src/log.h
 *  @brief Log class.
 */

#include "compose.hpp"
#include <string>
#include <mutex>

/** @class Log
 *  @brief Program-wide logging.
 */
class Log
{
public:
	static int const CLIENT = 0x1;
	static int const ERROR = 0x2;
	static int const DECISION = 0x4;
	static int const STARTUP = 0x8;
	static int const NODE = 0x10;

	static void log(int type, std::string);

private:
	/** mutex to protect calls to log() */
	static std::mutex _mutex;
};

#define LOG_CLIENT(...)      Log::log(Log::CLIENT,   String::compose(__VA_ARGS__));
#define LOG_CLIENT_NC(...)   Log::log(Log::CLIENT,   __VA_ARGS__);
#define LOG_ERROR(...)       Log::log(Log::ERROR,    String::compose(__VA_ARGS__));
#define LOG_ERROR_NC(...)    Log::log(Log::ERROR,    __VA_ARGS__);
#define LOG_DECISION(...)    Log::log(Log::DECISION, String::compose(__VA_ARGS__));
#define LOG_DECISION_NC(...) Log::log(Log::DECISION, __VA_ARGS__);
#define LOG_STARTUP(...)     Log::log(Log::STARTUP,  String::compose(__VA_ARGS__));
#define LOG_STARTUP_NC(...)  Log::log(Log::STARTUP,  __VA_ARGS__);
#define LOG_NODE(...)        Log::log(Log::NODE,     String::compose(__VA_ARGS__));
#define LOG_NODE_NC(...)     Log::log(Log::NODE,     __VA_ARGS__);
