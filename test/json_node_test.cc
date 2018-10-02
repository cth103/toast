#include "../src/json_node.h"
#include <boost/test/unit_test.hpp>

BOOST_AUTO_TEST_CASE(json_node_test)
{
	BOOST_CHECK_CLOSE(JSONNode::parse_reply("{ \"temperature\" : 19.5 }"), 19.5, 1);
	BOOST_CHECK_CLOSE(JSONNode::parse_reply("{\"temperature\" : 6.0}"), 6.0, 1);
}
