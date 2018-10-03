#include "log.h"
#include "config.h"
#include <iostream>

using std::cout;
using std::string;
using std::scoped_lock;
using std::mutex;

mutex Log::_mutex;

void
Log::log(string s)
{
	scoped_lock lm(_mutex);

	string const log_path = Config::instance()->log_directory() + "/toastd.log";
	FILE* f = fopen(log_path.c_str(), "a+");
	if (!f) {
		cout << String::compose("Could not open log file %1", log_path) << "\n";
		return;
	}

	time_t t = time(0);
	struct tm tm = *localtime(&t);

	fprintf(f, "%02d/%02d/%02d %02d:%02d:%02d %s\n", tm.tm_mday, tm.tm_mon, tm.tm_year, tm.tm_hour, tm.tm_min, tm.tm_sec, s.c_str());
	fclose(f);
}
