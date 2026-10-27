<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="valor" required="false" %>
<%-- Código de asistencia: AP, AN, TJ, TI, FJ, FI · SR = sin respuesta · vacío = no aplica --%>
<% if (valor != null && !valor.isEmpty()) { %><span class="code <%= valor %>"><%= "SR".equals(valor) ? "—" : valor %></span><% } else { %><span class="muted">·</span><% } %>
