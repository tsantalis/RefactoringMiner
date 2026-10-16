// The parts of googletest and googlemock that define code, given to CDT before the C and C++ files that include gtest or gmock headers,
// so that the tests, the mocked methods and the matchers are functions of the model, as written in the file.
// The test functions have the names of the expansions of googletest, i.e., Suite_Name_Test::TestBody for TEST(Suite, Name),
// whose classes derive from the fixtures, so that CDT finds the names of the fixtures in the tests, and are not classes of the model.
// The tests have the attribute Test, which is their annotation in the model, like @Test in Java, so that they are matched as tests.
// The assertions, i.e., EXPECT_EQ(a, b) or EXPECT_CALL(mock, f()), are not defined, so they are parsed as calls.
// The lines starting with // are removed before parsing.
namespace testing {
class Test {
 public:
  virtual ~Test();
 protected:
  virtual void SetUp();
  virtual void TearDown();
 private:
  virtual void TestBody() = 0;
};
template<class T> class WithParamInterface {
 public:
  typedef T ParamType;
  static const ParamType& GetParam();
};
template<class T> class TestWithParam : public Test, public WithParamInterface<T> {};
class MatchResultListener;
}
#define GTEST_STUB_TEST_(test_suite_name, test_name, parent_class) class test_suite_name##_##test_name##_Test : public parent_class { void TestBody() override; }; [[Test]] void test_suite_name##_##test_name##_Test::TestBody()
#define TEST(test_suite_name, test_name) GTEST_STUB_TEST_(test_suite_name, test_name, ::testing::Test)
#define GTEST_TEST(test_suite_name, test_name) GTEST_STUB_TEST_(test_suite_name, test_name, ::testing::Test)
#define TEST_F(test_fixture, test_name) GTEST_STUB_TEST_(test_fixture, test_name, test_fixture)
#define TEST_P(test_suite_name, test_name) GTEST_STUB_TEST_(test_suite_name, test_name, test_suite_name)
#define TYPED_TEST(CaseName, TestName) template <typename gtest_TypeParam_> class CaseName##_##TestName##_Test : public CaseName<gtest_TypeParam_> { typedef CaseName<gtest_TypeParam_> TestFixture; typedef gtest_TypeParam_ TypeParam; void TestBody() override; }; template <typename gtest_TypeParam_> [[Test]] void CaseName##_##TestName##_Test<gtest_TypeParam_>::TestBody()
#define TYPED_TEST_P(SuiteName, TestName) namespace gtest_suite_##SuiteName##_ { template <typename gtest_TypeParam_> class TestName : public SuiteName<gtest_TypeParam_> { typedef SuiteName<gtest_TypeParam_> TestFixture; typedef gtest_TypeParam_ TypeParam; void TestBody() override; }; } template <typename gtest_TypeParam_> [[Test]] void gtest_suite_##SuiteName##_::TestName<gtest_TypeParam_>::TestBody()
// MOCK_METHOD(ReturnType, Name, (Parameters), (Specifiers)) is the declaration of the method
#define GMOCK_STUB_FIRST_(first, ...) first
#define MOCK_METHOD(return_type, name, ...) return_type name GMOCK_STUB_FIRST_(__VA_ARGS__, )
// MATCHER_P(Name, p0, description) { body } is a function with the matched value arg, the parameters and the result_listener used in the body
#define MATCHER(name, description) template <typename arg_type> bool name(const arg_type& arg, ::testing::MatchResultListener* result_listener)
#define MATCHER_P(name, p0, description) template <typename arg_type, typename p0##_type> bool name(const arg_type& arg, const p0##_type& p0, ::testing::MatchResultListener* result_listener)
#define MATCHER_P2(name, p0, p1, description) template <typename arg_type, typename p0##_type, typename p1##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, ::testing::MatchResultListener* result_listener)
#define MATCHER_P3(name, p0, p1, p2, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, ::testing::MatchResultListener* result_listener)
#define MATCHER_P4(name, p0, p1, p2, p3, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, ::testing::MatchResultListener* result_listener)
#define MATCHER_P5(name, p0, p1, p2, p3, p4, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, ::testing::MatchResultListener* result_listener)
#define MATCHER_P6(name, p0, p1, p2, p3, p4, p5, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type, typename p5##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, const p5##_type& p5, ::testing::MatchResultListener* result_listener)
#define MATCHER_P7(name, p0, p1, p2, p3, p4, p5, p6, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type, typename p5##_type, typename p6##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, const p5##_type& p5, const p6##_type& p6, ::testing::MatchResultListener* result_listener)
#define MATCHER_P8(name, p0, p1, p2, p3, p4, p5, p6, p7, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type, typename p5##_type, typename p6##_type, typename p7##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, const p5##_type& p5, const p6##_type& p6, const p7##_type& p7, ::testing::MatchResultListener* result_listener)
#define MATCHER_P9(name, p0, p1, p2, p3, p4, p5, p6, p7, p8, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type, typename p5##_type, typename p6##_type, typename p7##_type, typename p8##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, const p5##_type& p5, const p6##_type& p6, const p7##_type& p7, const p8##_type& p8, ::testing::MatchResultListener* result_listener)
#define MATCHER_P10(name, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, description) template <typename arg_type, typename p0##_type, typename p1##_type, typename p2##_type, typename p3##_type, typename p4##_type, typename p5##_type, typename p6##_type, typename p7##_type, typename p8##_type, typename p9##_type> bool name(const arg_type& arg, const p0##_type& p0, const p1##_type& p1, const p2##_type& p2, const p3##_type& p3, const p4##_type& p4, const p5##_type& p5, const p6##_type& p6, const p7##_type& p7, const p8##_type& p8, const p9##_type& p9, ::testing::MatchResultListener* result_listener)
// the internal macros of googletest used in its own code, with the definitions of the configuration without the headers of the platform
#define GTEST_API_
#define GTEST_DISABLE_MSC_WARNINGS_PUSH_(warnings)
#define GTEST_DISABLE_MSC_WARNINGS_POP_()
#define GTEST_HAS_EXCEPTIONS 0
#define GTEST_HAS_RTTI 1
#define GTEST_HAS_STD_WSTRING 1
#define GTEST_INTERNAL_HAS_STRING_VIEW 1
#define GTEST_REMOVE_REFERENCE_AND_CONST_(T) typename std::remove_const<typename std::remove_reference<T>::type>::type
