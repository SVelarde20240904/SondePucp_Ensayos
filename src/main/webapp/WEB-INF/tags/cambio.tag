<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="sdp" tagdir="/WEB-INF/tags" %>
<%@ attribute name="antes" required="false" %>
<%@ attribute name="despues" required="true" %>
<span class="change"><% if (antes != null && !antes.isEmpty()) { %><sdp:chip color="${antes}"/><i>→</i><% } %><sdp:chip color="${despues}"/></span>
