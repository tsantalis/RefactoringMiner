/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.tags;

import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

/**
 * Tests for {@link MessageTag}.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Nicholas Williams
 */
public class MessageTagTests extends AbstractTagTests {

	@SuppressWarnings("serial")
	public void testMessageTagWithMessageSourceResolvable() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setMessage(new DefaultMessageSourceResolvable("test"));
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCode() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithNullCode() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode(null);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "null", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message {1}", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1,arg2");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message arg2", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndStringArgumentWithCustomSeparator() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments("arg1,1;arg2,2");
		tag.setArgumentSeparator(";");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1,1 message arg2,2", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndArrayArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(new Object[] {"arg1", 5});
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message 5", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndObjectArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		tag.setArguments(5);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 5 message {1}", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndArgumentAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.setArguments(5);
		tag.addArgument(7);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 5 message 7", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndNestedArgument() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.addArgument(7);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test 7 message {1}", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndNestedArguments() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("testArgs");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		tag.addArgument("arg1");
		tag.addArgument(6);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test arg1 message 6", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithCodeAndText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setText("testtext");
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", (message.toString()));
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithText() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test & text é");
		tag.setHtmlEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertTrue("Correct message", message.toString().startsWith("test &amp; text &"));
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithTextEncodingEscaped() throws JspException {
		PageContext pc = createPageContext();
		pc.getServletContext().setInitParameter(WebUtils.RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM, "true");
		pc.getResponse().setCharacterEncoding("UTF-8");
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("test <&> é");
		tag.setHtmlEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test &lt;&amp;&gt; é", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithTextAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("' test & text \\");
		tag.setJavaScriptEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "\\' test & text \\\\", message.toString());
	}

	@SuppressWarnings("serial")
	public void testMessageTagWithTextAndHtmlEscapeAndJavaScriptEscape() throws JspException {
		PageContext pc = createPageContext();
		final StringBuffer message = new StringBuffer();
		MessageTag tag = new MessageTag() {
			@Override
			protected void writeMessage(String msg) {
				message.append(msg);
			}
		};
		tag.setPageContext(pc);
		tag.setText("' test & text \\");
		tag.setHtmlEscape(true);
		tag.setJavaScriptEscape(true);
		assertTrue("Correct doStartTag return value", tag.doStartTag() == Tag.EVAL_BODY_INCLUDE);
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "&#39; test &amp; text \\\\", message.toString());
	}

	public void testMessageWithVarAndScope() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.setScope("page");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("text & text", pc.getAttribute("testvar"));
		tag.release();

		tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar2");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", pc.getAttribute("testvar2"));
		tag.release();
	}

	public void testMessageWithVar() throws JspException {
		PageContext pc = createPageContext();
		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setText("text & text");
		tag.setVar("testvar");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("text & text", pc.getAttribute("testvar"));
		tag.release();

		// try to reuse
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar");

		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
		assertEquals("Correct message", "test message", pc.getAttribute("testvar"));
	}

	public void testNullMessageSource() throws JspException {
		PageContext pc = createPageContext();
		ConfigurableWebApplicationContext ctx = (ConfigurableWebApplicationContext)
				RequestContextUtils.getWebApplicationContext(pc.getRequest(), pc.getServletContext());
		ctx.close();

		MessageTag tag = new MessageTag();
		tag.setPageContext(pc);
		tag.setCode("test");
		tag.setVar("testvar2");
		tag.doStartTag();
		assertEquals("Correct doEndTag return value", Tag.EVAL_PAGE, tag.doEndTag());
	}

	public void testRequestContext() throws ServletException {
		PageContext pc = createPageContext();
		RequestContext rc = new RequestContext((HttpServletRequest) pc.getRequest(), pc.getServletContext());
		assertEquals("test message", rc.getMessage("test"));
		assertEquals("test message", rc.getMessage("test", (Object[]) null));
		assertEquals("test message", rc.getMessage("test", "default"));
		assertEquals("test message", rc.getMessage("test", (Object[]) null, "default"));
		assertEquals("test arg1 message arg2",
				rc.getMessage("testArgs", new String[] {"arg1", "arg2"}, "default"));
		assertEquals("test arg1 message arg2",
				rc.getMessage("testArgs", Arrays.asList(new String[] {"arg1", "arg2"}), "default"));
		assertEquals("default", rc.getMessage("testa", "default"));
		assertEquals("default", rc.getMessage("testa", (List) null, "default"));
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(new String[] {"test"});
		assertEquals("test message", rc.getMessage(resolvable));
	}

}
