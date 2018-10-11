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

/** @file  src/log.cc
 *  @brief Log class.
 */

#include "log.h"
#include "config.h"
#include "util.h"
#include <iostream>

using std::cout;
using std::string;
using std::scoped_lock;
using std::mutex;

mutex Log::_mutex;

/** @param type Type of log message that this is; see constants in Log class.
 *  @param s Log message.
 */
void
Log::log(int type, string s)
{
	scoped_lock lm(_mutex);
	if (type && ((type & Config::instance()->log_types()) == 0)) {
		return;
	}

	string const log_path = Config::instance()->log_directory() + "/toastd.log";
	FILE* f = fopen(log_path.c_str(), "a+");
	if (!f) {
		cout << String::compose("Could not open log file %1", log_path) << "\n";
		return;
	}

	struct tm tm = now();
	fprintf(f, "%02d/%02d/%02d %02d:%02d:%02d %s\n", tm.tm_mday, tm.tm_mon, tm.tm_year + 1900, tm.tm_hour, tm.tm_min, tm.tm_sec, s.c_str());
	fclose(f);
}
