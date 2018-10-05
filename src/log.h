#include "compose.hpp"
#include <string>
#include <mutex>

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
	static std::mutex _mutex;
	static int _types;
};

#define LOG_CLIENT(...) Log::log(Log::CLIENT, String::compose(__VA_ARGS__));
#define LOG_CLIENT_NC(...) Log::log(Log::CLIENT, __VA_ARGS__);
#define LOG_ERROR(...) Log::log(Log::ERROR, String::compose(__VA_ARGS__));
#define LOG_ERROR_NC(...) Log::log(Log::ERROR, __VA_ARGS__);
#define LOG_DECISION(...) Log::log(Log::DECISION, String::compose(__VA_ARGS__));
#define LOG_DECISION_NC(...) Log::log(Log::DECISION, __VA_ARGS__);
#define LOG_STARTUP(...) Log::log(Log::STARTUP, String::compose(__VA_ARGS__));
#define LOG_STARTUP_NC(...) Log::log(Log::STARTUP, __VA_ARGS__);
#define LOG_NODE(...) Log::log(Log::NODE, String::compose(__VA_ARGS__));
#define LOG_NODE_NC(...) Log::log(Log::NODE, __VA_ARGS__);
