package edu.udb.ucacfc.shared.config;

import edu.udb.ucacfc.academico.categoria.Categoria;
import edu.udb.ucacfc.academico.categoria.CategoriaRepository;
import edu.udb.ucacfc.academico.docente.Docente;
import edu.udb.ucacfc.academico.docente.DocenteRepository;
import edu.udb.ucacfc.academico.modalidad.Modalidad;
import edu.udb.ucacfc.academico.modalidad.ModalidadRepository;
import edu.udb.ucacfc.academico.oferta.CursoService;
import edu.udb.ucacfc.academico.oferta.DiplomadoService;
import edu.udb.ucacfc.academico.oferta.dto.OfertaRequest;
import edu.udb.ucacfc.academico.oferta.dto.SesionRequest;
import edu.udb.ucacfc.catering.ServicioCatering;
import edu.udb.ucacfc.catering.ServicioCateringRepository;
import edu.udb.ucacfc.catering.TipoServicioCatering;
import edu.udb.ucacfc.cliente.Cliente;
import edu.udb.ucacfc.cliente.ClienteRepository;
import edu.udb.ucacfc.cliente.TipoCliente;
import edu.udb.ucacfc.espacio.Espacio;
import edu.udb.ucacfc.espacio.EspacioRepository;
import edu.udb.ucacfc.espacio.TipoEspacio;
import edu.udb.ucacfc.seguridad.auth.AuthService;
import edu.udb.ucacfc.seguridad.auth.dto.RegistroRequest;
import edu.udb.ucacfc.seguridad.usuario.Rol;
import edu.udb.ucacfc.seguridad.usuario.Usuario;
import edu.udb.ucacfc.seguridad.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Al arrancar:
 *  1) Si no existe NINGUN ADMIN activo, crea el administrador de app.admin.* (sin el, nadie podria configurar nada,
 *     porque el registro publico siempre crea CLIENTE).
 *  2) Si app.seed.datos-demo=true y no hay categorias, carga datos de ejemplo usando los MISMOS services
 *     (asi los datos demo cumplen todas las reglas: sesiones futuras, docentes sin cruces, espacios en agenda).
 * Nunca modifica datos existentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;
    private final ModalidadRepository modalidadRepository;
    private final DocenteRepository docenteRepository;
    private final EspacioRepository espacioRepository;
    private final ServicioCateringRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final CursoService cursoService;
    private final DiplomadoService diplomadoService;
    private final AuthService authService;
    private final Clock clock;

    @Value("${app.admin.nombre}")
    private String adminNombre;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.seed.datos-demo:true}")
    private boolean datosDemo;

    @Override
    public void run(String... args) {
        if (usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) == 0 && !usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            usuarioRepository.save(Usuario.builder().nombre(adminNombre).email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword)).rol(Rol.ADMIN).build());
            log.info("Administrador inicial creado: {} / {}", adminEmail, adminPassword);
        }
        if (datosDemo && categoriaRepository.count() == 0) {
            try {
                cargarDemo();
                log.info("Datos de ejemplo cargados");
            } catch (RuntimeException e) {
                log.warn("No se pudieron cargar todos los datos de ejemplo: {}", e.getMessage());
            }
        }
    }

    private void cargarDemo() {
        usuario("Maria Recepcion", "recepcion@ucacfc.edu.sv", "Recepcion2026", Rol.RECEPCIONISTA);
        usuario("Jorge Contabilidad", "contabilidad@ucacfc.edu.sv", "Conta2026", Rol.CONTABILIDAD);

        Categoria tec = categoriaRepository.save(Categoria.builder().nombre("Tecnologias de la informacion")
                .descripcion("Programacion, redes y ciberseguridad").build());
        Categoria ges = categoriaRepository.save(Categoria.builder().nombre("Gestion empresarial")
                .descripcion("Liderazgo, finanzas y proyectos").build());
        categoriaRepository.save(Categoria.builder().nombre("Idiomas").descripcion("Ingles y frances").build());

        Modalidad pres = modalidadRepository.save(Modalidad.builder().nombre("PRESENCIAL").requiereEspacioFisico(true).build());
        Modalidad virt = modalidadRepository.save(Modalidad.builder().nombre("VIRTUAL").requiereEspacioFisico(false).build());
        modalidadRepository.save(Modalidad.builder().nombre("HIBRIDA").requiereEspacioFisico(true).build());

        Docente d1 = docenteRepository.save(Docente.builder().nombre("Ing. Maria Lopez").especialidad("Desarrollo web con Java")
                .correo("mlopez@ucacfc.edu.sv").telefono("7123-4567").build());
        Docente d2 = docenteRepository.save(Docente.builder().nombre("Lic. Carlos Rivas").especialidad("Gestion de proyectos")
                .correo("crivas@ucacfc.edu.sv").telefono("7234-5678").build());
        Docente d3 = docenteRepository.save(Docente.builder().nombre("Ing. Sofia Martinez").especialidad("Ciberseguridad")
                .correo("smartinez@ucacfc.edu.sv").telefono("7345-6789").build());

        Espacio lab = espacioRepository.save(Espacio.builder().nombre("Laboratorio de computo 1").tipo(TipoEspacio.LABORATORIO)
                .capacidad(25).precioHora(new BigDecimal("40.00")).equipamiento("25 computadoras, proyector")
                .ubicacion("Edificio CFC, nivel 2").build());
        Espacio aula = espacioRepository.save(Espacio.builder().nombre("Aula 101").tipo(TipoEspacio.AULA).capacidad(35)
                .precioHora(new BigDecimal("25.00")).equipamiento("Pizarra, proyector").ubicacion("Edificio CFC, nivel 1").build());
        espacioRepository.save(Espacio.builder().nombre("Auditorio principal").tipo(TipoEspacio.AUDITORIO).capacidad(200)
                .precioHora(new BigDecimal("90.00")).equipamiento("Sonido, proyector, tarima").ubicacion("Edificio CFC").build());
        espacioRepository.save(Espacio.builder().nombre("Sala de reuniones A").tipo(TipoEspacio.SALA_REUNIONES).capacidad(12)
                .precioHora(new BigDecimal("20.00")).equipamiento("Pantalla, videoconferencia").ubicacion("Nivel 3").build());
        espacioRepository.save(Espacio.builder().nombre("Sala multimedia").tipo(TipoEspacio.SALA_MULTIMEDIA).capacidad(40)
                .precioHora(new BigDecimal("50.00")).equipamiento("Camaras, consola de audio").ubicacion("Nivel 2").build());

        servicio(TipoServicioCatering.COFFEE_BREAK, "Coffee break clasico", "Cafe, te, galletas y fruta", "3.50");
        servicio(TipoServicioCatering.DESAYUNO, "Desayuno tipico", "Huevos, frijoles, platano, queso, cafe", "5.75");
        servicio(TipoServicioCatering.ALMUERZO, "Almuerzo ejecutivo", "Plato fuerte, ensalada, postre y bebida", "8.50");
        servicio(TipoServicioCatering.CENA, "Cena de gala", "Entrada, plato fuerte y postre", "15.00");
        servicio(TipoServicioCatering.REFRIGERIO, "Refrigerio", "Sandwich, jugo y fruta", "3.00");

        authService.registrar(new RegistroRequest("Ana Lucia Perez", "ana.perez@correo.com", "Cliente2026",
                TipoCliente.PERSONA, "01234567-8", null, "7456-7890", "Colonia Escalon, San Salvador"));
        clienteRepository.save(Cliente.builder().tipo(TipoCliente.EMPRESA).duiNit("0614-010190-101-3")
                .nombre("Carlos Mendez").empresa("Inversiones Delta, S.A. de C.V.").correo("rrhh@delta.com.sv")
                .telefono("2250-1000").direccion("Boulevard Los Proceres, San Salvador").build());

        LocalDate lunes = LocalDate.now(clock).plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        cursoService.crear(new OfertaRequest("Spring Boot para empresas", "APIs REST con seguridad JWT", tec.getId(),
                pres.getId(), d1.getId(), lab.getId(), 25, new BigDecimal("150.00"),
                semanal(lunes, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), LocalTime.of(20, 0), 4)));
        cursoService.crear(new OfertaRequest("Fundamentos de ciberseguridad", "Respuesta a incidentes", tec.getId(),
                virt.getId(), d3.getId(), null, 40, new BigDecimal("95.00"),
                semanal(lunes, List.of(DayOfWeek.TUESDAY), LocalTime.of(19, 0), LocalTime.of(21, 0), 4)));
        diplomadoService.crear(new OfertaRequest("Diplomado en gestion de proyectos", "Metodologias PMI y agiles",
                ges.getId(), pres.getId(), d2.getId(), aula.getId(), 30, new BigDecimal("450.00"),
                semanal(lunes, List.of(DayOfWeek.SATURDAY), LocalTime.of(8, 0), LocalTime.of(12, 0), 6)));
    }

    private List<SesionRequest> semanal(LocalDate desde, List<DayOfWeek> dias, LocalTime hi, LocalTime hf, int semanas) {
        List<SesionRequest> s = new ArrayList<>();
        for (int w = 0; w < semanas; w++) {
            for (DayOfWeek d : dias) {
                LocalDate f = desde.plusWeeks(w).with(TemporalAdjusters.nextOrSame(d));
                s.add(new SesionRequest(f.atTime(hi), f.atTime(hf)));
            }
        }
        return s;
    }

    private void usuario(String nombre, String email, String pass, Rol rol) {
        if (!usuarioRepository.existsByEmailIgnoreCase(email)) {
            usuarioRepository.save(Usuario.builder().nombre(nombre).email(email)
                    .password(passwordEncoder.encode(pass)).rol(rol).build());
        }
    }

    private void servicio(TipoServicioCatering tipo, String nombre, String menu, String precio) {
        servicioRepository.save(ServicioCatering.builder().tipo(tipo).nombre(nombre).menuBase(menu)
                .precioUnitario(new BigDecimal(precio)).build());
    }
}
