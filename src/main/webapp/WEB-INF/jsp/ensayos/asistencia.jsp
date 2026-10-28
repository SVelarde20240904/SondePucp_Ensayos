<c:set var="titulo" value="Tomar asistencia"/><c:set var="menu" value="Tomar asistencia"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/ensayos/asistencia" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<input type="hidden" name="idEnsayo" value="${ensayo.idEnsayo}">
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>Asistencia · ${ensayo.diaLargo} ${ensayo.fechaCorta}</h1>
  <p>Marca cómo llegó cada persona. Quien no confirmó también puede marcarse.</p></div>
  <div class="actions"><button class="btn btn-primary" type="submit">Guardar asistencia</button></div><span class="star"></span></section>

<div class="legend"><span><sdp:code valor="AP"/> Asistencia puntual</span><span><sdp:code valor="AN"/> No planeada (dijo que no, pero vino)</span><span><sdp:code valor="TJ"/> Tardanza justificada</span>
  <span><sdp:code valor="TI"/> Tardanza injustificada</span><span><sdp:code valor="FJ"/> Falta justificada</span><span><sdp:code valor="FI"/> Falta injustificada</span>
  <span class="c-amarillo bold">“Sin respuesta” = no marcó la encuesta</span></div>

<div class="card"><h2>Miembros</h2><p>Primero los que confirmaron · ${ensayo.confirmados} confirmados, ${ensayo.sinRespuesta} sin respuesta</p>
  <table class="table cards-compact"><thead><tr><th>Miembro</th><th>Rol</th><th>Nivel</th><th>Confirmó</th><th>Asistencia real</th></tr></thead><tbody>
  <c:forEach var="a" items="${filas}">
    <tr><td><c:out value="${a.miembro}"/><c:if test="${a.equipoEnsayos}"> <span class="tag info">ENS</span></c:if><c:if test="${not empty a.motivo}"><small>Motivo: <c:out value="${a.motivo}"/></small></c:if></td>
      <td class="no-label"><span class="tag info role">${a.rolTexto}</span></td><td class="no-label"><sdp:chip color="${a.nivel}"/></td>
      <td><sdp:tag texto="${a.confirmacion == 'SI' ? 'Sí' : (a.confirmacion == 'NO' ? 'No' : 'Sin respuesta')}" tono="${a.confirmacion == 'SI' ? 'ok' : (a.confirmacion == 'NO' ? 'neutral' : 'warn')}"/></td>
      <td class="no-label"><div class="seg">
        <c:forEach var="c" items="${['AP','AN','TJ','TI','FJ','FI']}">
          <label class="${c == 'AN' ? 'AP' : c}"><input type="radio" name="asis_${a.idMiembro}" value="${c}" ${a.estadoReal == c ? 'checked' : ''}><span>${c}</span></label>
        </c:forEach>
        <label class="none"><input type="radio" name="asis_${a.idMiembro}" value="" ${empty a.estadoReal ? 'checked' : ''}><span title="Sin marcar">·</span></label>
      </div></td></tr>
  </c:forEach>
  </tbody></table>
</div>
<div class="inline" style="justify-content:flex-end"><button class="btn btn-primary" type="submit">Guardar asistencia</button></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
