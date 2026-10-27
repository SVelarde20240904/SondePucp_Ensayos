<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="texto" required="true" %>
<%@ attribute name="tono" required="false" %>
<%@ attribute name="rol" required="false" type="java.lang.Boolean" %>
<span class="tag ${empty tono ? 'neutral' : tono}${rol ? ' role' : ''}"><c:out value="${texto}"/></span>
