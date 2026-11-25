<c:set var="titulo" value="Externos del jueves"/><c:set var="menu" value="Inicio"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Taller del jueves</span><h1>Registro de externos</h1>
  <p>Para que alguien de afuera de la PUCP entre al taller, tiene que estar en la lista que RRPP le envía a la DAES.</p></div>
  <span class="star"></span></section>
<div class="card">
  <c:choose>
    <c:when test="${sem.sinTaller}"><p>Esta semana no hay taller del jueves, así que no hay registro de externos.</p></c:when>
    <c:when test="${sem.cerrado}"><p>El registro para el taller del <b>${sem.juevesTexto}</b> ya cerró. Cuando RRPP publique el de la próxima semana, te aparece en Inicio.</p></c:when>
    <c:otherwise><p>El registro para el taller del <b>${sem.juevesTexto}</b> abre el <b>${sem.publicarTexto}</b> y cierra cuando RRPP envíe la lista a la DAES (a más tardar el <b>${sem.envioTexto}</b>).
      Cuando RRPP publique el link, te aparece en Inicio.</p></c:otherwise>
  </c:choose>
  <a class="btn btn-secondary btn-sm" href="${ctx}/inicio">← Inicio</a>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
