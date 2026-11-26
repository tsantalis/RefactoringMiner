package info.android15.testableactivity;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainActivityControllerTest {

    @Mock MainActivity activity;
    @Mock Button buttonClickMe;
    AtomicReference<View.OnClickListener> buttonClickMeOnClickListener;

    MainActivityController controller;

    @Before
    public void before() {
        buttonClickMeOnClickListener = requireOnClick(activity, R.id.buttonClickMe, buttonClickMe);
        controller = new MainActivityController(activity);
        controller.onCreate(null);
    }

    @Test
    public void newColor() throws Exception {
        controller.onCreate(null);
        buttonClickMeOnClickListener.get().onClick(buttonClickMe);
        verify(buttonClickMe, times(1)).setBackgroundColor(anyInt());
    }

    public AtomicReference<View.OnClickListener> requireOnClick(Activity activityMock, int viewId, View viewMock) {
        final AtomicReference<View.OnClickListener> listenerRef = new AtomicReference<>();
        when(activityMock.findViewById(viewId)).thenReturn(viewMock);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                listenerRef.set((View.OnClickListener)invocation.getArguments()[0]);
                return null;
            }
        }).when(viewMock).setOnClickListener(any(View.OnClickListener.class));
        return listenerRef;
    }
}
