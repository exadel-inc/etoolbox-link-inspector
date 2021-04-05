<%@include file="/libs/granite/ui/global.jsp" %>
<%@page session="false"
        import="com.exadel.etoolbox.linkinspector.core.services.GridDataSource,
                com.adobe.granite.ui.components.ds.DataSource" %>
<%--
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%
    GridDataSource service = (GridDataSource) sling.getService(GridDataSource.class);
    request.setAttribute(DataSource.class.getName(), service.getDataSource());
%>
