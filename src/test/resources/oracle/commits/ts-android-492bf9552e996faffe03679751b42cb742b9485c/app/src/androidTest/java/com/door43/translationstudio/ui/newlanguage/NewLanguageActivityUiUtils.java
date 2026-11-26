package com.door43.translationstudio.ui.newlanguage;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.unfoldingword.door43client.Door43Client;
import org.unfoldingword.door43client.models.Question;
import org.unfoldingword.tools.logger.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.door43.translationstudio.App;
import com.door43.translationstudio.R;
import com.door43.questionnaire.QuestionnairePager;
import com.door43.questionnaire.QuestionnaireActivity;

import java.lang.reflect.Field;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import android.support.test.espresso.contrib.RecyclerViewActions;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

/**
 * shared methods for QuestionnaireActivity UI testing
 */
public class NewLanguageActivityUiUtils {

    private String mStringToBetyped;
    Context mTestContext;
    Context mAppContext;
    Field mf_resultCode;
    Field mf_resultData;


    @Rule
    public ActivityTestRule<NewTempLanguageActivity> mActivityRule = new ActivityTestRule<>(
            NewTempLanguageActivity.class,
            true,    // initialTouchMode
            false);  // don't launchActivity yet
    private QuestionnairePager pager;

    @Before
    public void setUp() {
        // Specify a valid string.
        mStringToBetyped = "Espresso";
        Logger.flush();
        mTestContext = InstrumentationRegistry.getContext();
        Door43Client library = App.getLibrary();
        if(!App.isLibraryDeployed()) {
            try {
                App.deployDefaultLibrary();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pager = new QuestionnairePager(App.getLibrary().index().getQuestionnaires().get(0));
    }

    /**
     * force page orientation change
     */
    protected void rotateScreen() {
        Context context = InstrumentationRegistry.getTargetContext();
        int orientation
                = context.getResources().getConfiguration().orientation;

        Activity activity = mActivityRule.getActivity();
        activity.setRequestedOrientation(
                (orientation == Configuration.ORIENTATION_PORTRAIT) ?
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * check values for answers returned by activity
     * @param answers
     * @param expectedAnswer
     * @throws Exception
     */
   protected void verifyAnswers(JSONArray answers, boolean expectedAnswer) throws Exception {
        for (int i = 0; i < answers.length(); i++) {
            JSONObject answer = answers.getJSONObject(i);
            int id = answer.getInt("question_id");
            String answerText =  answer.getString("answer");

            Question questionnaireQuestion = findQuestion(id);

            String expected;
            if(questionnaireQuestion.inputType == Question.InputType.String) {
                expected = generateAnswerForQuestion(id);
            } else {
                expected = expectedAnswer ? "true" : "false";
            }
            assertEquals("Question " + id + " mismatch", expected, answerText);
        }
    }

    /**
     * get activity results data and convert to JSON
     * @param currentActivity
     * @param key
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws JSONException
     */
    protected JSONObject getJsonData(QuestionnaireActivity currentActivity, String key) throws NoSuchFieldException, IllegalAccessException, JSONException {
        Intent resultData = getResultData(currentActivity);
        String jsonDataStr = resultData.getStringExtra(key);
        return new JSONObject(jsonDataStr);
    }

    /**
     * setup to capture the final results of activity
     * @throws NoSuchFieldException
     */
    protected void setupForCaptureOfResultData() throws NoSuchFieldException {
//        try {
//            getQuestionPages();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        mf_resultData = Activity.class.getDeclaredField("mResultData"); //NoSuchFieldException
        mf_resultData.setAccessible(true);
        mf_resultCode = Activity.class.getDeclaredField("mResultCode"); //NoSuchFieldException
        mf_resultCode.setAccessible(true);
    }

    /**
     * cleanup after setupForCaptureOfResultData
     * @throws NoSuchFieldException
     */
    protected void clearResultData() throws NoSuchFieldException {
        mf_resultData.setAccessible(false);
        mf_resultData = null;
        mf_resultCode.setAccessible(false);
        mf_resultCode = null;
    }

    /**
     * get activity results data
     * @param currentActivity
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    protected Intent getResultData(QuestionnaireActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        return (Intent) mf_resultData.get(currentActivity);
    }

    /**
     * get activity results code
     * @param currentActivity
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    protected int getResultCode(QuestionnaireActivity currentActivity) throws NoSuchFieldException, IllegalAccessException {
        return mf_resultCode.getInt(currentActivity);
    }

    /**
     * verify that missing required answer dialog is displayed
     */
    protected void thenShouldHaveRequiredAnswerDialog() {
        ViewInteraction vi = onView(withText(R.string.invalid_entry_title));
        assertNotNull(vi);
//        String warning = mAppContext.getResources().getString(R.string.missing_question_answer);
//        String[] lines = warning.split("\n");
//        warning = lines[0];
//        onView(withId(R.id.dialog_content)).check(matches(withText(startsWith(warning))));
    }

    /**
     * verify that missing non-required answer dialog is displayed
     */
    protected void thenShouldHaveMissingAnswerDialog() {
        assertNotNull(onView(withText(R.string.answers_missing_title)));
        assertNotNull(onView(withText(R.string.answers_missing_continue)));
    }

    /**
     * iteratively fill all the question pages with canned answers
     *
     * @param doDone
     * @param requiredOnly
     * @param valueForBooleans
     * @param hideKeyboard
     */
    protected void fillAllPagesAndRotate(boolean doDone, boolean requiredOnly, boolean valueForBooleans, boolean hideKeyboard) {
        int pageCount = pager.size();

        for (int i = 0; i < pageCount - 1; i++) {

            fillPage(i, false, requiredOnly, valueForBooleans, hideKeyboard);
            rotateScreen();
            verifyPageLayout( pageCount, i);

            onView(withId(R.id.next_button)).perform(click());
            verifyPageLayout( pageCount, i + 1);
        }

        fillPage(pageCount - 1, false, requiredOnly, valueForBooleans, hideKeyboard);

        if(doDone) {
            onView(withId(R.id.done_button)).perform(click());
        }
    }

    /**
     * iteratively fill all the question pages with canned answers
     *
     * @param doDone
     * @param requiredOnly
     * @param valueForBooleans
     * @param hideKeyboard
     */
    protected void fillAllPages(boolean doDone, boolean requiredOnly, boolean valueForBooleans, boolean hideKeyboard) {
        int pageCount = pager.size();

        for (int i = 0; i < pageCount - 1; i++) {

            fillPage(i, true, requiredOnly, valueForBooleans, hideKeyboard);
            verifyPageLayout( pageCount, i + 1);
        }

        fillPage(pageCount - 1, false, requiredOnly, valueForBooleans, hideKeyboard);

        if(doDone) {
            onView(withId(R.id.done_button)).perform(click());
        }
    }

    /**
     * fill a page of questions with canned answers
     * @param pageNum
     * @param doNext
     * @param requiredOnly
     * @param valueForBooleans
     * @param hideKeyboard
     */
    protected void fillPage(int pageNum, boolean doNext, boolean requiredOnly, boolean valueForBooleans, boolean hideKeyboard) {
        List<Question> questions = pager.getPage(pageNum).getQuestions();

        for (int i = 0; i < questions.size(); i++) {

            Question question = questions.get(i);
            if(!requiredOnly || question.isRequired) {
                if(question.inputType == Question.InputType.Boolean) {
                    boolean value = valueForBooleans;
                    setBoolean(pageNum, i, value);
                } else {
                    String text = generateAnswerForQuestion(question.tdId);
                    addEditText(pageNum, i, text, hideKeyboard);
                }
            } else { // not setting
                if(question.inputType == Question.InputType.Boolean) {
                    verifyButtons(pageNum, i, false, false);
                } else {
                    verifyAnswer(pageNum,i,"");
                }
            }
        }

        if(doNext) {
            onView(withId(R.id.next_button)).perform(click());
        }
    }

    /**
     * create canned string answer for question number
     * @param questionId
     * @return
     */
    private String generateAnswerForQuestion(long questionId) {
        return String.format("A-%d", questionId);
    }

    /**
     * create canned boolean answer for question number
     * @param questionNum
     * @return
     */
    private boolean generateAnswerForBoolean(int questionNum) {
        return questionNum % 2 == 1;
    }

    /**
     * verify answer in question
     * @param pageNum
     * @param questionNum
     * @param text
     */
    protected void verifyAnswer(int pageNum, int questionNum, String text) {
        Question question = pager.getPage(pageNum).getQuestion(questionNum);
        String questionText = question.text;
        ViewInteraction interaction = onView(allOf(withId(R.id.edit_text), hasSibling(withText(questionText))));
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.scrollToPosition(questionNum));
        interaction.check(matches(withText(text)));
    }

    /**
     * enter answer into question
     * @param pageNum
     * @param questionNum
     * @param newText
     * @param hideKeyboard
     */
    protected void addEditText(int pageNum, int questionNum, String newText, boolean hideKeyboard) {
        verifyAnswer(pageNum, questionNum, "");
        Question question = pager.getPage(pageNum).getQuestion(questionNum);
        String questionText = question.text;
        ViewInteraction interaction = onView(allOf(withId(R.id.edit_text), hasSibling(withText(questionText))));
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.scrollToPosition(questionNum));
        interaction.perform(typeText(newText));
        interaction.check(matches(withHint(question.help))); // doesn't seem to work on second question
        if(hideKeyboard) {
            interaction.perform(closeSoftKeyboard());
        }
        interaction.check(matches(withText(newText)));
    }

    /**
     * verify check state of radio button
     * @param pageNum
     * @param questionNum
     * @param resource
     * @param isChecked
     */
    private void verifyButton(int pageNum, int questionNum, int resource, boolean isChecked) {
        String questionText = pager.getPage(pageNum).getQuestion(questionNum).text;
        Matcher<View> parent = allOf(withClassName(endsWith("RadioGroup")), hasSibling(withText(questionText)));

        ViewInteraction interaction = onView(allOf(withId(resource), withParent(parent)));
        if(isChecked) {
            interaction.check(matches(isChecked()));
        } else {
            interaction.check(matches(not(isChecked())));
        }
    }

    /**
     * verify check states of boolean answer
     * @param pageNum
     * @param questionNum
     * @param yesChecked
     * @param noChecked
     */
    private void verifyButtons(int pageNum, int questionNum, boolean yesChecked, boolean noChecked) {
        verifyButton( pageNum,  questionNum, R.id.radio_button_yes, yesChecked);
        verifyButton( pageNum,  questionNum, R.id.radio_button_no, noChecked);
    }

    /**
     * set state for boolean answer
     * @param pageNum
     * @param questionNum
     * @param value
     */
    private void setBoolean(int pageNum, int questionNum, boolean value) {
        verifyButtons(pageNum, questionNum, false, false); // should not be set yet
        String questionText = pager.getPage(pageNum).getQuestion(questionNum).text;
        Matcher<View> parent = allOf(withClassName(endsWith("RadioGroup")), hasSibling(withText(questionText)));

        int resource = value ? R.id.radio_button_yes : R.id.radio_button_no;
        int oppositeResource = !value ? R.id.radio_button_yes : R.id.radio_button_no;
        ViewInteraction interaction = onView(allOf(withId(resource), withParent(parent)));
        interaction.perform(click());
        interaction.check(matches(isChecked()));
        onView(allOf(withId(oppositeResource), withParent(parent))).check(matches(not(isChecked())));
    }

    /**
     * find question for ID
     * @param id
     * @return
     * @throws Exception
     */
    private Question findQuestion(long id) throws Exception {
        return pager.getQuestion(id);
    }

    /**
     * verify layout of page (title and navigation buttons)
     * @param pageCount
     * @param pageNum
     */
    protected void verifyPageLayout(int pageCount, int pageNum) {
        thenTitlePageCountShouldMatch(pageCount, pageNum);
        verifyNavButtonSettings(pageCount, pageNum);
    }

    /**
     * verify title for page
     * @param pageCount
     * @param pageNum
     */
    private void thenTitlePageCountShouldMatch(int pageCount, int pageNum) {
        mAppContext = InstrumentationRegistry.getTargetContext();
        String titleFormat = mAppContext.getResources().getString(R.string.questionnaire_title);

        String title = String.format(titleFormat, pageNum + 1, pageCount);
        matchToolbarTitle(title);
    }

    /**
     * verify title for page
     * @param pageCount
     * @param pageNum
     */
    protected void thenTitlePageCountShouldNotMatch(int pageCount, int pageNum) {
        mAppContext = InstrumentationRegistry.getTargetContext();
        String titleFormat = mAppContext.getResources().getString(R.string.questionnaire_title);

        String title = String.format(titleFormat, pageNum + 1, pageCount);
        notMatchToolbarTitle(title);
    }

    /**
     * verify navigation buttons displayed on page
     * @param pageCount
     * @param pageNum
     */
    private void verifyNavButtonSettings(int pageCount, int pageNum) {
        if(pageNum == 0) {
            onView(withId(R.id.previous_button)).check(matches(not(isDisplayed())));
        } else {
            onView(withId(R.id.previous_button)).check(matches(isDisplayed()));
        }

        if(pageNum < pageCount - 1) {
            onView(withId(R.id.next_button)).check(matches(isDisplayed()));
            onView(withId(R.id.done_button)).check(matches(not(isDisplayed())));
        } else {
            onView(withId(R.id.done_button)).check(matches(isDisplayed()));
            onView(withId(R.id.next_button)).check(matches(not(isDisplayed())));
        }
    }

    /**
     * get interaction for toolbar with title
     * @param title
     * @return
     */
    private static ViewInteraction matchToolbarTitle(
            CharSequence title) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(is(title))));
    }

    /**
     * get interaction for toolbar with title
     * @param title
     * @return
     */
    private static ViewInteraction notMatchToolbarTitle(
            CharSequence title) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(not(withToolbarTitle(is(title)))));
    }

    /**
     * match toolbar
     * @param textMatcher
     * @return
     */
    private static Matcher<Object> withToolbarTitle(
            final Matcher<CharSequence> textMatcher) {
        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            @Override public boolean matchesSafely(Toolbar toolbar) {
                return textMatcher.matches(toolbar.getTitle());
            }
            @Override public void describeTo(Description description) {
                description.appendText("with toolbar title: ");
                textMatcher.describeTo(description);
            }
        };
    }

}