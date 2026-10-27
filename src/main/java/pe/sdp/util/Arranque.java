package pe.sdp.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pe.sdp.daos.ArranqueDao;
import pe.sdp.daos.BaseDao;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.logging.Logger;

/** Lo que pasa al prender y al apagar la aplicación. */
@WebListener
public class Arranque implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger("SDP");

    @Override
    public void contextInitialized(ServletContextEvent e) {
        LOG.info("SDP · entorno: " + (Config.produccion() ? "PRODUCCIÓN" : "desarrollo") + " · HTTPS obligatorio: " + Config.forzarHttps());
        ArranqueDao dao = new ArranqueDao();
        if (!dao.bdResponde()) {
            LOG.severe("SDP · No se pudo conectar a la base de datos. Revisa DB_URL / DB_USER / DB_PASSWORD.");
            return;
        }
        try {
            String correo = Config.get("SDP_ADMIN_CORREO", null, null);
            String clave = Config.get("SDP_ADMIN_CLAVE", null, null);
            if (correo != null && clave != null) {
                if (clave.length() < 8) {
                    LOG.warning("SDP · SDP_ADMIN_CLAVE debe tener al menos 8 caracteres; no se activó el admin inicial.");
                } else {
                    String nombre = dao.activarAdminInicial(correo, Password.hash(clave));
                    if (nombre != null) LOG.info("SDP · Admin inicial activado: " + nombre + " <" + correo + ">. Cambiará la contraseña al entrar.");
                }
            }
            int prueba = dao.cuentasDePrueba();
            if (prueba > 0 && Config.produccion())
                LOG.warning("SDP · Hay " + prueba + " cuentas de prueba (@sdp.test) en la BD. En producción están bloqueadas, pero mejor bórralas.");
        } catch (Exception ex) {
            LOG.warning("SDP · Revisión de arranque incompleta: " + ex.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        BaseDao.cerrarPool();
        // Suelta el driver de MySQL para que Tomcat no se quede con memoria al redesplegar
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            if (d.getClass().getClassLoader() == cl) {
                try { DriverManager.deregisterDriver(d); } catch (Exception ignored) { }
            }
        }
        try {   // hilo de limpieza del conector de MySQL (por reflexión: así compila aunque se use otro driver)
            Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread").getMethod("checkedShutdown").invoke(null);
        } catch (Throwable ignored) { }
    }
}
