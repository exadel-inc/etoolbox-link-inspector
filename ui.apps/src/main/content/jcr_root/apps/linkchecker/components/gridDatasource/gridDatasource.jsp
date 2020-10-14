<%
%><%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"
          import="com.exadel.linkchecker.core.services.GridDataSource,
                  com.adobe.granite.ui.components.ds.DataSource" %>
<%
    GridDataSource service = (GridDataSource)sling.getService(GridDataSource.class);
    DataSource dataSource = service.getDataSource(request, cmp, resource);
    request.setAttribute(DataSource.class.getName(), dataSource);
%>
