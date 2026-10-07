/**
 * dashboard.js - Lógica SPA Interactiva y Conexión End-to-End con la API REST
 * Club de Robótica - FP-UNE
 */

document.addEventListener('DOMContentLoaded', async function () {
    'use strict';

    // 1. Verificación de Seguridad y Redirección (Guardia de Ruta)
    if (!window.Auth || !window.Auth.requireAuth()) {
        return;
    }

    // 2. Obtener datos del usuario desde la sesión / JWT
    const currentUser = window.Auth.getUser();
    const token = window.Auth.getToken();
    const claims = window.Auth.parseJwt(token);

    const userRole = (currentUser && currentUser.rol ? currentUser.rol : (claims && claims.rol ? claims.rol : 'MIEMBRO')).toUpperCase();
    const userName = (currentUser && currentUser.nombreCompleto) || (claims && claims.nombreCompleto) || 'Integrante';
    const userEmail = (currentUser && currentUser.email) || (claims && claims.sub) || '';
    const userCi = (currentUser && currentUser.ci) || (claims && claims.ci) || '';

    // Caché en memoria
    let cachePrestamos = [];
    let cacheMateriales = [];
    let cacheIntegrantes = [];
    let cacheAsistencias = [];
    let prestamoSeleccionadoId = null;

    // Inicializar Componentes de UI
    initUserInfoUI();
    initRoleAccessControl();
    initSidebarToggle();
    initSpaNavigation();
    initLogoutHandlers();
    initRfidSimulator();
    initSearchFilters();

    // Cargar datos iniciales del Dashboard
    await cargarDatosIniciales();

    /**
     * Renderiza los datos del usuario en la barra de navegación, sidebar y banner.
     */
    function initUserInfoUI() {
        const initials = userName
            .split(' ')
            .filter(Boolean)
            .map(n => n[0])
            .slice(0, 2)
            .join('')
            .toUpperCase() || 'U';

        const sidebarAvatar = document.getElementById('sidebarAvatar');
        const topbarAvatar = document.getElementById('topbarAvatar');
        const sidebarUserName = document.getElementById('sidebarUserName');
        const topbarUserName = document.getElementById('topbarUserName');
        const welcomeUserName = document.getElementById('welcomeUserName');
        const dropdownUserEmail = document.getElementById('dropdownUserEmail');

        if (sidebarAvatar) sidebarAvatar.textContent = initials;
        if (topbarAvatar) topbarAvatar.textContent = initials;
        if (sidebarUserName) sidebarUserName.textContent = userName;
        if (topbarUserName) topbarUserName.textContent = userName;
        if (welcomeUserName) welcomeUserName.textContent = userName;
        if (dropdownUserEmail) dropdownUserEmail.textContent = userEmail || `CI: ${userCi}`;

        applyRoleBadge('sidebarUserRoleBadge', userRole);
        applyRoleBadge('topbarUserRoleBadge', userRole);
        applyRoleBadge('welcomeRoleBadge', userRole);

        const currentDateElem = document.getElementById('currentDateDisplay');
        if (currentDateElem) {
            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            currentDateElem.textContent = new Date().toLocaleDateString('es-PY', options);
        }
    }

    function applyRoleBadge(elemId, role) {
        const badge = document.getElementById(elemId);
        if (!badge) return;
        badge.textContent = role;
        badge.className = 'badge';
        if (role === 'ADMIN') badge.classList.add('badge-role-ADMIN');
        else if (role === 'DIRECTIVA') badge.classList.add('badge-role-DIRECTIVA');
        else badge.classList.add('badge-role-MIEMBRO');
    }

    /**
     * Control de acceso basado en roles (RBAC).
     */
    function initRoleAccessControl() {
        const restrictedElements = document.querySelectorAll('.role-restricted, [data-allowed-roles]');
        restrictedElements.forEach(el => {
            const allowedAttr = el.getAttribute('data-allowed-roles');
            if (!allowedAttr) return;

            const allowedRoles = allowedAttr.split(',').map(r => r.trim().toUpperCase());
            const hasAccess = allowedRoles.includes(userRole);

            if (!hasAccess) {
                el.style.display = 'none';
            } else {
                el.style.removeProperty('display');
            }
        });
    }

    /**
     * Sistema de Navegación SPA: Alterna entre vistas sin recargar la página y sincroniza con la URL.
     */
    const HASH_TO_VIEW = {
        '#dashboard': 'view-dashboard',
        '#prestamos': 'view-prestamos',
        '#aprobar-prestamos': 'view-aprobar-prestamos',
        '#inventario': 'view-inventario',
        '#asistencia': 'view-asistencia',
        '#integrantes': 'view-integrantes'
    };

    const VIEW_TO_HASH = {
        'view-dashboard': '#dashboard',
        'view-prestamos': '#prestamos',
        'view-aprobar-prestamos': '#aprobar-prestamos',
        'view-inventario': '#inventario',
        'view-asistencia': '#asistencia',
        'view-integrantes': '#integrantes'
    };

    function initSpaNavigation() {
        const navItems = document.querySelectorAll('.sidebar-nav-item, [data-view]');

        navItems.forEach(item => {
            item.addEventListener('click', function (e) {
                e.preventDefault();
                const targetViewId = this.getAttribute('data-view') || HASH_TO_VIEW[this.getAttribute('href')];
                if (!targetViewId) return;

                if (VIEW_TO_HASH[targetViewId] && window.location.hash !== VIEW_TO_HASH[targetViewId]) {
                    history.pushState(null, '', VIEW_TO_HASH[targetViewId]);
                }

                switchView(targetViewId);

                // En pantallas móviles, cerrar el sidebar tras seleccionar
                const wrapper = document.getElementById('wrapper');
                if (window.innerWidth < 992 && wrapper && wrapper.classList.contains('toggled')) {
                    wrapper.classList.remove('toggled');
                }
            });
        });

        // Escuchar navegación con flechas atrás/adelante del navegador
        window.addEventListener('popstate', function () {
            const currentHash = window.location.hash || '#dashboard';
            const targetViewId = HASH_TO_VIEW[currentHash] || 'view-dashboard';
            switchView(targetViewId);
        });

        // Inicializar vista según el hash presente en la URL
        if (window.location.hash && HASH_TO_VIEW[window.location.hash]) {
            switchView(HASH_TO_VIEW[window.location.hash]);
        }
    }

    function switchView(viewId) {
        const views = document.querySelectorAll('.spa-view');
        let viewFound = false;

        views.forEach(v => {
            if (v.id === viewId) {
                v.classList.add('active-view');
                viewFound = true;
            } else {
                v.classList.remove('active-view');
            }
        });

        if (!viewFound) return;

        // Actualizar active class en links del sidebar
        const sidebarLinks = document.querySelectorAll('.sidebar-nav .nav-link');
        sidebarLinks.forEach(link => {
            if (link.getAttribute('data-view') === viewId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        // Actualizar título en topbar
        const titles = {
            'view-dashboard': 'Panel de Control',
            'view-prestamos': 'Gestión de Préstamos',
            'view-aprobar-prestamos': 'Aprobación de Préstamos (Directiva)',
            'view-inventario': 'Inventario de Materiales',
            'view-asistencia': 'Control de Asistencias IoT',
            'view-integrantes': 'Padrón de Integrantes'
        };

        const pageTitle = document.getElementById('pageTitleHeader');
        if (pageTitle && titles[viewId]) {
            pageTitle.textContent = titles[viewId];
        }

        // Carga bajo demanda al cambiar de pestaña
        if (viewId === 'view-prestamos' || viewId === 'view-aprobar-prestamos') {
            cargarPrestamos();
        } else if (viewId === 'view-inventario') {
            cargarInventario();
        } else if (viewId === 'view-asistencia') {
            cargarAsistencias();
        } else if (viewId === 'view-integrantes') {
            cargarIntegrantes();
        } else if (viewId === 'view-dashboard') {
            cargarPrestamos();
            cargarAsistencias();
            actualizarKpis();
        }
    }

    function initSidebarToggle() {
        const wrapper = document.getElementById('wrapper');
        const toggleBtn = document.getElementById('sidebarToggle');
        if (toggleBtn && wrapper) {
            toggleBtn.addEventListener('click', function (e) {
                e.preventDefault();
                wrapper.classList.toggle('toggled');
            });
        }
    }

    function initLogoutHandlers() {
        const handleLogout = (e) => {
            e.preventDefault();
            if (confirm('¿Deseas cerrar tu sesión?')) {
                window.Auth.logout();
            }
        };

        const btnTopbarLogout = document.getElementById('btnTopbarLogout');
        const btnSidebarLogout = document.getElementById('btnSidebarLogout');
        if (btnTopbarLogout) btnTopbarLogout.addEventListener('click', handleLogout);
        if (btnSidebarLogout) btnSidebarLogout.addEventListener('click', handleLogout);
    }

    /**
     * Carga inicial consolidada de datos reales desde el backend.
     */
    async function cargarDatosIniciales() {
        try {
            await Promise.allSettled([
                cargarPrestamos(),
                cargarInventario(),
                cargarAsistencias(),
                (window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA') ? cargarIntegrantes() : Promise.resolve())
            ]);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error al cargar datos iniciales:', err);
        }
    }

    function actualizarKpis() {
        const kpiPrestamos = document.getElementById('kpiPrestamosActivos');
        const kpiAsistencias = document.getElementById('kpiAsistenciasMes');
        const kpiStock = document.getElementById('kpiComponentesStock');
        const kpiIntegrantes = document.getElementById('kpiTotalIntegrantes');

        if (kpiPrestamos) {
            const pendientes = cachePrestamos.filter(p => p.estado === 'SOLICITADO').length;
            kpiPrestamos.textContent = `${cachePrestamos.length} (${pendientes} pend.)`;
        }

        if (kpiAsistencias) {
            kpiAsistencias.textContent = `${cacheAsistencias.length} registros`;
        }

        if (kpiStock) {
            const totalStock = cacheMateriales.reduce((acc, m) => acc + (m.cantidadTotal || 0), 0);
            kpiStock.textContent = `${totalStock} unidades`;
        }

        if (kpiIntegrantes) {
            kpiIntegrantes.textContent = `${cacheIntegrantes.length || 3} miembros`;
        }

        const pendingBadge = document.getElementById('pendingLoansBadge');
        if (pendingBadge) {
            const pend = cachePrestamos.filter(p => p.estado === 'SOLICITADO').length;
            pendingBadge.textContent = pend;
            pendingBadge.style.display = pend > 0 ? 'inline-block' : 'none';
        }
    }

    // ========================================================
    // 1. CARGA Y GESTIÓN DE PRÉSTAMOS
    // ========================================================
    async function cargarPrestamos() {
        try {
            const res = await window.Auth.authFetch('/prestamos', { method: 'GET' });
            if (!res.ok) throw new Error('No se pudo obtener el listado de préstamos.');
            cachePrestamos = await res.json();
            renderTablaPrestamos(cachePrestamos);
            renderTablaAprobaciones(cachePrestamos);
            renderDashboardResumenPrestamos(cachePrestamos);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando préstamos:', err);
            renderErrorEnTabla('tablaPrestamosCompleta', 7, 'Error al conectar con /api/prestamos.');
            renderErrorEnTabla('tablaAprobarPrestamos', 7, 'Error al conectar con /api/prestamos.');
            renderErrorEnTabla('tablaDashboardPrestamos', 4, 'Error al conectar con /api/prestamos.');
        }
    }

    function renderDashboardResumenPrestamos(prestamos) {
        const tbody = document.getElementById('tablaDashboardPrestamos');
        if (!tbody) return;

        if (!prestamos || prestamos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center py-3 text-muted">No hay préstamos activos registrados.</td></tr>`;
            return;
        }

        const ultimos = prestamos.slice(0, 5);
        tbody.innerHTML = ultimos.map(p => {
            const badgeClass = getBadgeEstadoPrestamo(p.estado);
            const matCount = p.detalles ? p.detalles.length : 0;
            const matTexto = matCount > 0 ? `${matCount} material(es)` : 'General';
            return `
                <tr>
                    <td class="ps-4 fw-bold text-primary">#${p.prestamoId}</td>
                    <td>
                        <span class="fw-semibold d-block">${escapeHtml(p.nombreIntegrante || 'Miembro')}</span>
                    </td>
                    <td><small class="badge bg-light text-dark border">${matTexto}</small></td>
                    <td><span class="badge ${badgeClass} py-1 px-2">${p.estado}</span></td>
                </tr>
            `;
        }).join('');
    }

    function renderTablaPrestamos(prestamos) {
        const tbody = document.getElementById('tablaPrestamosCompleta');
        if (!tbody) return;

        if (!prestamos || prestamos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No hay préstamos registrados en la base de datos.</td></tr>`;
            return;
        }

        const canApprove = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');

        tbody.innerHTML = prestamos.map(p => {
            const badgeClass = getBadgeEstadoPrestamo(p.estado);
            const materialesText = p.detalles && p.detalles.length > 0
                ? p.detalles.map(d => `${escapeHtml(d.nombreMaterial)} (x${d.cantidad})`).join(', ')
                : (p.descripcion || 'Sin detalle de materiales');

            let actionHtml = '';
            if (canApprove && p.estado === 'SOLICITADO') {
                actionHtml = `<button class="btn btn-sm btn-warning text-dark fw-semibold btn-modal-aprobar" data-id="${p.prestamoId}"><i class="bi bi-check-circle me-1"></i>Aprobar</button>`;
            } else if (p.estado === 'SOLICITADO') {
                actionHtml = `<span class="badge bg-light text-muted border">En Espera</span>`;
            } else {
                actionHtml = `<span class="badge bg-light text-success border"><i class="bi bi-check2"></i> Procesado</span>`;
            }

            return `
                <tr id="row-prestamo-${p.prestamoId}">
                    <td class="ps-4 fw-bold text-primary">#${p.prestamoId}</td>
                    <td>
                        <span class="fw-semibold d-block">${escapeHtml(p.nombreIntegrante || 'Miembro')}</span>
                        <small class="text-muted">${escapeHtml(p.emailIntegrante || '')}</small>
                    </td>
                    <td><span class="badge bg-light text-dark border">${escapeHtml(p.nombreProyecto || 'Préstamo General')}</span></td>
                    <td><div class="small fw-medium">${materialesText}</div></td>
                    <td><small class="text-muted"><i class="bi bi-calendar-event me-1"></i>${p.fechaDevolucionPrevista ? p.fechaDevolucionPrevista.substring(0, 10) : 'Pendiente'}</small></td>
                    <td><span class="badge ${badgeClass} py-1 px-2">${p.estado}</span></td>
                    <td class="text-end pe-4">${actionHtml}</td>
                </tr>
            `;
        }).join('');

        attachModalAprobarListeners();
    }

    function renderTablaAprobaciones(prestamos) {
        const tbody = document.getElementById('tablaAprobarPrestamos');
        if (!tbody) return;

        const solicitados = prestamos.filter(p => p.estado === 'SOLICITADO');

        if (solicitados.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-success"><i class="bi bi-check-circle me-1"></i> No hay solicitudes pendientes de aprobación en este momento.</td></tr>`;
            return;
        }

        tbody.innerHTML = solicitados.map(p => {
            const materialesText = p.detalles && p.detalles.length > 0
                ? p.detalles.map(d => `<span class="badge bg-light text-dark border me-1">${escapeHtml(d.nombreMaterial)}: <strong>${d.cantidad}</strong></span>`).join(' ')
                : escapeHtml(p.descripcion || 'Sin detalle');

            return `
                <tr>
                    <td class="ps-4 fw-bold text-primary">#${p.prestamoId}</td>
                    <td><span class="fw-semibold">${escapeHtml(p.nombreIntegrante)}</span></td>
                    <td>${escapeHtml(p.nombreProyecto || 'N/A')}</td>
                    <td>${materialesText}</td>
                    <td><small class="text-muted">${p.fechaDevolucionPrevista ? p.fechaDevolucionPrevista.substring(0, 10) : 'A definir'}</small></td>
                    <td><span class="badge bg-warning text-dark py-1 px-2">SOLICITADO</span></td>
                    <td class="text-end pe-4">
                        <button class="btn btn-sm btn-success fw-semibold btn-modal-aprobar" data-id="${p.prestamoId}">
                            <i class="bi bi-shield-check me-1"></i> Aprobar con Bloqueo Pesimista
                        </button>
                    </td>
                </tr>
            `;
        }).join('');

        attachModalAprobarListeners();
    }

    function attachModalAprobarListeners() {
        const buttons = document.querySelectorAll('.btn-modal-aprobar');
        const modalEl = document.getElementById('modalAprobarPrestamo');
        const modalText = document.getElementById('modalPrestamoIdText');
        const btnConfirm = document.getElementById('btnConfirmarAprobacion');

        if (!modalEl || !buttons.length) return;
        const modalInstance = new bootstrap.Modal(modalEl);

        buttons.forEach(btn => {
            btn.onclick = function () {
                prestamoSeleccionadoId = this.getAttribute('data-id');
                if (modalText) modalText.textContent = `#${prestamoSeleccionadoId}`;
                modalInstance.show();
            };
        });

        if (btnConfirm) {
            btnConfirm.onclick = async function () {
                if (!prestamoSeleccionadoId) return;

                btnConfirm.disabled = true;
                btnConfirm.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Ejecutando Bloqueo...`;

                try {
                    const res = await window.Auth.authFetch(`/prestamos/${prestamoSeleccionadoId}/aprobar`, {
                        method: 'POST',
                        body: JSON.stringify({
                            prestamoId: Number(prestamoSeleccionadoId),
                            observacion: 'Aprobación autorizada desde el Panel Directivo'
                        })
                    });

                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        const errMsg = (data && data.message) ? data.message : 'Error al procesar la aprobación.';
                        showNotification(errMsg, 'danger');
                    } else {
                        showNotification(`¡Préstamo #${prestamoSeleccionadoId} aprobado exitosamente con LockModeType.PESSIMISTIC_WRITE en MySQL!`, 'success');
                        modalInstance.hide();
                        await cargarPrestamos();
                    }
                } catch (e) {
                    showNotification('Error de comunicación con el backend.', 'danger');
                } finally {
                    btnConfirm.disabled = false;
                    btnConfirm.innerHTML = `<i class="bi bi-check-lg me-1"></i> Aprobar Préstamo`;
                }
            };
        }
    }

    function getBadgeEstadoPrestamo(estado) {
        if (estado === 'SOLICITADO') return 'bg-warning text-dark';
        if (estado === 'APROBADO') return 'bg-info text-dark';
        if (estado === 'ENTREGADO') return 'bg-primary text-white';
        if (estado === 'DEVUELTO') return 'bg-success text-white';
        return 'bg-secondary text-white';
    }

    // ========================================================
    // 2. CARGA DE INVENTARIO Y STOCK
    // ========================================================
    async function cargarInventario() {
        try {
            const res = await window.Auth.authFetch('/materiales', { method: 'GET' });
            if (!res.ok) throw new Error('Error al listar materiales.');
            cacheMateriales = await res.json();
            renderTablaInventario(cacheMateriales);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando inventario:', err);
            renderErrorEnTabla('tablaInventario', 7, 'Error al conectar con /api/materiales.');
        }
    }

    function renderTablaInventario(materiales) {
        const tbody = document.getElementById('tablaInventario');
        if (!tbody) return;

        if (!materiales || materiales.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No hay componentes en el inventario.</td></tr>`;
            return;
        }

        tbody.innerHTML = materiales.map(m => `
            <tr>
                <td class="ps-4 fw-bold text-secondary">#${m.id}</td>
                <td>
                    <span class="fw-semibold text-dark">${escapeHtml(m.nombre)}</span>
                    <small class="text-muted d-block" style="font-size:0.75rem;">${escapeHtml(m.descripcion || '')}</small>
                </td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(m.categoriaNombre || 'General')}</span></td>
                <td>${escapeHtml(m.marca || '-')} / ${escapeHtml(m.modelo || '-')}</td>
                <td>
                    <span class="badge ${m.cantidadTotal > 0 ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-danger-subtle text-danger'} px-2 py-1 fs-6">
                        ${m.cantidadTotal} un.
                    </span>
                </td>
                <td><small class="text-muted"><i class="bi bi-geo-alt me-1"></i>${escapeHtml(m.ubicacion || 'Laboratorio')}</small></td>
                <td><span class="badge bg-success py-1 px-2">${m.estado}</span></td>
            </tr>
        `).join('');
    }

    // ========================================================
    // 3. CONTROL DE ASISTENCIA Y SIMULADOR RFID
    // ========================================================
    async function cargarAsistencias() {
        try {
            const res = await window.Auth.authFetch('/asistencia', { method: 'GET' });
            if (!res.ok) throw new Error('Error al listar asistencias.');
            cacheAsistencias = await res.json();
            renderTablaAsistencias(cacheAsistencias);
            renderDashboardResumenAsistencias(cacheAsistencias);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando asistencias:', err);
            renderErrorEnTabla('tablaAsistencias', 5, 'Error al conectar con /api/asistencia.');
            renderErrorEnTabla('tablaDashboardAsistencias', 3, 'Error al conectar con /api/asistencia.');
        }
    }

    function renderDashboardResumenAsistencias(asistencias) {
        const tbody = document.getElementById('tablaDashboardAsistencias');
        if (!tbody) return;

        if (!asistencias || asistencias.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-muted">Aún no hay marcaciones registradas hoy.</td></tr>`;
            return;
        }

        const ultimas = [...asistencias].slice(-5).reverse();
        tbody.innerHTML = ultimas.map(a => {
            const badgeClass = a.tipo === 'ENTRADA' ? 'bg-success' : 'bg-primary';
            const horaFormateada = a.fechaHora ? new Date(a.fechaHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
            return `
                <tr>
                    <td class="ps-3"><span class="fw-semibold">${escapeHtml(a.integranteNombre)}</span></td>
                    <td><small class="text-muted">${horaFormateada}</small></td>
                    <td><span class="badge ${badgeClass} py-1 px-2">${a.tipo}</span></td>
                </tr>
            `;
        }).join('');
    }

    function renderTablaAsistencias(asistencias) {
        const tbody = document.getElementById('tablaAsistencias');
        if (!tbody) return;

        if (!asistencias || asistencias.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Aún no hay marcaciones registradas hoy. ¡Prueba el simulador arriba!</td></tr>`;
            return;
        }

        tbody.innerHTML = asistencias.map(a => {
            const badgeClass = a.tipo === 'ENTRADA' ? 'bg-success' : 'bg-primary';
            const horaFormateada = a.fechaHora ? new Date(a.fechaHora).toLocaleString('es-PY') : '-';

            return `
                <tr>
                    <td class="ps-4 text-muted small">#${a.id}</td>
                    <td><span class="fw-semibold">${escapeHtml(a.integranteNombre)}</span></td>
                    <td><small class="text-muted"><i class="bi bi-clock me-1"></i>${horaFormateada}</small></td>
                    <td><span class="badge ${badgeClass} py-1 px-2">${a.tipo}</span></td>
                    <td><code class="text-secondary">${escapeHtml(a.dispositivo || 'ESP32')}</code></td>
                </tr>
            `;
        }).join('');
    }

    function initRfidSimulator() {
        const form = document.getElementById('formMarcacionRfid');
        const inputNfc = document.getElementById('inputNfcUid');
        const inputDisp = document.getElementById('inputDispositivo');
        const btnMarcar = document.getElementById('btnMarcarRfid');
        const resultadoDiv = document.getElementById('resultadoMarcacion');

        if (!form) return;

        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            const nfcUid = inputNfc ? inputNfc.value.trim() : '';
            const dispositivo = inputDisp ? inputDisp.value.trim() : 'ESP32_LAB';

            if (!nfcUid) {
                alert('Ingresa el UID de la tarjeta RFID.');
                return;
            }

            btnMarcar.disabled = true;
            btnMarcar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Procesando lectura RFID...`;

            try {
                const res = await window.Auth.authFetch('/asistencia/marcar', {
                    method: 'POST',
                    body: JSON.stringify({ nfcUid, dispositivo })
                });

                const data = await res.json().catch(() => null);

                if (!res.ok) {
                    const msg = (data && data.message) ? data.message : 'Error al registrar marcación RFID.';
                    mostrarResultadoRfid(false, msg, resultadoDiv);
                } else {
                    const tipo = data.tipo || 'ENTRADA';
                    const nombre = data.integranteNombre || 'Integrante';
                    const hora = data.fechaHora ? new Date(data.fechaHora).toLocaleTimeString() : new Date().toLocaleTimeString();
                    mostrarResultadoRfid(true, `¡Marcación de <strong>${tipo}</strong> exitosa para <strong>${escapeHtml(nombre)}</strong> a las ${hora}!`, resultadoDiv);
                    await cargarAsistencias();
                }
            } catch (err) {
                mostrarResultadoRfid(false, 'No se pudo conectar con el endpoint /api/asistencia/marcar.', resultadoDiv);
            } finally {
                btnMarcar.disabled = false;
                btnMarcar.innerHTML = `<i class="bi bi-send me-1"></i> Enviar Marcación`;
            }
        });
    }

    function mostrarResultadoRfid(isSuccess, message, container) {
        if (!container) return;
        container.style.display = 'block';
        const alertClass = isSuccess ? 'alert-success' : 'alert-danger';
        const icon = isSuccess ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill';

        container.innerHTML = `
            <div class="alert ${alertClass} alert-dismissible fade show d-flex align-items-center mb-0 py-2" role="alert">
                <i class="bi ${icon} me-2 fs-5"></i>
                <div>${message}</div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
    }

    // ========================================================
    // 4. PADRÓN DE INTEGRANTES (SOLO ADMIN)
    // ========================================================
    async function cargarIntegrantes() {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) return;

        try {
            const res = await window.Auth.authFetch('/integrantes', { method: 'GET' });
            if (!res.ok) throw new Error('Error al listar integrantes.');
            cacheIntegrantes = await res.json();
            renderTablaIntegrantes(cacheIntegrantes);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando integrantes:', err);
            renderErrorEnTabla('tablaIntegrantes', 8, 'Error al conectar con /api/integrantes.');
        }
    }

    function renderTablaIntegrantes(integrantes) {
        const tbody = document.getElementById('tablaIntegrantes');
        if (!tbody) return;

        if (!integrantes || integrantes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No hay integrantes registrados.</td></tr>`;
            return;
        }

        tbody.innerHTML = integrantes.map(i => {
            let roleBadge = 'bg-secondary';
            if (i.rol === 'ADMIN') roleBadge = 'badge-role-ADMIN';
            else if (i.rol === 'DIRECTIVA') roleBadge = 'badge-role-DIRECTIVA';
            else roleBadge = 'badge-role-MIEMBRO';

            return `
                <tr>
                    <td class="ps-4 fw-bold text-secondary">#${i.id}</td>
                    <td>
                        <span class="fw-semibold text-dark">${escapeHtml(i.nombreCompleto)}</span>
                        <small class="text-muted d-block">${escapeHtml(i.email)}</small>
                    </td>
                    <td><code>${escapeHtml(i.ci)}</code></td>
                    <td><small class="text-muted">${escapeHtml(i.carnetUniversitario || '-')}</small></td>
                    <td><small class="badge bg-light text-dark border">${escapeHtml(i.carrera || '-')}</small></td>
                    <td><span class="badge ${roleBadge} py-1 px-2">${i.rol}</span></td>
                    <td><code class="text-primary">${escapeHtml(i.nfcUid || 'No asignado')}</code></td>
                    <td><span class="badge bg-success py-1 px-2">${i.estado}</span></td>
                </tr>
            `;
        }).join('');
    }

    // ========================================================
    // BÚSQUEDA Y FILTRADO EN TABLAS
    // ========================================================
    function initSearchFilters() {
        const inputMat = document.getElementById('inputBuscarMaterial');
        if (inputMat) {
            inputMat.addEventListener('input', function () {
                const term = this.value.toLowerCase();
                const filtrados = cacheMateriales.filter(m =>
                    m.nombre.toLowerCase().includes(term) ||
                    (m.categoriaNombre && m.categoriaNombre.toLowerCase().includes(term))
                );
                renderTablaInventario(filtrados);
            });
        }

        const inputInt = document.getElementById('inputBuscarIntegrante');
        if (inputInt) {
            inputInt.addEventListener('input', function () {
                const term = this.value.toLowerCase();
                const filtrados = cacheIntegrantes.filter(i =>
                    i.nombreCompleto.toLowerCase().includes(term) ||
                    i.ci.toLowerCase().includes(term) ||
                    (i.carrera && i.carrera.toLowerCase().includes(term))
                );
                renderTablaIntegrantes(filtrados);
            });
        }

        // Botones de recarga
        const btnRecargarPrestamos = document.getElementById('btnRecargarPrestamos');
        const btnRecargarAprobaciones = document.getElementById('btnRecargarAprobaciones');
        const btnRecargarInventario = document.getElementById('btnRecargarInventario');
        const btnRecargarAsistencias = document.getElementById('btnRecargarAsistencias');
        const btnRecargarIntegrantes = document.getElementById('btnRecargarIntegrantes');

        if (btnRecargarPrestamos) btnRecargarPrestamos.onclick = () => cargarPrestamos();
        if (btnRecargarAprobaciones) btnRecargarAprobaciones.onclick = () => cargarPrestamos();
        if (btnRecargarInventario) btnRecargarInventario.onclick = () => cargarInventario();
        if (btnRecargarAsistencias) btnRecargarAsistencias.onclick = () => cargarAsistencias();
        if (btnRecargarIntegrantes) btnRecargarIntegrantes.onclick = () => cargarIntegrantes();
    }

    function renderErrorEnTabla(tbodyId, colspan, msg) {
        const tbody = document.getElementById(tbodyId);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="${colspan}" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-1"></i> ${escapeHtml(msg)}</td></tr>`;
        }
    }

    function showNotification(message, type = 'info') {
        const container = document.getElementById('alertContainer');
        if (!container) return;

        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show shadow-sm`;
        alertDiv.role = 'alert';
        alertDiv.innerHTML = `
            <div class="d-flex align-items-center">
                <i class="bi ${type === 'success' ? 'bi-check-circle-fill' : type === 'danger' ? 'bi-x-circle-fill' : 'bi-info-circle-fill'} me-2"></i>
                <div>${escapeHtml(message)}</div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        container.appendChild(alertDiv);
        setTimeout(() => {
            alertDiv.classList.remove('show');
            setTimeout(() => alertDiv.remove(), 250);
        }, 5000);
    }

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});
