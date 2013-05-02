<%@ page import="com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.Entity" %>
<%@ page import="com.google.appengine.tck.logservice.RequestLogsTest" %>
<%@ page import="com.google.apphosting.api.ApiProxy" %>
<%--
  ~ Copyright 2013 Google Inc. All Rights Reserved.
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%
    String requestId = (String) ApiProxy.getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.request_log_id");

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = new Entity(RequestLogsTest.ENTITY_KIND, request.getParameter("entityName"));
    entity.setProperty(RequestLogsTest.REQUEST_ID_PROPERTY, requestId);
    datastore.put(entity);
%>

<%= requestId %>