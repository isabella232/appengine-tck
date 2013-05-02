/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.tck.taskqueue;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tck.taskqueue.support.DefaultQueueServlet;
import com.google.appengine.tck.taskqueue.support.PrintServlet;
import com.google.appengine.tck.taskqueue.support.RequestData;
import com.google.appengine.tck.taskqueue.support.RetryTestServlet;
import com.google.appengine.tck.taskqueue.support.TestQueueServlet;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withHeader;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withMethod;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withTaskName;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;
import static com.google.appengine.api.taskqueue.TaskOptions.Method.DELETE;
import static com.google.appengine.api.taskqueue.TaskOptions.Method.GET;
import static com.google.appengine.api.taskqueue.TaskOptions.Method.HEAD;
import static com.google.appengine.api.taskqueue.TaskOptions.Method.POST;
import static com.google.appengine.api.taskqueue.TaskOptions.Method.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class AsyncTasksTest extends QueueTestBase {

    @Before
    public void setUp() throws Exception {
        DefaultQueueServlet.reset();
        TestQueueServlet.reset();
        RetryTestServlet.reset();
    }

    @After
    public void tearDown() throws Exception {
        PrintServlet.reset();
    }

    @Test
    public void testSmoke() throws Exception {
        final Queue queue = QueueFactory.getQueue("tasks-queue");
        waitOnFuture(queue.addAsync(withUrl(URL)));
        sync();
        assertNotNull(PrintServlet.getLastRequest());
    }

    @Test
    public void testTaskWithoutUrlIsSubmittedToDefaultUrl() throws Exception {
        Queue defaultQueue = QueueFactory.getDefaultQueue();
        waitOnFuture(defaultQueue.addAsync(withMethod(POST)));
        sync();
        assertTrue("DefaultQueueServlet was not invoked", DefaultQueueServlet.wasInvoked());

        Queue testQueue = QueueFactory.getQueue("test");
        waitOnFuture(testQueue.addAsync(withMethod(POST)));
        sync();
        assertTrue("TestQueueServlet was not invoked", TestQueueServlet.wasInvoked());
    }

    @Test
    public void testTaskHandleContainsAllNecessaryProperties() throws Exception {
        Queue queue = QueueFactory.getDefaultQueue();
        TaskHandle handle = waitOnFuture(queue.addAsync(withTaskName("foo").payload("payload")));

        assertEquals("default", handle.getQueueName());
        assertEquals("foo", handle.getName());
        assertEquals("payload", new String(handle.getPayload(), "UTF-8"));
//        assertEquals(, handle.getEtaMillis());    // TODO
//        assertEquals(, handle.getRetryCount());
    }

    @Test
    public void testTaskHandleContainsAutoGeneratedTaskNameWhenTaskNameNotDefinedInTaskOptions() throws Exception {
        Queue queue = QueueFactory.getDefaultQueue();
        TaskHandle handle = waitOnFuture(queue.addAsync());
        assertNotNull(handle.getName());
    }

    @Test
    public void testRequestHeaders() throws Exception {
        Queue defaultQueue = QueueFactory.getDefaultQueue();
        waitOnFuture(defaultQueue.addAsync(withTaskName("task1")));
        sync();

        RequestData request = DefaultQueueServlet.getLastRequest();
        assertEquals("default", request.getHeader(QUEUE_NAME));
        assertEquals("task1", request.getHeader(TASK_NAME));
        assertNotNull(request.getHeader(TASK_RETRY_COUNT));
        assertNotNull(request.getHeader(TASK_EXECUTION_COUNT));
//        assertNotNull(request.getHeader("X-AppEngine-TaskETA"));    // TODO

        Queue testQueue = QueueFactory.getQueue("test");
        waitOnFuture(testQueue.addAsync(withTaskName("task2")));
        sync();

        request = TestQueueServlet.getLastRequest();
        assertEquals("test", request.getHeader(QUEUE_NAME));
        assertEquals("task2", request.getHeader(TASK_NAME));
    }

    @Test
    public void testAllPushMethodsAreSupported() throws Exception {
        assertServletReceivesCorrectMethod(GET);
        assertServletReceivesCorrectMethod(PUT);
        assertServletReceivesCorrectMethod(HEAD);
        assertServletReceivesCorrectMethod(POST);
        assertServletReceivesCorrectMethod(DELETE);
    }

    private void assertServletReceivesCorrectMethod(TaskOptions.Method method) {
        MethodRequestHandler handler = new MethodRequestHandler();
        PrintServlet.setRequestHandler(handler);

        Queue queue = QueueFactory.getQueue("tasks-queue");
        waitOnFuture(queue.addAsync(withUrl(URL).method(method)));
        sync();

        assertEquals("Servlet received invalid HTTP method.", method.name(), handler.method);
    }

    @Test
    public void testPayload() throws Exception {
        String sentPayload = "payload";

        Queue queue = QueueFactory.getDefaultQueue();
        waitOnFuture(queue.addAsync(withPayload(sentPayload)));
        sync();

        String receivedPayload = new String(DefaultQueueServlet.getLastRequest().getBody(), "UTF-8");
        assertEquals(sentPayload, receivedPayload);
    }

    @Test
    public void testHeaders() throws Exception {
        Queue queue = QueueFactory.getDefaultQueue();
        waitOnFuture(queue.addAsync(withHeader("header_key", "header_value")));
        sync();

        RequestData lastRequest = DefaultQueueServlet.getLastRequest();
        assertEquals("header_value", lastRequest.getHeader("header_key"));
    }

    @Test
    public void testParams() throws Exception {
        class ParamHandler implements PrintServlet.RequestHandler {
            private String paramValue;

            public void handleRequest(ServletRequest req) {
                paramValue = req.getParameter("single_value");
            }
        }

        ParamHandler handler = new ParamHandler();
        PrintServlet.setRequestHandler(handler);

        final Queue queue = QueueFactory.getQueue("tasks-queue");
        waitOnFuture(queue.addAsync(withUrl(URL).param("single_value", "param_value")));
        sync();

        assertEquals("param_value", handler.paramValue);
    }

    @Test
    public void testMultiValueParams() throws Exception {
        class ParamHandler implements PrintServlet.RequestHandler {
            private String[] paramValues;

            public void handleRequest(ServletRequest req) {
                paramValues = req.getParameterValues("multi_value");
            }
        }

        ParamHandler handler = new ParamHandler();
        PrintServlet.setRequestHandler(handler);

        final Queue queue = QueueFactory.getQueue("tasks-queue");
        waitOnFuture(queue.addAsync(
            withUrl(URL)
                .param("multi_value", "param_value1")
                .param("multi_value", "param_value2")));
        sync();

        assertNotNull(handler.paramValues);
        assertEquals(
            new HashSet<String>(Arrays.asList("param_value1", "param_value2")),
            new HashSet<String>(Arrays.asList(handler.paramValues)));
    }

    @Test
    public void testRetry() throws Exception {
        RetryTestServlet.setNumberOfTimesToFail(1);

        Queue queue = QueueFactory.getDefaultQueue();
        waitOnFuture(queue.addAsync(withUrl("/_ah/retryTest").retryOptions(RetryOptions.Builder.withTaskRetryLimit(5))));
        sync();

        assertEquals(2, RetryTestServlet.getInvocationCount());

        RequestData request1 = RetryTestServlet.getRequest(0);
        assertEquals("0", request1.getHeader(TASK_RETRY_COUNT));
        assertEquals("0", request1.getHeader(TASK_EXECUTION_COUNT));

        RequestData request2 = RetryTestServlet.getRequest(1);
        assertEquals("1", request2.getHeader(TASK_RETRY_COUNT));
        assertEquals("1", request2.getHeader(TASK_EXECUTION_COUNT));
    }

    @Test
    public void testRetryLimitIsHonored() throws Exception {
        RetryTestServlet.setNumberOfTimesToFail(10);

        Queue queue = QueueFactory.getDefaultQueue();
        waitOnFuture(queue.addAsync(withUrl("/_ah/retryTest").retryOptions(RetryOptions.Builder.withTaskRetryLimit(2))));
        sync();

        assertEquals(2, RetryTestServlet.getInvocationCount());
    }

    private class MethodRequestHandler implements PrintServlet.RequestHandler {
        private String method;

        public void handleRequest(ServletRequest req) {
            method = ((HttpServletRequest) req).getMethod();
        }
    }

}
