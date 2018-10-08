#include "../src/config.h"
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_CASE(config_test)
{
	Config c("test/config1");
	BOOST_REQUIRE_EQUAL(c.log_types(), 9);
	BOOST_REQUIRE_EQUAL(c.server_port(), 1234);
	BOOST_REQUIRE_EQUAL(c.log_directory(), "/dev/null");
	BOOST_REQUIRE_EQUAL(c.hidden_zones().size(), 1);
	BOOST_REQUIRE_EQUAL(c.hidden_zones().front(), "Landing");
	BOOST_REQUIRE_EQUAL(c.auto_off_hours().size(), 8);
	auto oh = c.auto_off_hours();
	auto i = oh.begin();
	BOOST_CHECK_EQUAL(*i++, 0);
	BOOST_CHECK_EQUAL(*i++, 1);
	BOOST_CHECK_EQUAL(*i++, 2);
	BOOST_CHECK_EQUAL(*i++, 3);
	BOOST_CHECK_EQUAL(*i++, 4);
	BOOST_CHECK_EQUAL(*i++, 5);
	BOOST_CHECK_EQUAL(*i++, 6);
	BOOST_CHECK_EQUAL(*i++, 10);
}
