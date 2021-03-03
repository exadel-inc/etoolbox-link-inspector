<%@include file="/libs/granite/ui/global.jsp" %>
<%@page session="false"
        import="com.exadel.aembox.linkchecker.core.services.GridDataSource,
                com.adobe.granite.ui.components.ds.DataSource" %>
<%
    GridDataSource service = (GridDataSource) sling.getService(GridDataSource.class);
    request.setAttribute(DataSource.class.getName(), service.getDataSource());
%>
