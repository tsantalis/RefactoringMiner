
//              Copyright Catch2 Authors
// Distributed under the Boost Software License, Version 1.0.
//   (See accompanying file LICENSE.txt or copy at
//        https://www.boost.org/LICENSE_1_0.txt)

// SPDX-License-Identifier: BSL-1.0

#include <catch2/catch_test_macros.hpp>
#include <catch2/matchers/catch_matchers_templated.hpp>
#include <catch2/matchers/catch_matchers_container_properties.hpp>

#include <array>

#if defined( CATCH_INTERNAL_CONSTEXPR_MATCHERS_ENABLED )

namespace {
    struct MatchAllMatcher final : public Catch::Matchers::MatcherGenericBase {
    public:
        template <typename Any>
        constexpr bool match( Any&& ) const {
            return true;
        }

        std::string describe() const override {
            using namespace std::string_literals;
            return "Matches anything"s;
        }
    };

    constexpr MatchAllMatcher MatchAll() { return MatchAllMatcher(); }

} // namespace

TEST_CASE( "Constexpr support for matchers", "[constexpr][matchers][approvals]" ) {
    STATIC_REQUIRE( MatchAll().match( 1 ) );
    STATIC_REQUIRE_THAT( 1, MatchAll() );
}

TEST_CASE( "IsEmpty and HasSize matchers can be used in constexpr contexts",
           "[constexpr][matchers][approvals]" ){
    static constexpr std::array<int, 0> empty{};
    STATIC_REQUIRE_THAT( empty, Catch::Matchers::IsEmpty() );
    static constexpr int arr[1] = { 2 };
    STATIC_REQUIRE_THAT( arr, Catch::Matchers::SizeIs( 1 ) );
    STATIC_REQUIRE_THAT( arr, Catch::Matchers::SizeIs( MatchAll() ) );
}

// Combining matchers needs C++26 and P2738, so they are in separate preprocessor block
#    if __cpp_constexpr >= 202306L

TEST_CASE("Constexpr support for combining matchers",
    "[constexpr][matchers][approvals]") {
    STATIC_REQUIRE( ( MatchAll() && MatchAll() ).match( 1 ) );
    STATIC_REQUIRE( ( MatchAll() || MatchAll() ).match( 1 ) );
    STATIC_REQUIRE( ( !!MatchAll() ).match( 1 ) );
    STATIC_REQUIRE_THAT( 1, MatchAll() && MatchAll() );
    STATIC_REQUIRE_THAT( 1, MatchAll() || MatchAll() );
    STATIC_REQUIRE_THAT( 1, !!MatchAll() );
}

#endif // __cpp_constexpr >= 202306L

#endif
