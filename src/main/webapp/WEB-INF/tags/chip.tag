<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="color" required="false" %>
<%-- Chip de nivel/color: AZUL, VERDE, AMARILLO, ROJO o vacío = "Sin nivel" --%>
<%
  String c = color == null ? "" : color;
  String cls, txt;
  switch (c) {
    case "AZUL": cls = "azul"; txt = "Azul"; break;
    case "VERDE": cls = "verde"; txt = "Verde"; break;
    case "AMARILLO": cls = "amarillo"; txt = "Amarillo"; break;
    case "ROJO": cls = "rojo"; txt = "Rojo"; break;
    default: cls = "sin"; txt = "Sin nivel";
  }
%><span class="chip <%= cls %>"><%= txt %></span>
