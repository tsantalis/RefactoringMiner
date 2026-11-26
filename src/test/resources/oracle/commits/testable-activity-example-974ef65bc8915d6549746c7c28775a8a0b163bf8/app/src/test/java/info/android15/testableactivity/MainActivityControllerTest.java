package info.android15.testableactivity;

import android.view.View;
import android.widget.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainActivityControllerTest {

    @Mock MainActivity activity;
    @Mock Button buttonClickMe;

    MainActivityController controller;

    @Before
    public void before() {
        when(activity.findViewById(R.id.buttonClickMe)).thenReturn(buttonClickMe);
        controller = new MainActivityController(activity);
    }

    @Test
    public void testOnCreate() throws Exception {
        controller.onCreate(null);
        verify(buttonClickMe, times(1)).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void newColor() throws Exception {
        controller.onCreate(null);
        controller.onButtonClickMe();
        verify(buttonClickMe, times(1)).setBackgroundColor(anyInt());
    }
}
