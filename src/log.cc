#include "log.h"
#include "config.h"
#include <iostream>

using std::cout;
using std::string;

void
Log::log(string s)
{
	FILE* f = fopen(LOG_DIRECTORY "/toastd.log", "a+");
	if (!f) {
		cout << "Could not open log file.\n";
		return;
	}

	time_t t = time(0);
	struct tm tm = *localtime(&t);

	fprintf(f, "%02d/%02d/%02d %02d:%02d:%02d %s\n", tm.tm_mday, tm.tm_mon, tm.tm_year, tm.tm_hour, tm.tm_min, tm.tm_sec, s.c_str());
	fclose(f);
}
