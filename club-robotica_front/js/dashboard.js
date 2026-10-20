/**
 * dashboard.js - Logica SPA Interactiva y Conexion End-to-End con la API REST (CRUD Completo)
 * Club de Robotica - FP-UNE
 */

/**
 * Utilidad global de escape seguro contra inyecciones XSS
 */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
if (typeof window !== 'undefined') {
    window.escapeHtml = escapeHtml;
}

async function initDashboard() {
    'use strict';

    // 1. Verificacion de Seguridad y Redireccion (Guardia de Ruta)
    if (!window.Auth || !window.Auth.requireAuth()) {
        return;
    }

    // 2. Limpieza preventiva de backdrops u overlays residuales tras recarga
    try {
        document.body.classList.remove('modal-open');
        document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    } catch (e) {
        console.warn('[Dashboard] Error al limpiar backdrops previos:', e);
    }

    // 3. Obtener datos del usuario desde la sesion / JWT
    let currentUser = window.Auth.getUser();
    let token = window.Auth.getToken();
    let claims = window.Auth.parseJwt(token);

    let userRole = (currentUser && currentUser.rol ? currentUser.rol : (claims && claims.rol ? claims.rol : 'MIEMBRO')).toUpperCase();
    let userName = (currentUser && currentUser.nombreCompleto) || (claims && claims.nombreCompleto) || 'Integrante';
    let userEmail = (currentUser && currentUser.email) || (claims && claims.sub) || '';
    let userCi = (currentUser && currentUser.ci) || (claims && claims.ci) || '';
    let userId = (currentUser && currentUser.id) || (claims && claims.integranteId) || null;

    // Cache en memoria
    let cachePrestamos = [];
    let cacheMateriales = [];
    let cacheIntegrantes = [];
    let cacheAsistencias = [];
    let cacheCategorias = [];
    let cacheProyectos = [];
    let cacheHorasDashboard = null;

    let prestamoSeleccionadoId = null;
    let accionEliminarPendiente = null;
    let isInitialLoading = true;

    // Instancias de Modales Bootstrap con proteccion defensiva
    const modales = {};
    function getModal(id) {
        const el = document.getElementById(id);
        if (!el || typeof bootstrap === 'undefined' || !bootstrap.Modal) return null;
        if (!modales[id]) {
            try {
                modales[id] = (bootstrap.Modal.getOrCreateInstance)
                    ? bootstrap.Modal.getOrCreateInstance(el)
                    : (bootstrap.Modal.getInstance(el) || new bootstrap.Modal(el));
            } catch (err) {
                console.warn(`[Dashboard] No se pudo inicializar modal #${id}:`, err);
                return null;
            }
        }
        return modales[id];
    }

    // Inicializar Componentes de UI protegidos individualmente
    try { initUserInfoUI(); } catch (e) { console.error('[Dashboard] Error en initUserInfoUI:', e); }
    try { initRoleAccessControl(); } catch (e) { console.error('[Dashboard] Error en initRoleAccessControl:', e); }
    try { initSidebarToggle(); } catch (e) { console.error('[Dashboard] Error en initSidebarToggle:', e); }
    try { initSpaNavigation(); } catch (e) { console.error('[Dashboard] Error en initSpaNavigation:', e); }
    try { initLogoutHandlers(); } catch (e) { console.error('[Dashboard] Error en initLogoutHandlers:', e); }
    try { initMarcadorManual(); } catch (e) { console.error('[Dashboard] Error en initMarcadorManual:', e); }
    try { initRfidCaptura(); } catch (e) { console.error('[Dashboard] Error en initRfidCaptura:', e); }
    try { initSearchFilters(); } catch (e) { console.error('[Dashboard] Error en initSearchFilters:', e); }
    try { initSearchablePrestamoPickers(); } catch (e) { console.error('[Dashboard] Error en initSearchablePrestamoPickers:', e); }
    try { initCrudForms(); } catch (e) { console.error('[Dashboard] Error en initCrudForms:', e); }
    try { initChatbotIa(); } catch (e) { console.error('[Dashboard] Error en initChatbotIa:', e); }

    // Cargar datos iniciales del Dashboard
    try {
        await cargarDatosIniciales();
    } catch (e) {
        console.error('[Dashboard] Error en carga inicial:', e);
    } finally {
        isInitialLoading = false;
    }

    /**
     * Renderiza los datos del usuario en la barra de navegacion, sidebar y banner.
     */
    function initUserInfoUI() {
        const initials = userName
            .split(' ')
            .filter(Boolean)
            .map(n => n[0])
            .slice(0, 2)
            .join('')
            .toUpperCase() || 'U';

        const topbarAvatar = document.getElementById('topbarAvatar');
        const topbarUserName = document.getElementById('topbarUserName');
        const welcomeUserName = document.getElementById('welcomeUserName');
        const dropdownUserEmail = document.getElementById('dropdownUserEmail');

        if (topbarAvatar) topbarAvatar.textContent = initials;
        if (topbarUserName) topbarUserName.textContent = userName;
        if (welcomeUserName) welcomeUserName.textContent = userName;
        if (dropdownUserEmail) dropdownUserEmail.textContent = userEmail || `CI: ${userCi}`;

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

        // En vistas sin autorizacion directiva, expandir la tabla de prestamos a ancho completo
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            const widgetPrestamos = document.getElementById('dashboardWidgetPrestamos');
            if (widgetPrestamos) {
                widgetPrestamos.classList.remove('col-xl-7');
                widgetPrestamos.classList.add('col-xl-12');
            }
        }
    }

    /**
     * Sistema de Navegacion SPA: Alterna entre vistas sin recargar la pagina.
     */
    const HASH_TO_VIEW = {
        '#dashboard': 'view-dashboard',
        '#prestamos': 'view-prestamos',
        '#aprobar-prestamos': 'view-aprobar-prestamos',
        '#inventario': 'view-inventario',
        '#proyectos': 'view-proyectos',
        '#asistencia': 'view-asistencia',
        '#integrantes': 'view-integrantes'
    };

    const VIEW_TO_HASH = {
        'view-dashboard': '#dashboard',
        'view-prestamos': '#prestamos',
        'view-aprobar-prestamos': '#aprobar-prestamos',
        'view-inventario': '#inventario',
        'view-proyectos': '#proyectos',
        'view-asistencia': '#asistencia',
        'view-integrantes': '#integrantes'
    };

    const RESTRICTED_VIEWS = {
        'view-asistencia': ['ADMIN', 'DIRECTIVA'],
        'view-aprobar-prestamos': ['ADMIN', 'DIRECTIVA'],
        'view-integrantes': ['ADMIN', 'DIRECTIVA']
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

                const wrapper = document.getElementById('wrapper');
                if (window.innerWidth < 992 && wrapper && wrapper.classList.contains('toggled')) {
                    wrapper.classList.remove('toggled');
                }
            });
        });

        window.addEventListener('popstate', function () {
            const currentHash = window.location.hash || '#dashboard';
            const targetViewId = HASH_TO_VIEW[currentHash] || 'view-dashboard';
            switchView(targetViewId);
        });

        if (window.location.hash && HASH_TO_VIEW[window.location.hash]) {
            switchView(HASH_TO_VIEW[window.location.hash]);
        }
    }

    function switchView(viewId) {
        // Guardia de seguridad: Impedir navegacion por URL/hash a vistas restringidas
        if (RESTRICTED_VIEWS[viewId] && !window.Auth.hasAnyRole(...RESTRICTED_VIEWS[viewId])) {
            showNotification('Acceso restringido: No tienes permisos para acceder a este módulo por seguridad y discreción de información.', 'warning');
            if (window.location.hash !== '#dashboard') {
                history.replaceState(null, '', '#dashboard');
            }
            switchView('view-dashboard');
            return;
        }

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

        const sidebarLinks = document.querySelectorAll('.sidebar-nav .nav-link');
        sidebarLinks.forEach(link => {
            if (link.getAttribute('data-view') === viewId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        // Sincronizar estado activo en la barra de navegación inferior móvil
        const mobileNavLinks = document.querySelectorAll('.mobile-bottom-nav .mobile-nav-item[data-view]');
        mobileNavLinks.forEach(link => {
            if (link.getAttribute('data-view') === viewId) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });

        if (window.innerWidth < 992) {
            closeMobileSidebar();
        }

        const titles = {
            'view-dashboard': 'Panel de Control',
            'view-prestamos': 'Gestion de Prestamos',
            'view-aprobar-prestamos': 'Aprobacion de Prestamos (Directiva)',
            'view-inventario': 'Inventario de Materiales',
            'view-proyectos': 'Gestion de Proyectos',
            'view-asistencia': 'Control de Asistencias IoT',
            'view-integrantes': 'Padron de Integrantes'
        };

        const pageTitle = document.getElementById('pageTitleHeader');
        if (pageTitle && titles[viewId]) {
            pageTitle.textContent = titles[viewId];
        }

        if (!isInitialLoading) {
            if (viewId === 'view-prestamos' || viewId === 'view-aprobar-prestamos') {
                cargarPrestamos();
                cargarMaterialesSelect();
                cargarIntegrantesSelect();
                cargarProyectosSelect();
            } else if (viewId === 'view-inventario') {
                cargarInventario();
                cargarCategorias();
            } else if (viewId === 'view-proyectos') {
                cargarProyectos();
            } else if (viewId === 'view-asistencia') {
                cargarAsistencias();
            } else if (viewId === 'view-integrantes') {
                cargarIntegrantes();
            } else if (viewId === 'view-dashboard') {
                cargarPrestamos();
                cargarAsistencias();
                actualizarKpis();
                cargarProyectos();
                cargarHorasDashboard();
            }
        }
    }

    function closeMobileSidebar() {
        const wrapper = document.getElementById('wrapper');
        if (wrapper && wrapper.classList.contains('toggled')) {
            wrapper.classList.remove('toggled');
        }
    }

    function initSidebarToggle() {
        const wrapper = document.getElementById('wrapper');
        const toggleBtn = document.getElementById('sidebarToggle');
        const closeBtn = document.getElementById('btnSidebarClose');
        const backdrop = document.getElementById('sidebarBackdrop');
        const mobileMenuBtn = document.getElementById('btnMobileDrawerOpen');

        if (toggleBtn && wrapper) {
            toggleBtn.addEventListener('click', function (e) {
                e.preventDefault();
                wrapper.classList.toggle('toggled');
            });
        }

        if (mobileMenuBtn && wrapper) {
            mobileMenuBtn.addEventListener('click', function (e) {
                e.preventDefault();
                wrapper.classList.add('toggled');
            });
        }

        if (closeBtn) {
            closeBtn.addEventListener('click', function (e) {
                e.preventDefault();
                closeMobileSidebar();
            });
        }

        if (backdrop) {
            backdrop.addEventListener('click', function (e) {
                e.preventDefault();
                closeMobileSidebar();
            });
        }
    }

    function initLogoutHandlers() {
        const handleLogout = (e) => {
            e.preventDefault();
            if (confirm('Deseas cerrar tu sesion?')) {
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
                cargarCategorias(),
                cargarProyectos(),
                (window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA') ? cargarAsistencias() : Promise.resolve()),
                (window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA') ? cargarIntegrantes() : Promise.resolve()),
                cargarHorasDashboard()
            ]);
            cargarMaterialesSelect();
            cargarIntegrantesSelect();
            cargarProyectosSelect();
            actualizarSelectCategorias();
            actualizarKpis();
            renderMisProyectosDashboard();
        } catch (err) {
            console.error('[Dashboard] Error al cargar datos iniciales:', err);
        }
    }

    function actualizarKpis() {
        const kpiPrestamos = document.getElementById('kpiPrestamosActivos');
        const kpiAsistencias = document.getElementById('kpiAsistenciasMes');
        const kpiStock = document.getElementById('kpiComponentesStock');
        const kpiIntegrantes = document.getElementById('kpiTotalIntegrantes');

        const esAdmin = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');

        if (kpiPrestamos) {
            if (!esAdmin) {
                const misPrestamos = cachePrestamos.filter(p => p.integranteId == userId);
                const misPendientes = misPrestamos.filter(p => p.estado === 'SOLICITADO').length;
                kpiPrestamos.textContent = `${misPrestamos.length} (${misPendientes} pend.)`;
            } else {
                const pendientes = cachePrestamos.filter(p => p.estado === 'SOLICITADO').length;
                kpiPrestamos.textContent = `${cachePrestamos.length} (${pendientes} pend.)`;
            }
        }

        if (kpiAsistencias && esAdmin) {
            const hoy = new Date();
            const ingresosHoy = cacheAsistencias.filter(a => {
                const tipo = a.tipoMarcacion || a.tipo || 'ENTRADA';
                if (tipo !== 'ENTRADA') return false;
                if (!a.fechaHora) return false;
                const f = new Date(a.fechaHora);
                return f.getDate() === hoy.getDate() &&
                       f.getMonth() === hoy.getMonth() &&
                       f.getFullYear() === hoy.getFullYear();
            });
            kpiAsistencias.textContent = `${ingresosHoy.length} ingresos`;
        }

        if (kpiStock) {
            const totalStock = cacheMateriales.reduce((acc, m) => acc + (m.cantidadTotal || 0), 0);
            kpiStock.textContent = `${totalStock} unidades`;
        }

        if (kpiIntegrantes && esAdmin) {
            kpiIntegrantes.textContent = `${cacheIntegrantes.length || 3} miembros`;
        }

        const pendingBadge = document.getElementById('pendingLoansBadge');
        if (pendingBadge && esAdmin) {
            const pend = cachePrestamos.filter(p => p.estado === 'SOLICITADO').length;
            pendingBadge.textContent = pend;
            pendingBadge.style.display = pend > 0 ? 'inline-block' : 'none';
        }

        // Verificar alertas de préstamos vencidos o que vencen hoy
        const alertaVencidosContainer = document.getElementById('alertaPrestamosVencidosDashboard');
        if (alertaVencidosContainer) {
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            let prestamosARevisar = esAdmin ? cachePrestamos : cachePrestamos.filter(p => p.integranteId == userId);
            const vencidos = prestamosARevisar.filter(p => {
                if (p.estado !== 'ENTREGADO' && p.estado !== 'VENCIDO') return false;
                if (!p.fechaDevolucionEstimada) return false;
                const fDev = new Date(p.fechaDevolucionEstimada);
                fDev.setHours(0, 0, 0, 0);
                return fDev < hoy;
            });

            const vencenHoy = prestamosARevisar.filter(p => {
                if (p.estado !== 'ENTREGADO') return false;
                if (!p.fechaDevolucionEstimada) return false;
                const fDev = new Date(p.fechaDevolucionEstimada);
                fDev.setHours(0, 0, 0, 0);
                return fDev.getTime() === hoy.getTime();
            });

            if (vencidos.length > 0 || vencenHoy.length > 0) {
                let msg = '';
                if (vencidos.length > 0) {
                    msg += `<strong>${vencidos.length} préstamo(s) con fecha vencida</strong> que requieren devolución inmediata. `;
                }
                if (vencenHoy.length > 0) {
                    msg += `<strong>${vencenHoy.length} préstamo(s) tienen fecha de devolución para hoy</strong>.`;
                }

                alertaVencidosContainer.innerHTML = `
                    <div class="alert alert-danger border-danger border-opacity-25 shadow-sm d-flex flex-column flex-sm-row align-items-sm-center justify-content-between p-3 mb-3 mb-md-4 rounded-3 gap-2" role="alert">
                        <div class="d-flex align-items-center gap-3">
                            <i class="bi bi-exclamation-octagon-fill fs-3 text-danger flex-shrink-0"></i>
                            <div>
                                <h6 class="fw-bold mb-0 text-danger">Atención: Préstamos con plazo límite</h6>
                                <p class="mb-0 small text-dark">${msg}</p>
                            </div>
                        </div>
                        <a href="#prestamos" class="btn btn-sm btn-danger fw-semibold sidebar-nav-item flex-shrink-0" data-view="view-prestamos">
                            Ver Préstamos <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>
                `;
                alertaVencidosContainer.style.display = 'block';
            } else {
                alertaVencidosContainer.style.display = 'none';
                alertaVencidosContainer.innerHTML = '';
            }
        }
    }

    // ========================================================
    // 1. GESTION Y CRUD DE CATEGORIAS
    // ========================================================
    async function cargarCategorias() {
        try {
            const res = await window.Auth.authFetch('/categorias', { method: 'GET' });
            if (res.ok) {
                cacheCategorias = await res.json();
                actualizarSelectCategorias();
                renderTablaModalCategorias();
            }
        } catch (err) {
            console.error('[Dashboard] Error al cargar categorias:', err);
        }
    }

    function actualizarSelectCategorias() {
        const select = document.getElementById('materialCategoriaSelect');
        if (select) {
            const currentVal = select.value;
            select.innerHTML = '<option value="">Selecciona categoria...</option>' +
                cacheCategorias.map(c => `<option value="${c.id}">${escapeHtml(c.nombre)}</option>`).join('');
            if (currentVal) select.value = currentVal;
        }

        const filtroCat = document.getElementById('filtroMaterialCategoria');
        if (filtroCat) {
            const currentFilter = filtroCat.value;
            filtroCat.innerHTML = '<option value="">Todas las categorias</option>' +
                cacheCategorias.map(c => `<option value="${escapeHtml(c.nombre)}">${escapeHtml(c.nombre)}</option>`).join('');
            if (currentFilter) filtroCat.value = currentFilter;
        }
    }

    function resetFormCategoria() {
        const form = document.getElementById('formNuevaCategoria');
        if (form) form.reset();
        const inputId = document.getElementById('inputCategoriaId');
        if (inputId) inputId.value = '';
        const titulo = document.getElementById('tituloFormCategoria');
        if (titulo) titulo.innerHTML = '<i class="bi bi-plus-circle text-primary me-1"></i> Registrar Nueva Categoria';
        const btnGuardar = document.getElementById('btnGuardarCategoria');
        if (btnGuardar) btnGuardar.innerHTML = '<i class="bi bi-save me-1"></i> Guardar';
        const btnCancelar = document.getElementById('btnCancelarEdicionCategoria');
        if (btnCancelar) btnCancelar.classList.add('d-none');
    }

    function renderTablaModalCategorias() {
        const tbody = document.getElementById('tablaModalCategorias');
        if (!tbody) return;
        if (!cacheCategorias || cacheCategorias.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center py-2 text-muted">No hay categorias registradas.</td></tr>`;
            return;
        }
        const canManageCat = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        tbody.innerHTML = cacheCategorias.map(c => `
            <tr>
                <td class="fw-bold">#${c.id}</td>
                <td><strong>${escapeHtml(c.nombre)}</strong></td>
                <td><small class="text-muted">${escapeHtml(c.descripcion || '-')}</small></td>
                <td class="text-end">
                    ${canManageCat ? `
                        <button class="btn btn-sm btn-outline-primary py-0 px-2 me-1 btn-editar-categoria" data-id="${c.id}" data-nombre="${escapeHtml(c.nombre)}" data-desc="${escapeHtml(c.descripcion || '')}" title="Editar categoria">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger py-0 px-2 btn-eliminar-categoria" data-id="${c.id}" title="Eliminar">
                            <i class="bi bi-trash"></i>
                        </button>
                    ` : '<span class="text-muted small">Solo lectura</span>'}
                </td>
            </tr>
        `).join('');

        document.querySelectorAll('.btn-editar-categoria').forEach(btn => {
            btn.onclick = function () {
                const id = this.getAttribute('data-id');
                const nombre = this.getAttribute('data-nombre') || '';
                const desc = this.getAttribute('data-desc') || '';
                const inputId = document.getElementById('inputCategoriaId');
                const inputNombre = document.getElementById('inputNombreCategoria');
                const inputDesc = document.getElementById('inputDescCategoria');
                const titulo = document.getElementById('tituloFormCategoria');
                const btnGuardar = document.getElementById('btnGuardarCategoria');
                const btnCancelar = document.getElementById('btnCancelarEdicionCategoria');

                if (inputId) inputId.value = id;
                if (inputNombre) {
                    inputNombre.value = nombre;
                    inputNombre.focus();
                }
                if (inputDesc) inputDesc.value = desc;
                if (titulo) titulo.innerHTML = '<i class="bi bi-pencil-square text-warning me-1"></i> Modificar Categoria #' + id;
                if (btnGuardar) btnGuardar.innerHTML = '<i class="bi bi-check-lg me-1"></i> Actualizar';
                if (btnCancelar) btnCancelar.classList.remove('d-none');
            };
        });

        document.querySelectorAll('.btn-eliminar-categoria').forEach(btn => {
            btn.onclick = async function () {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes permisos para eliminar categorías.', 'warning');
                    return;
                }
                const id = this.getAttribute('data-id');
                if (confirm(`Eliminar la categoria #${id}? Asegurate de que no tenga materiales asociados.`)) {
                    try {
                        const res = await window.Auth.authFetch(`/categorias/${id}`, { method: 'DELETE' });
                        if (!res.ok) {
                            const errData = await res.json().catch(() => null);
                            showNotification(errData?.message || 'No se pudo eliminar la categoria.', 'danger');
                        } else {
                            showNotification('Categoria eliminada exitosamente.', 'success');
                            resetFormCategoria();
                            await cargarCategorias();
                        }
                    } catch (e) {
                        showNotification('Error al conectar con /api/categorias.', 'danger');
                    }
                }
            };
        });
    }

    // ========================================================
    // 2. GESTION Y CRUD DE PROYECTOS
    // ========================================================
    async function cargarProyectos() {
        try {
            const res = await window.Auth.authFetch('/proyectos', { method: 'GET' });
            if (res.ok) {
                cacheProyectos = await res.json();
                renderTablaProyectos(cacheProyectos);
                cargarProyectosSelect();
                renderMisProyectosDashboard();
            }
        } catch (err) {
            console.error('[Dashboard] Error al cargar proyectos:', err);
        }
    }

    function renderTablaProyectos(proyectos) {
        const tbody = document.getElementById('tablaProyectos');
        if (!tbody) return;

        const contador = document.getElementById('contadorProyectos');
        if (contador) {
            contador.innerHTML = `<i class="bi bi-list-check me-1"></i> ${proyectos ? proyectos.length : 0} de ${cacheProyectos.length} proyectos`;
        }

        if (!proyectos || proyectos.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">
                        <i class="bi bi-funnel me-1"></i> No se encontraron proyectos con los filtros seleccionados.
                    </td>
                </tr>
            `;
            return;
        }

        const canManageProjects = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        tbody.innerHTML = proyectos.map(p => {
            const badgeClass = getBadgeEstadoProyecto(p.estado);
            return `
                <tr>
                    <td class="ps-4 fw-bold text-muted">${p.id}</td>
                    <td class="fw-semibold text-primary">${escapeHtml(p.nombre)}</td>
                    <td class="text-secondary small" style="max-width: 250px;">
                        <span class="d-inline-block text-truncate" style="max-width: 240px;" title="${escapeHtml(p.descripcion || '')}">
                            ${escapeHtml(p.descripcion || 'Sin descripcion')}
                        </span>
                    </td>
                    <td><small class="text-muted">${p.fechaInicio || '-'}</small></td>
                    <td><small class="text-muted">${p.fechaFin || '<span class="badge bg-light text-dark border">En curso</span>'}</small></td>
                    <td><span class="badge ${badgeClass}">${escapeHtml(p.estado || 'PLANIFICADO')}</span></td>
                    <td class="text-end pe-4">
                        <div class="btn-group btn-group-sm" role="group">
                            <button type="button" class="btn btn-outline-secondary btn-ver-proyecto" data-id="${p.id}" title="Ver Detalles">
                                <i class="bi bi-eye"></i>
                            </button>
                            ${canManageProjects ? `
                                <button type="button" class="btn btn-outline-primary btn-editar-proyecto" data-id="${p.id}" title="Editar">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button type="button" class="btn btn-outline-danger btn-eliminar-proyecto" data-id="${p.id}" title="Eliminar">
                                    <i class="bi bi-trash"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        tbody.querySelectorAll('.btn-ver-proyecto').forEach(btn => {
            btn.addEventListener('click', () => verProyecto(btn.getAttribute('data-id')));
        });
        tbody.querySelectorAll('.btn-editar-proyecto').forEach(btn => {
            btn.addEventListener('click', () => editarProyecto(btn.getAttribute('data-id')));
        });
        tbody.querySelectorAll('.btn-eliminar-proyecto').forEach(btn => {
            btn.addEventListener('click', () => eliminarProyecto(btn.getAttribute('data-id')));
        });
    }

    function getBadgeEstadoProyecto(estado) {
        switch (estado) {
            case 'ACTIVO': return 'bg-success';
            case 'EN_DESARROLLO': return 'bg-primary';
            case 'PLANIFICADO': return 'bg-secondary';
            case 'FINALIZADO': return 'bg-info text-dark';
            case 'CANCELADO': return 'bg-danger';
            default: return 'bg-secondary';
        }
    }

    /**
     * Determina si el usuario actualmente autenticado figura como integrante en el proyecto especificado.
     * Realiza verificaciones defensivas comparando ID de integrante, Cédula de Identidad (CI) y Correo Electrónico.
     */
    function usuarioParticipaEnProyecto(p) {
        if (!p || !Array.isArray(p.integrantes) || p.integrantes.length === 0) return false;

        const current = window.Auth.getUser() || currentUser || {};
        const cId = userId || current.id || (claims && claims.integranteId) || null;
        const cCi = (userCi || current.ci || (claims && claims.ci) || '').toString().trim();
        const cEmail = (userEmail || current.email || (claims && claims.sub) || '').toString().trim().toLowerCase();

        return p.integrantes.some(integ => {
            if (!integ) return false;
            const integId = integ.integranteId != null ? integ.integranteId : integ.id;
            if (cId != null && integId != null && String(integId) === String(cId)) return true;
            if (cCi && integ.ci && String(integ.ci).trim() === cCi) return true;
            if (cEmail && integ.email && String(integ.email).trim().toLowerCase() === cEmail) return true;
            return false;
        });
    }

    /**
     * Renderiza el widget de proyectos activos propios en la vista principal del Dashboard (view-dashboard).
     */
    function renderMisProyectosDashboard() {
        const contenedor = document.getElementById('contenedorMisProyectosCards');
        const badgeContador = document.getElementById('badgeContadorMisProyectos');
        if (!contenedor) return;

        if (!Array.isArray(cacheProyectos)) {
            contenedor.innerHTML = `
                <div class="col-12 text-center py-4 text-muted">
                    <i class="bi bi-exclamation-circle me-1"></i> No se pudieron cargar los proyectos.
                </div>
            `;
            return;
        }

        // Filtrar proyectos activos (no finalizados ni cancelados) en los que participa el usuario
        const misProyectos = cacheProyectos.filter(p => {
            const estado = (p.estado || '').toUpperCase().trim();
            const esActivo = estado !== 'FINALIZADO' && estado !== 'CANCELADO';
            return esActivo && usuarioParticipaEnProyecto(p);
        });

        // Actualizar el badge de conteo en la cabecera del widget
        if (badgeContador) {
            if (misProyectos.length === 0) {
                badgeContador.className = 'badge bg-light text-muted border px-3 py-2';
                badgeContador.innerHTML = '<i class="bi bi-folder me-1"></i> Sin proyectos activos';
            } else {
                badgeContador.className = 'badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 fw-semibold';
                badgeContador.innerHTML = `<i class="bi bi-rocket-takeoff-fill me-1"></i> ${misProyectos.length} proyecto${misProyectos.length === 1 ? '' : 's'} activo${misProyectos.length === 1 ? '' : 's'}`;
            }
        }

        // Estado vacio amigable cuando el miembro aun no esta asignado a ningun proyecto activo
        if (misProyectos.length === 0) {
            contenedor.innerHTML = `
                <div class="col-12">
                    <div class="text-center py-4 px-3 bg-light rounded-3 border border-dashed">
                        <div class="mb-2">
                            <i class="bi bi-folder-x text-muted" style="font-size: 2.2rem;"></i>
                        </div>
                        <h6 class="fw-bold text-dark mb-1">No tienes proyectos activos asignados actualmente</h6>
                        <p class="text-muted small mb-3" style="max-width: 520px; margin: 0 auto;">
                            Cuando formes parte de un equipo o la directiva te asigne a un proyecto de robótica, podrás consultar su avance, tus compañeros de equipo y los componentes asignados directamente desde aquí.
                        </p>
                        <button type="button" class="btn btn-sm btn-outline-primary" id="btnIrAProyectosDesdeDashboard">
                            <i class="bi bi-kanban me-1"></i>Explorar proyectos del club
                        </button>
                    </div>
                </div>
            `;

            const btnIrProyectos = document.getElementById('btnIrAProyectosDesdeDashboard');
            if (btnIrProyectos) {
                btnIrProyectos.addEventListener('click', () => {
                    if (typeof switchView === 'function') {
                        switchView('view-proyectos');
                        if (window.history && window.history.pushState) {
                            window.history.pushState(null, '', '#proyectos');
                        }
                    }
                });
            }
            return;
        }

        // Renderizar tarjetas responsivas
        contenedor.innerHTML = misProyectos.map(p => {
            const badgeClass = getBadgeEstadoProyecto(p.estado);
            const numIntegrantes = (p.integrantes && Array.isArray(p.integrantes)) ? p.integrantes.length : 0;
            const numMateriales = (p.materiales && Array.isArray(p.materiales)) ? p.materiales.length : 0;
            const desc = p.descripcion ? p.descripcion : 'Sin descripción detallada registrada para este proyecto.';

            return `
                <div class="col-12 col-md-6 col-xl-4">
                    <div class="card h-100 border shadow-sm hover-shadow transition-all" style="border-radius: 10px;">
                        <div class="card-body d-flex flex-column p-3">
                            <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                                <span class="badge ${badgeClass} text-uppercase px-2 py-1" style="font-size: 0.72rem;">
                                    <i class="bi bi-circle-fill me-1" style="font-size: 0.45rem;"></i>${escapeHtml(p.estado || 'ACTIVO')}
                                </span>
                                <small class="text-muted" style="font-size: 0.75rem;">
                                    <i class="bi bi-calendar3 me-1"></i>${escapeHtml(p.fechaInicio || 'En curso')}
                                </small>
                            </div>

                            <h6 class="card-title fw-bold text-dark mb-1 text-truncate" title="${escapeHtml(p.nombre)}">
                                <i class="bi bi-kanban text-primary me-1"></i>${escapeHtml(p.nombre)}
                            </h6>

                            <p class="card-text text-secondary small flex-grow-1 mb-3" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.5em; line-height: 1.25;" title="${escapeHtml(desc)}">
                                ${escapeHtml(desc)}
                            </p>

                            <div class="d-flex flex-wrap align-items-center justify-content-between gap-1 pt-2 border-top mt-auto mb-3 small text-muted">
                                <span>
                                    <i class="bi bi-people-fill text-primary me-1"></i>
                                    <strong>${numIntegrantes}</strong> ${numIntegrantes === 1 ? 'miembro' : 'miembros'}
                                </span>
                                <span>
                                    <i class="bi bi-cpu-fill text-success me-1"></i>
                                    <strong>${numMateriales}</strong> ${numMateriales === 1 ? 'material' : 'materiales'}
                                </span>
                            </div>

                            <button type="button" class="btn btn-outline-primary btn-sm w-100 btn-ver-proyecto-dashboard" data-id="${p.id}">
                                <i class="bi bi-eye me-1"></i>Ver detalles del proyecto
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        // Event listener para ver detalles del proyecto
        contenedor.querySelectorAll('.btn-ver-proyecto-dashboard').forEach(btn => {
            btn.addEventListener('click', () => {
                const pid = btn.getAttribute('data-id');
                if (pid && typeof verProyecto === 'function') {
                    verProyecto(pid);
                }
            });
        });
    }

    async function verProyecto(id) {
        let p = cacheProyectos.find(item => item.id == id);
        try {
            const res = await window.Auth.authFetch(`/proyectos/${id}`, { method: 'GET' });
            if (res.ok) {
                p = await res.json();
            }
        } catch (e) {
            console.warn('Cargando proyecto desde cache local', e);
        }
        if (!p) return;

        const canManage = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        const cuerpo = document.getElementById('verProyectoCuerpo');
        const titulo = document.getElementById('verProyectoNombre');
        if (titulo) titulo.innerHTML = `<i class="bi bi-kanban me-2 text-primary"></i>${escapeHtml(p.nombre)}`;

        if (cuerpo) {
            const integrantesList = (p.integrantes && p.integrantes.length > 0)
                ? p.integrantes.map(integ => `
                    <li class="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
                        <div>
                            <strong>${escapeHtml(integ.nombre)} ${escapeHtml(integ.apellido || '')}</strong>
                            <small class="text-muted d-block">${escapeHtml(integ.email || '')} ${integ.carrera ? `| ${escapeHtml(integ.carrera)}` : ''}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary-subtle text-primary border">${escapeHtml(integ.rolProyecto || 'Colaborador')}</span>
                            ${canManage ? `
                                <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2 btn-desasignar-integrante" data-pid="${p.id}" data-iid="${integ.integranteId}" title="Quitar del proyecto">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            ` : ''}
                        </div>
                    </li>
                `).join('')
                : `<li class="list-group-item text-muted text-center py-3">No hay integrantes asignados a este proyecto todavía.</li>`;

            const materialesList = (p.materiales && p.materiales.length > 0)
                ? p.materiales.map(mat => {
                    const cant = (mat.cantidadPlanificada != null) ? mat.cantidadPlanificada : ((mat.cantidad != null) ? mat.cantidad : 1);
                    const desc = mat.categoriaNombre || mat.observaciones || 'Componente asignado';
                    return `
                    <li class="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
                        <div>
                            <strong>${escapeHtml(mat.nombreMaterial || 'Componente')}</strong>
                            <small class="text-muted d-block">${escapeHtml(desc)}</small>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-secondary-subtle text-secondary border">x${cant} unidades</span>
                            ${canManage ? `
                                <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2 btn-desasignar-material" data-pid="${p.id}" data-mid="${mat.materialId}" title="Quitar componente">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            ` : ''}
                        </div>
                    </li>
                `;}).join('')
                : `<li class="list-group-item text-muted text-center py-3">No hay componentes o materiales planificados asignados.</li>`;

            let formsGestionHtml = '';
            if (canManage) {
                const optIntegrantes = cacheIntegrantes.map(i => `<option value="${i.id}">${escapeHtml(i.nombre)} ${escapeHtml(i.apellido)} (${escapeHtml(i.ci || i.email)})</option>`).join('');
                const optMateriales = cacheMateriales.map(m => `<option value="${m.id}">${escapeHtml(m.nombre)} (Stock: ${m.cantidadTotal})</option>`).join('');

                formsGestionHtml = `
                    <div class="row g-3 mt-3 border-top pt-3">
                        <!-- Asignar Integrante -->
                        <div class="col-md-6">
                            <div class="card h-100 border bg-light shadow-none">
                                <div class="card-header bg-white py-2 fw-semibold small text-primary d-flex align-items-center">
                                    <i class="bi bi-person-plus me-1"></i> Asignar Miembro al Equipo
                                </div>
                                <div class="card-body p-3">
                                    <div class="mb-2">
                                        <select class="form-select form-select-sm" id="selectNuevoIntegranteProyecto">
                                            <option value="">Seleccionar miembro...</option>
                                            ${optIntegrantes}
                                        </select>
                                    </div>
                                    <div class="mb-2">
                                        <input type="text" class="form-control form-control-sm" id="inputRolIntegranteProyecto" placeholder="Rol (ej. Lider, Desarrollador)">
                                    </div>
                                    <button type="button" class="btn btn-sm btn-primary w-100" id="btnAsignarIntegranteProyecto" data-pid="${p.id}">
                                        <i class="bi bi-plus-circle me-1"></i> Asignar al Equipo
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- Asignar Material -->
                        <div class="col-md-6">
                            <div class="card h-100 border bg-light shadow-none">
                                <div class="card-header bg-white py-2 fw-semibold small text-secondary d-flex align-items-center">
                                    <i class="bi bi-cpu me-1"></i> Planificar Componente
                                </div>
                                <div class="card-body p-3">
                                    <div class="mb-2">
                                        <select class="form-select form-select-sm" id="selectNuevoMaterialProyecto">
                                            <option value="">Seleccionar componente...</option>
                                            ${optMateriales}
                                        </select>
                                    </div>
                                    <div class="row g-2 mb-2">
                                        <div class="col-4">
                                            <input type="number" class="form-control form-control-sm" id="inputCantidadMaterialProyecto" min="1" value="1" placeholder="Cant">
                                        </div>
                                        <div class="col-8">
                                            <input type="text" class="form-control form-control-sm" id="inputObsMaterialProyecto" placeholder="Obs / Notas">
                                        </div>
                                    </div>
                                    <button type="button" class="btn btn-sm btn-secondary w-100" id="btnAsignarMaterialProyecto" data-pid="${p.id}">
                                        <i class="bi bi-plus-circle me-1"></i> Asignar Material
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            }

            cuerpo.innerHTML = `
                <div class="mb-3">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="badge ${getBadgeEstadoProyecto(p.estado)} fs-6">${escapeHtml(p.estado)}</span>
                        <small class="text-muted">ID Proyecto: #${p.id}</small>
                    </div>
                    <p class="text-secondary bg-light p-3 rounded-2 mb-3">${escapeHtml(p.descripcion || 'Sin descripcion detallada.')}</p>
                    <div class="row g-2 small border rounded-2 p-2 bg-white mb-3">
                        <div class="col-6"><strong>Fecha de Inicio:</strong> ${p.fechaInicio || '-'}</div>
                        <div class="col-6"><strong>Fecha Fin:</strong> ${p.fechaFin || 'En curso / Abierto'}</div>
                    </div>
                </div>

                <div class="row g-3">
                    <!-- Lista de Integrantes -->
                    <div class="col-md-6">
                        <h6 class="fw-bold text-dark small text-uppercase mb-2"><i class="bi bi-people me-1 text-primary"></i> Equipo del Proyecto (${p.integrantes ? p.integrantes.length : 0})</h6>
                        <ul class="list-group list-group-flush border rounded-2" style="max-height: 220px; overflow-y: auto;">
                            ${integrantesList}
                        </ul>
                    </div>

                    <!-- Lista de Materiales -->
                    <div class="col-md-6">
                        <h6 class="fw-bold text-dark small text-uppercase mb-2"><i class="bi bi-box-seam me-1 text-secondary"></i> Materiales Planificados (${p.materiales ? p.materiales.length : 0})</h6>
                        <ul class="list-group list-group-flush border rounded-2" style="max-height: 220px; overflow-y: auto;">
                            ${materialesList}
                        </ul>
                    </div>
                </div>

                ${formsGestionHtml}
            `;

            if (canManage) {
                // Quitar integrante
                cuerpo.querySelectorAll('.btn-desasignar-integrante').forEach(btn => {
                    btn.onclick = async () => {
                        const pid = btn.getAttribute('data-pid');
                        const iid = btn.getAttribute('data-iid');
                        try {
                            const res = await window.Auth.authFetch(`/proyectos/${pid}/integrantes/${iid}`, { method: 'DELETE' });
                            if (res.ok) {
                                showNotification('Miembro desasignado del proyecto.', 'info');
                                await verProyecto(pid);
                                await cargarProyectos();
                            } else {
                                const err = await res.json().catch(() => null);
                                showNotification(err?.message || 'Error al quitar miembro.', 'danger');
                            }
                        } catch (e) {
                            showNotification('Error al procesar la solicitud.', 'danger');
                        }
                    };
                });

                // Quitar material
                cuerpo.querySelectorAll('.btn-desasignar-material').forEach(btn => {
                    btn.onclick = async () => {
                        const pid = btn.getAttribute('data-pid');
                        const mid = btn.getAttribute('data-mid');
                        try {
                            const res = await window.Auth.authFetch(`/proyectos/${pid}/materiales/${mid}`, { method: 'DELETE' });
                            if (res.ok) {
                                showNotification('Componente desasignado del proyecto.', 'info');
                                await verProyecto(pid);
                                await cargarProyectos();
                            } else {
                                const err = await res.json().catch(() => null);
                                showNotification(err?.message || 'Error al quitar componente.', 'danger');
                            }
                        } catch (e) {
                            showNotification('Error al procesar la solicitud.', 'danger');
                        }
                    };
                });

                // Asignar integrante
                const btnAsigInteg = cuerpo.querySelector('#btnAsignarIntegranteProyecto');
                if (btnAsigInteg) {
                    btnAsigInteg.onclick = async () => {
                        const pid = btnAsigInteg.getAttribute('data-pid');
                        const selInteg = cuerpo.querySelector('#selectNuevoIntegranteProyecto').value;
                        const rolInteg = cuerpo.querySelector('#inputRolIntegranteProyecto').value.trim() || 'Colaborador';
                        if (!selInteg) {
                            showNotification('Por favor selecciona un integrante.', 'warning');
                            return;
                        }
                        try {
                            const res = await window.Auth.authFetch(`/proyectos/${pid}/integrantes/${selInteg}?rolProyecto=${encodeURIComponent(rolInteg)}`, {
                                method: 'POST'
                            });
                            if (res.ok) {
                                showNotification('Integrante asignado exitosamente al proyecto!', 'success');
                                await verProyecto(pid);
                                await cargarProyectos();
                            } else {
                                const err = await res.json().catch(() => null);
                                showNotification(err?.message || 'Error al asignar integrante.', 'danger');
                            }
                        } catch (e) {
                            showNotification('Error al procesar la asignación.', 'danger');
                        }
                    };
                }

                // Asignar material
                const btnAsigMat = cuerpo.querySelector('#btnAsignarMaterialProyecto');
                if (btnAsigMat) {
                    btnAsigMat.onclick = async () => {
                        const pid = btnAsigMat.getAttribute('data-pid');
                        const selMat = cuerpo.querySelector('#selectNuevoMaterialProyecto').value;
                        const cantMat = Number(cuerpo.querySelector('#inputCantidadMaterialProyecto').value) || 1;
                        const obsMat = cuerpo.querySelector('#inputObsMaterialProyecto').value.trim();
                        if (!selMat) {
                            showNotification('Por favor selecciona un material.', 'warning');
                            return;
                        }
                        try {
                            const res = await window.Auth.authFetch(`/proyectos/${pid}/materiales`, {
                                method: 'POST',
                                body: JSON.stringify({
                                    materialId: Number(selMat),
                                    cantidad: cantMat,
                                    observaciones: obsMat
                                })
                            });
                            if (res.ok) {
                                showNotification('Componente asignado exitosamente al proyecto!', 'success');
                                await verProyecto(pid);
                                await cargarProyectos();
                            } else {
                                const err = await res.json().catch(() => null);
                                showNotification(err?.message || 'Error al asignar componente.', 'danger');
                            }
                        } catch (e) {
                            showNotification('Error al procesar la asignación.', 'danger');
                        }
                    };
                }
            }
        }
        getModal('modalVerProyecto')?.show();
    }

    function editarProyecto(id) {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            showNotification('No tienes permisos para editar proyectos.', 'warning');
            return;
        }
        const p = cacheProyectos.find(item => item.id == id);
        if (!p) return;

        document.getElementById('modalProyectoTitulo').innerHTML = `<i class="bi bi-pencil-square me-2"></i>Editar Proyecto #${p.id}`;
        document.getElementById('proyectoId').value = p.id;
        document.getElementById('proyectoNombre').value = p.nombre || '';
        document.getElementById('proyectoDescripcion').value = p.descripcion || '';
        document.getElementById('proyectoFechaInicio').value = p.fechaInicio || '';
        document.getElementById('proyectoFechaFin').value = p.fechaFin || '';
        document.getElementById('proyectoEstado').value = p.estado || 'ACTIVO';

        getModal('modalProyecto')?.show();
    }

    function eliminarProyecto(id) {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            showNotification('No tienes permisos para eliminar proyectos.', 'warning');
            return;
        }
        const p = cacheProyectos.find(item => item.id == id);
        if (!p) return;

        const texto = document.getElementById('textoConfirmarEliminacion');
        if (texto) {
            texto.innerHTML = `Estas seguro de que deseas eliminar el proyecto <strong>${escapeHtml(p.nombre)}</strong>?`;
        }

        const btnConfirm = document.getElementById('btnConfirmarEliminacionAccion');
        if (btnConfirm) {
            btnConfirm.onclick = async () => {
                btnConfirm.disabled = true;
                try {
                    const res = await window.Auth.authFetch(`/proyectos/${id}`, { method: 'DELETE' });
                    if (res.ok) {
                        showNotification('Proyecto procesado exitosamente!', 'success');
                        getModal('modalConfirmarEliminacion')?.hide();
                        await cargarProyectos();
                    } else {
                        const err = await res.json().catch(() => null);
                        showNotification((err && err.message) ? err.message : 'No se pudo eliminar el proyecto.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error de conexion con el servidor.', 'danger');
                } finally {
                    btnConfirm.disabled = false;
                }
            };
        }

        getModal('modalConfirmarEliminacion')?.show();
    }

    function cargarProyectosSelect() {
        const select = document.getElementById('prestamoProyectoSelect');
        if (!select) return;
        select.innerHTML = '<option value="">Ninguno / Prestamo General</option>' +
            cacheProyectos.map(p => `<option value="${p.id}">${escapeHtml(p.nombre)}</option>`).join('');
    }

    // ========================================================
    // 3. CARGA Y CRUD DE MATERIALES (INVENTARIO)
    // ========================================================
    async function cargarInventario() {
        try {
            const res = await window.Auth.authFetch('/materiales', { method: 'GET' });
            if (!res.ok) throw new Error('Error al listar materiales.');
            cacheMateriales = await res.json();
            renderTablaInventario(cacheMateriales);
            cargarMaterialesSelect();
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando inventario:', err);
            renderErrorEnTabla('tablaInventario', 8, 'Error al conectar con /api/materiales.');
        }
    }

    function renderTablaInventario(materiales) {
        const tbody = document.getElementById('tablaInventario');
        if (!tbody) return;

        const contador = document.getElementById('contadorMateriales');
        if (contador) {
            contador.innerHTML = `<i class="bi bi-list-check me-1"></i> ${materiales ? materiales.length : 0} de ${cacheMateriales.length} materiales`;
        }

        if (!materiales || materiales.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><i class="bi bi-funnel me-1"></i>No se encontraron componentes con los filtros seleccionados.</td></tr>`;
            return;
        }

        tbody.innerHTML = materiales.map(m => `
            <tr id="row-material-${m.id}">
                <td class="ps-4 fw-bold text-secondary">#${m.id}</td>
                <td>
                    <span class="fw-semibold text-dark">${escapeHtml(m.nombre)}</span>
                    <small class="text-muted d-block" style="font-size:0.75rem;">${escapeHtml(m.descripcion || '')}</small>
                </td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(m.categoriaNombre || 'General')}</span></td>
                <td>${escapeHtml(m.marca || '-')} / ${escapeHtml(m.modelo || '-')}</td>
                <td>
                    <div class="d-flex flex-column align-items-start">
                        <span class="badge ${(m.cantidadDisponible !== undefined ? m.cantidadDisponible : m.cantidadTotal) > 0 ? 'bg-success text-white' : 'bg-danger text-white'} px-2 py-1 mb-1">
                            Disp: ${m.cantidadDisponible !== undefined ? m.cantidadDisponible : m.cantidadTotal} un.
                        </span>
                        <small class="text-muted" style="font-size:0.75rem;">
                            Total: ${m.cantidadTotal} un. ${(m.cantidadReservada && m.cantidadReservada > 0) ? `(${m.cantidadReservada} prestados)` : ''}
                        </small>
                    </div>
                </td>
                <td><small class="text-muted"><i class="bi bi-geo-alt me-1"></i>${escapeHtml(m.ubicacion || 'Laboratorio')}</small></td>
                <td><span class="badge ${getBadgeEstadoMaterial(m.estado)} py-1 px-2">${m.estado}</span></td>
                <td class="text-end pe-4">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-info btn-ver-material" data-id="${m.id}" title="Ver Detalle">
                            <i class="bi bi-eye"></i>
                        </button>
                        ${window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA') ? `
                            <button class="btn btn-outline-primary btn-editar-material" data-id="${m.id}" title="Editar">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-outline-danger btn-eliminar-material" data-id="${m.id}" title="Eliminar">
                                <i class="bi bi-trash"></i>
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `).join('');

        attachMaterialActionListeners();
    }

    function getBadgeEstadoMaterial(estado) {
        if (estado === 'ACTIVO') return 'bg-success';
        if (estado === 'MANTENIMIENTO') return 'bg-warning text-dark';
        if (estado === 'BAJA') return 'bg-danger';
        return 'bg-secondary';
    }

    function attachMaterialActionListeners() {
        document.querySelectorAll('.btn-ver-material').forEach(btn => {
            btn.onclick = () => {
                const id = Number(btn.getAttribute('data-id'));
                const mat = cacheMateriales.find(m => m.id === id);
                if (!mat) return;
                const cuerpo = document.getElementById('cuerpoVerMaterial');
                if (cuerpo) {
                    cuerpo.innerHTML = `
                        <div class="d-flex align-items-center justify-content-between mb-3 border-bottom pb-2">
                            <h5 class="fw-bold mb-0 text-primary">${escapeHtml(mat.nombre)}</h5>
                            <span class="badge ${getBadgeEstadoMaterial(mat.estado)} fs-6">${mat.estado}</span>
                        </div>
                        <p class="text-muted">${escapeHtml(mat.descripcion || 'Sin descripcion detallada.')}</p>
                        <div class="row g-2 small">
                            <div class="col-6"><strong>ID:</strong> #${mat.id}</div>
                            <div class="col-6"><strong>Categoria:</strong> ${escapeHtml(mat.categoriaNombre || 'General')}</div>
                            <div class="col-6"><strong>Marca:</strong> ${escapeHtml(mat.marca || '-')}</div>
                            <div class="col-6"><strong>Modelo:</strong> ${escapeHtml(mat.modelo || '-')}</div>
                            <div class="col-6"><strong>Stock Total:</strong> <span class="badge bg-secondary text-white">${mat.cantidadTotal} unidades</span></div>
                            <div class="col-6"><strong>Disponibles:</strong> <span class="badge ${(mat.cantidadDisponible !== undefined ? mat.cantidadDisponible : mat.cantidadTotal) > 0 ? 'bg-success text-white' : 'bg-danger text-white'}">${mat.cantidadDisponible !== undefined ? mat.cantidadDisponible : mat.cantidadTotal} unidades</span></div>
                            <div class="col-6"><strong>En Prestamos:</strong> <span class="badge bg-warning text-dark">${mat.cantidadReservada || 0} unidades</span></div>
                            <div class="col-6"><strong>Ubicacion:</strong> ${escapeHtml(mat.ubicacion || 'Laboratorio')}</div>
                        </div>
                    `;
                }
                getModal('modalVerMaterial')?.show();
            };
        });

        document.querySelectorAll('.btn-editar-material').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para editar componentes o materiales.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                const mat = cacheMateriales.find(m => m.id === id);
                if (!mat) return;

                document.getElementById('modalMaterialTitulo').innerHTML = `<i class="bi bi-pencil-square me-2"></i>Editar Componente #${mat.id}`;
                document.getElementById('materialIdInput').value = mat.id;
                document.getElementById('materialNombreInput').value = mat.nombre || '';
                document.getElementById('materialMarcaInput').value = mat.marca || '';
                document.getElementById('materialModeloInput').value = mat.modelo || '';
                document.getElementById('materialCantidadInput').value = mat.cantidadTotal || 0;
                document.getElementById('materialEstadoSelect').value = mat.estado || 'ACTIVO';
                document.getElementById('materialUbicacionInput').value = mat.ubicacion || '';
                document.getElementById('materialDescripcionInput').value = mat.descripcion || '';

                actualizarSelectCategorias();
                if (mat.categoriaId) {
                    document.getElementById('materialCategoriaSelect').value = mat.categoriaId;
                }

                getModal('modalMaterial')?.show();
            };
        });

        document.querySelectorAll('.btn-eliminar-material').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para eliminar componentes o materiales.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                const mat = cacheMateriales.find(m => m.id === id);
                const nombre = mat ? mat.nombre : `#${id}`;

                abrirModalConfirmarEliminacion(
                    `Deseas eliminar el material <strong>${escapeHtml(nombre)}</strong>? Si tiene prestamos asociados, se recomienda cambiar su estado a "BAJA" o "MANTENIMIENTO".`,
                    async () => {
                        try {
                            const res = await window.Auth.authFetch(`/materiales/${id}`, { method: 'DELETE' });
                            if (!res.ok) {
                                const errData = await res.json().catch(() => null);
                                showNotification(errData?.message || 'No se pudo eliminar el material.', 'danger');
                            } else {
                                showNotification(`Material #${id} eliminado correctamente.`, 'success');
                                await cargarInventario();
                            }
                        } catch (e) {
                            showNotification('Error al eliminar el material.', 'danger');
                        }
                    }
                );
            };
        });
    }

    function cargarMaterialesSelect() {
        const select = document.getElementById('prestamoMaterialSelect');
        if (!select) return;
        select.innerHTML = '<option value="">Seleccionar material del inventario...</option>' +
            cacheMateriales
                .filter(m => m.estado === 'ACTIVO' && ((m.cantidadDisponible !== undefined ? m.cantidadDisponible : m.cantidadTotal) > 0))
                .map(m => {
                    const disp = m.cantidadDisponible !== undefined ? m.cantidadDisponible : m.cantidadTotal;
                    return `<option value="${m.id}">${escapeHtml(m.nombre)} (Disp: ${disp} un.)</option>`;
                })
                .join('');
    }

    // ========================================================
    // 4. CARGA Y CRUD DE INTEGRANTES (PADRON)
    // ========================================================
    async function cargarIntegrantes() {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            cacheIntegrantes = [];
            cargarIntegrantesSelect();
            return;
        }
        try {
            const res = await window.Auth.authFetch('/integrantes', { method: 'GET' });
            if (!res.ok) throw new Error('Error al listar integrantes.');
            cacheIntegrantes = await res.json();
            renderTablaIntegrantes(cacheIntegrantes);
            cargarIntegrantesSelect();
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando integrantes:', err);
            renderErrorEnTabla('tablaIntegrantes', 9, 'Error al conectar con /api/integrantes.');
        }
    }

    function renderTablaIntegrantes(integrantes) {
        const tbody = document.getElementById('tablaIntegrantes');
        if (!tbody) return;

        const contador = document.getElementById('contadorIntegrantes');
        if (contador) {
            contador.innerHTML = `<i class="bi bi-list-check me-1"></i> ${integrantes ? integrantes.length : 0} de ${cacheIntegrantes.length} miembros`;
        }

        if (!integrantes || integrantes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-muted"><i class="bi bi-funnel me-1"></i>No se encontraron integrantes con los filtros seleccionados.</td></tr>`;
            return;
        }

        tbody.innerHTML = integrantes.map(i => {
            let roleBadge = 'bg-secondary';
            if (i.rol === 'ADMIN') roleBadge = 'badge-role-ADMIN';
            else if (i.rol === 'DIRECTIVA') roleBadge = 'badge-role-DIRECTIVA';
            else roleBadge = 'badge-role-MIEMBRO';

            return `
                <tr id="row-integrante-${i.id}">
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
                    <td><span class="badge ${i.estado === 'ACTIVO' ? 'bg-success' : 'bg-secondary'} py-1 px-2">${i.estado}</span></td>
                    <td class="text-end pe-4">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-info btn-ver-integrante" data-id="${i.id}" title="Ver Perfil">
                                <i class="bi bi-eye"></i>
                            </button>
                            ${window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA') ? `
                                <button class="btn btn-outline-primary btn-editar-integrante" data-id="${i.id}" title="Editar">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button class="btn btn-outline-danger btn-eliminar-integrante" data-id="${i.id}" title="Eliminar / Baja">
                                    <i class="bi bi-trash"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        attachIntegranteActionListeners();
    }

    function attachIntegranteActionListeners() {
        document.querySelectorAll('.btn-ver-integrante').forEach(btn => {
            btn.onclick = () => {
                const id = Number(btn.getAttribute('data-id'));
                const integ = cacheIntegrantes.find(i => i.id === id);
                if (!integ) return;
                const cuerpo = document.getElementById('cuerpoVerIntegrante');
                if (cuerpo) {
                    cuerpo.innerHTML = `
                        <div class="text-center mb-4">
                            <div class="user-avatar-circle mx-auto mb-2" style="width: 56px; height: 56px; font-size: 1.5rem;">
                                ${(integ.nombreCompleto || 'U').charAt(0).toUpperCase()}
                            </div>
                            <h5 class="fw-bold mb-1">${escapeHtml(integ.nombreCompleto)}</h5>
                            <span class="badge badge-role-${integ.rol}">${integ.rol}</span>
                            <span class="badge ${integ.estado === 'ACTIVO' ? 'bg-success' : 'bg-secondary'} ms-1">${integ.estado}</span>
                        </div>
                        <div class="row g-2 small border-top pt-3">
                            <div class="col-6"><strong>Cedula (CI):</strong> <code>${escapeHtml(integ.ci)}</code></div>
                            <div class="col-6"><strong>Carnet Univ.:</strong> ${escapeHtml(integ.carnetUniversitario || '-')}</div>
                            <div class="col-6"><strong>Email:</strong> ${escapeHtml(integ.email)}</div>
                            <div class="col-6"><strong>Telefono:</strong> ${escapeHtml(integ.telefono || '-')}</div>
                            <div class="col-6"><strong>Carrera:</strong> ${escapeHtml(integ.carrera || '-')}</div>
                            <div class="col-6"><strong>RFID UID:</strong> <code class="text-primary">${escapeHtml(integ.nfcUid || 'No asignado')}</code></div>
                            <div class="col-12 mt-2"><strong>Fecha Ingreso:</strong> ${integ.fechaIngreso || 'Registrado'}</div>
                        </div>
                    `;
                }
                getModal('modalVerIntegrante')?.show();
            };
        });

        document.querySelectorAll('.btn-editar-integrante').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para editar integrantes.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                const integ = cacheIntegrantes.find(i => i.id === id);
                if (!integ) return;

                document.getElementById('modalIntegranteTitulo').innerHTML = `<i class="bi bi-pencil-square me-2"></i>Editar Integrante #${integ.id}`;
                document.getElementById('integranteIdInput').value = integ.id;
                document.getElementById('integranteNombreInput').value = integ.nombre || '';
                document.getElementById('integranteApellidoInput').value = integ.apellido || '';
                document.getElementById('integranteCiInput').value = integ.ci || '';
                document.getElementById('integranteCarnetInput').value = integ.carnetUniversitario || '';
                document.getElementById('integranteTelefonoInput').value = integ.telefono || '';
                document.getElementById('integranteEmailInput').value = integ.email || '';
                document.getElementById('integranteCarreraSelect').value = integ.carrera || 'INGENIERIA_DE_SISTEMAS';
                document.getElementById('integranteRolSelect').value = integ.rol || 'MIEMBRO';
                document.getElementById('integranteEstadoSelect').value = integ.estado || 'ACTIVO';
                document.getElementById('integranteNfcInput').value = integ.nfcUid || '';
                document.getElementById('integrantePasswordInput').value = '';

                getModal('modalIntegrante')?.show();
            };
        });

        document.querySelectorAll('.btn-eliminar-integrante').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para dar de baja o eliminar integrantes.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                const integ = cacheIntegrantes.find(i => i.id === id);
                const nombre = integ ? integ.nombreCompleto : `#${id}`;

                abrirModalConfirmarEliminacion(
                    `Deseas dar de baja o eliminar al integrante <strong>${escapeHtml(nombre)}</strong>? Si tiene prestamos o marcaciones historicas, pasara automaticamente a estado INACTIVO.`,
                    async () => {
                        try {
                            const res = await window.Auth.authFetch(`/integrantes/${id}`, { method: 'DELETE' });
                            if (!res.ok) {
                                const errData = await res.json().catch(() => null);
                                showNotification(errData?.message || 'No se pudo eliminar al integrante.', 'danger');
                            } else {
                                showNotification(`Integrante #${id} procesado exitosamente.`, 'success');
                                await cargarIntegrantes();
                            }
                        } catch (e) {
                            showNotification('Error al eliminar integrante.', 'danger');
                        }
                    }
                );
            };
        });
    }

    function cargarIntegrantesSelect() {
        const select = document.getElementById('prestamoIntegranteSelect');
        if (!select) return;

        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            select.innerHTML = `<option value="${userId || ''}" selected>${escapeHtml(userName)} (CI: ${escapeHtml(userCi || '-')})</option>`;
            if (userId) select.value = userId;
            return;
        }

        select.innerHTML = '<option value="">Seleccionar solicitante...</option>' +
            cacheIntegrantes
                .filter(i => i.estado === 'ACTIVO')
                .map(i => `<option value="${i.id}">${escapeHtml(i.nombreCompleto)} (CI: ${escapeHtml(i.ci)})</option>`)
                .join('');

        if (userId && !window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            select.value = userId;
        }
    }

    function initSearchablePrestamoPickers() {
        const inputInt = document.getElementById('prestamoIntegranteBusqueda');
        const resInt = document.getElementById('prestamoIntegranteResultados');
        const btnLimpiarInt = document.getElementById('btnLimpiarIntegranteSel');

        const inputMat = document.getElementById('prestamoMaterialBusqueda');
        const resMat = document.getElementById('prestamoMaterialResultados');
        const btnLimpiarMat = document.getElementById('btnLimpiarMaterialSel');

        // Busqueda de Integrante Solicitante
        if (inputInt && resInt) {
            const buscarIntegrantes = () => {
                const query = (inputInt.value || '').trim().toLowerCase();
                const filtrados = cacheIntegrantes.filter(i => {
                    if (i.estado !== 'ACTIVO') return false;
                    if (!query) return true;
                    const nombre = (i.nombreCompleto || `${i.nombre || ''} ${i.apellido || ''}`).toLowerCase();
                    const ci = (i.ci || '').toLowerCase();
                    const carnet = (i.carnetUniversitario || '').toLowerCase();
                    const email = (i.email || '').toLowerCase();
                    return nombre.includes(query) || ci.includes(query) || carnet.includes(query) || email.includes(query);
                }).slice(0, 8);

                if (filtrados.length === 0) {
                    resInt.innerHTML = `<div class="p-3 text-muted text-center small"><i class="bi bi-search me-1"></i>No se encontraron integrantes activos</div>`;
                } else {
                    resInt.innerHTML = filtrados.map(i => {
                        let roleBadge = 'badge-role-MIEMBRO';
                        if (i.rol === 'ADMIN') roleBadge = 'badge-role-ADMIN';
                        else if (i.rol === 'DIRECTIVA') roleBadge = 'badge-role-DIRECTIVA';

                        return `
                            <button type="button" class="list-group-item list-group-item-action py-2 px-3 d-flex justify-content-between align-items-center item-sel-integrante" data-id="${i.id}">
                                <div class="text-start">
                                    <strong class="d-block text-dark lh-sm">${escapeHtml(i.nombreCompleto || `${i.nombre || ''} ${i.apellido || ''}`)}</strong>
                                    <small class="text-muted">CI: ${escapeHtml(i.ci || '-')} ${i.carnetUniversitario ? ' | Carnet: ' + escapeHtml(i.carnetUniversitario) : ''}</small>
                                </div>
                                <span class="badge ${roleBadge} ms-2">${i.rol || 'MIEMBRO'}</span>
                            </button>
                        `;
                    }).join('');
                }

                resInt.style.display = 'block';

                resInt.querySelectorAll('.item-sel-integrante').forEach(item => {
                    item.onclick = (e) => {
                        e.preventDefault();
                        const id = Number(item.getAttribute('data-id'));
                        const integ = cacheIntegrantes.find(x => x.id === id);
                        if (integ) {
                            seleccionarIntegranteEnPrestamo(integ);
                        }
                    };
                });
            };

            inputInt.addEventListener('input', buscarIntegrantes);
            inputInt.addEventListener('focus', buscarIntegrantes);
        }

        if (btnLimpiarInt) {
            btnLimpiarInt.onclick = (e) => {
                e.preventDefault();
                limpiarSeleccionIntegrantePrestamo();
            };
        }

        // Busqueda de Componente o Material
        if (inputMat && resMat) {
            const buscarMateriales = () => {
                const query = (inputMat.value || '').trim().toLowerCase();
                const filtrados = cacheMateriales.filter(m => {
                    const disp = m.cantidadDisponible !== undefined ? m.cantidadDisponible : (m.cantidadTotal || 0);
                    if (m.estado !== 'ACTIVO' || disp <= 0) return false;
                    if (!query) return true;
                    const nombre = (m.nombre || '').toLowerCase();
                    const desc = (m.descripcion || '').toLowerCase();
                    const cat = (m.categoriaNombre || '').toLowerCase();
                    const marca = (m.marca || '').toLowerCase();
                    const modelo = (m.modelo || '').toLowerCase();
                    return nombre.includes(query) || desc.includes(query) || cat.includes(query) || marca.includes(query) || modelo.includes(query);
                }).slice(0, 8);

                if (filtrados.length === 0) {
                    resMat.innerHTML = `<div class="p-3 text-muted text-center small"><i class="bi bi-search me-1"></i>No se encontraron componentes disponibles</div>`;
                } else {
                    resMat.innerHTML = filtrados.map(m => `
                        <button type="button" class="list-group-item list-group-item-action py-2 px-3 d-flex justify-content-between align-items-center item-sel-material" data-id="${m.id}">
                            <div class="text-start">
                                <strong class="d-block text-dark lh-sm">${escapeHtml(m.nombre)}</strong>
                                <small class="text-muted">${escapeHtml(m.categoriaNombre || 'General')} | Ubic: ${escapeHtml(m.ubicacion || 'Lab')}</small>
                            </div>
                            <span class="badge bg-success-subtle text-success border border-success-subtle ms-2">${m.cantidadDisponible !== undefined ? m.cantidadDisponible : m.cantidadTotal} disp.</span>
                        </button>
                    `).join('');
                }

                resMat.style.display = 'block';

                resMat.querySelectorAll('.item-sel-material').forEach(item => {
                    item.onclick = (e) => {
                        e.preventDefault();
                        const id = Number(item.getAttribute('data-id'));
                        const mat = cacheMateriales.find(x => x.id === id);
                        if (mat) {
                            seleccionarMaterialEnPrestamo(mat);
                        }
                    };
                });
            };

            inputMat.addEventListener('input', buscarMateriales);
            inputMat.addEventListener('focus', buscarMateriales);
        }

        if (btnLimpiarMat) {
            btnLimpiarMat.onclick = (e) => {
                e.preventDefault();
                limpiarSeleccionMaterialPrestamo();
            };
        }

        // Cerrar listas emergentes al hacer clic fuera
        document.addEventListener('click', (e) => {
            const contInt = document.getElementById('contenedorBuscadorIntegrante');
            if (contInt && !contInt.contains(e.target) && resInt) {
                resInt.style.display = 'none';
            }
            const contMat = document.getElementById('contenedorBuscadorMaterial');
            if (contMat && !contMat.contains(e.target) && resMat) {
                resMat.style.display = 'none';
            }
        });
    }

    function seleccionarIntegranteEnPrestamo(integrante, disableClear = false) {
        if (!integrante) return;
        const select = document.getElementById('prestamoIntegranteSelect');
        if (select) {
            let opt = select.querySelector(`option[value="${integrante.id}"]`);
            if (!opt) {
                opt = document.createElement('option');
                opt.value = integrante.id;
                opt.textContent = `${integrante.nombreCompleto || integrante.nombre || 'Integrante'} (CI: ${integrante.ci || '-'})`;
                select.appendChild(opt);
            }
            select.value = integrante.id;
            select.dispatchEvent(new Event('change'));
        }

        const selCard = document.getElementById('prestamoIntegranteSeleccionado');
        const busCont = document.getElementById('contenedorBuscadorIntegrante');
        const resDiv = document.getElementById('prestamoIntegranteResultados');
        const avatar = document.getElementById('prestamoSelAvatar');
        const nombre = document.getElementById('prestamoSelNombre');
        const detalle = document.getElementById('prestamoSelDetalle');
        const btnLimpiar = document.getElementById('btnLimpiarIntegranteSel');

        const nom = (integrante.nombreCompleto || `${integrante.nombre || ''} ${integrante.apellido || ''}`).trim() || 'U';
        if (avatar) avatar.textContent = nom.substring(0, 2).toUpperCase();
        if (nombre) nombre.textContent = nom;
        if (detalle) detalle.textContent = `CI: ${integrante.ci || 'S/D'} | ${integrante.carnetUniversitario ? 'Carnet: ' + integrante.carnetUniversitario : (integrante.rol || 'Miembro')}`;

        if (btnLimpiar) {
            btnLimpiar.style.display = disableClear ? 'none' : 'block';
        }

        if (resDiv) resDiv.style.display = 'none';
        if (busCont) busCont.style.display = 'none';
        if (selCard) selCard.style.display = 'block';
    }

    function limpiarSeleccionIntegrantePrestamo() {
        const select = document.getElementById('prestamoIntegranteSelect');
        if (select) {
            select.value = '';
            select.dispatchEvent(new Event('change'));
        }

        const inputBusqueda = document.getElementById('prestamoIntegranteBusqueda');
        if (inputBusqueda) inputBusqueda.value = '';

        const selCard = document.getElementById('prestamoIntegranteSeleccionado');
        const busCont = document.getElementById('contenedorBuscadorIntegrante');
        const resDiv = document.getElementById('prestamoIntegranteResultados');

        if (resDiv) resDiv.style.display = 'none';
        if (selCard) selCard.style.display = 'none';
        if (busCont) busCont.style.display = 'block';
    }

    function seleccionarMaterialEnPrestamo(material) {
        if (!material) return;
        const disp = material.cantidadDisponible !== undefined ? material.cantidadDisponible : (material.cantidadTotal || 0);
        const select = document.getElementById('prestamoMaterialSelect');
        if (select) {
            let opt = select.querySelector(`option[value="${material.id}"]`);
            if (!opt) {
                opt = document.createElement('option');
                opt.value = material.id;
                opt.textContent = `${material.nombre} (Disp: ${disp} un.)`;
                select.appendChild(opt);
            }
            select.value = material.id;
            select.dispatchEvent(new Event('change'));
        }

        const selCard = document.getElementById('prestamoMaterialSeleccionado');
        const busCont = document.getElementById('contenedorBuscadorMaterial');
        const resDiv = document.getElementById('prestamoMaterialResultados');
        const nombre = document.getElementById('prestamoSelMaterialNombre');
        const detalle = document.getElementById('prestamoSelMaterialDetalle');
        const cantidadInput = document.getElementById('prestamoCantidadInput');

        if (nombre) nombre.textContent = material.nombre;
        if (detalle) detalle.textContent = `Disp: ${disp} un. (Total: ${material.cantidadTotal}) | ${material.categoriaNombre || 'General'} | Ubic: ${material.ubicacion || 'Lab'}`;

        if (cantidadInput) {
            cantidadInput.max = Math.max(1, disp);
            if (Number(cantidadInput.value) > disp || Number(cantidadInput.value) < 1) {
                cantidadInput.value = disp > 0 ? 1 : 0;
            }
        }

        if (resDiv) resDiv.style.display = 'none';
        if (busCont) busCont.style.display = 'none';
        if (selCard) selCard.style.display = 'block';
    }

    function limpiarSeleccionMaterialPrestamo() {
        const select = document.getElementById('prestamoMaterialSelect');
        if (select) {
            select.value = '';
            select.dispatchEvent(new Event('change'));
        }

        const inputBusqueda = document.getElementById('prestamoMaterialBusqueda');
        if (inputBusqueda) inputBusqueda.value = '';

        const selCard = document.getElementById('prestamoMaterialSeleccionado');
        const busCont = document.getElementById('contenedorBuscadorMaterial');
        const resDiv = document.getElementById('prestamoMaterialResultados');

        if (resDiv) resDiv.style.display = 'none';
        if (selCard) selCard.style.display = 'none';
        if (busCont) busCont.style.display = 'block';
    }

    function prepararSelectoresNuevoPrestamo() {
        const form = document.getElementById('formNuevoPrestamo');
        if (form) form.reset();

        cargarMaterialesSelect();
        cargarIntegrantesSelect();
        cargarProyectosSelect();

        limpiarSeleccionMaterialPrestamo();

        const canPickAnyone = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        const badgeRol = document.getElementById('prestamoSolicitanteBadgeRol');

        if (!canPickAnyone) {
            let integActual = cacheIntegrantes.find(i => i.id === userId || (userCi && i.ci === userCi) || (userEmail && i.email === userEmail));
            if (!integActual) {
                integActual = {
                    id: userId,
                    nombreCompleto: userName,
                    ci: userCi || '',
                    carnetUniversitario: '',
                    rol: userRole
                };
            }
            seleccionarIntegranteEnPrestamo(integActual, true);
            if (badgeRol) {
                badgeRol.textContent = 'Tu usuario (Bloqueado)';
                badgeRol.className = 'badge bg-primary-subtle text-primary border border-primary-subtle fw-semibold';
            }
        } else {
            limpiarSeleccionIntegrantePrestamo();
            if (badgeRol) {
                badgeRol.textContent = 'Buscar por nombre o CI';
                badgeRol.className = 'badge bg-light text-secondary border fw-normal';
            }
        }

        const fechaDevolucion = new Date();
        fechaDevolucion.setDate(fechaDevolucion.getDate() + 7);
        const fechaDevInput = document.getElementById('prestamoFechaDevolucionInput');
        if (fechaDevInput) {
            fechaDevInput.value = fechaDevolucion.toISOString().substring(0, 10);
        }
    }

    // ========================================================
    // 5. CARGA Y CRUD DE PRESTAMOS
    // ========================================================
    async function cargarPrestamos() {
        try {
            const res = await window.Auth.authFetch('/prestamos', { method: 'GET' });
            if (!res.ok) throw new Error('No se pudo obtener el listado de prestamos.');
            cachePrestamos = await res.json();
            renderTablaPrestamos(cachePrestamos);
            renderTablaAprobaciones(cachePrestamos);
            renderDashboardResumenPrestamos(cachePrestamos);
            actualizarKpis();
        } catch (err) {
            console.error('[Dashboard] Error cargando prestamos:', err);
            renderErrorEnTabla('tablaPrestamosCompleta', 7, 'Error al conectar con /api/prestamos.');
            renderErrorEnTabla('tablaAprobarPrestamos', 7, 'Error al conectar con /api/prestamos.');
            renderErrorEnTabla('tablaDashboardPrestamos', 4, 'Error al conectar con /api/prestamos.');
        }
    }

    function renderDashboardResumenPrestamos(prestamos) {
        const tbody = document.getElementById('tablaDashboardPrestamos');
        if (!tbody) return;

        if (!prestamos || prestamos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center py-3 text-muted">No hay prestamos activos registrados.</td></tr>`;
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

        const canManage = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        let listaPrestamos = prestamos || [];

        // Si es miembro sin autorizacion directiva, mostrar unicamente sus prestamos personales
        if (!canManage) {
            listaPrestamos = listaPrestamos.filter(p =>
                (userId && p.integranteId === userId) ||
                (userEmail && p.emailIntegrante && p.emailIntegrante.toLowerCase() === userEmail.toLowerCase())
            );
        }

        const contador = document.getElementById('contadorPrestamos');
        if (contador) {
            if (canManage) {
                contador.innerHTML = `<i class="bi bi-list-check me-1"></i> ${listaPrestamos.length} de ${cachePrestamos.length} prestamos`;
            } else {
                contador.innerHTML = `<i class="bi bi-person-check me-1"></i> ${listaPrestamos.length} solicitudes personales`;
            }
        }

        if (listaPrestamos.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted"><i class="bi bi-funnel me-1"></i>${canManage ? 'No se encontraron prestamos con los filtros seleccionados.' : 'No tienes solicitudes o prestamos registrados todavia.'}</td></tr>`;
            return;
        }

        tbody.innerHTML = listaPrestamos.map(p => {
            const badgeClass = getBadgeEstadoPrestamo(p.estado);
            const materialesText = p.detalles && p.detalles.length > 0
                ? p.detalles.map(d => `${escapeHtml(d.nombreMaterial)} (x${d.cantidad})`).join(', ')
                : (p.descripcion || 'Sin detalle de materiales');

            let extraActions = '';
            if (canManage) {
                if (p.estado === 'SOLICITADO') {
                    extraActions += `
                        <button class="btn btn-warning text-dark fw-semibold btn-modal-aprobar" data-id="${p.prestamoId}" title="Aprobar Solicitud (Bloqueo Pesimista)">
                            <i class="bi bi-shield-check"></i>
                        </button>
                        <button class="btn btn-outline-danger btn-rechazar-prestamo" data-id="${p.prestamoId}" title="Rechazar Solicitud">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    `;
                } else if (p.estado === 'APROBADO') {
                    extraActions += `
                        <button class="btn btn-outline-primary btn-entregar-prestamo" data-id="${p.prestamoId}" title="Marcar como Entregado al Solicitante">
                            <i class="bi bi-box-arrow-right"></i>
                        </button>
                        <button class="btn btn-outline-secondary btn-cancelar-prestamo" data-id="${p.prestamoId}" title="Cancelar Prestamo y Liberar Stock">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    `;
                } else if (p.estado === 'ENTREGADO' || p.estado === 'VENCIDO') {
                    extraActions += `
                        <button class="btn btn-outline-success btn-devolver-prestamo" data-id="${p.prestamoId}" title="Registrar Devolucion al Laboratorio">
                            <i class="bi bi-box-arrow-in-left"></i>
                        </button>
                    `;
                }
            } else {
                if (p.estado === 'SOLICITADO') {
                    extraActions += `
                        <button class="btn btn-outline-danger btn-cancelar-prestamo" data-id="${p.prestamoId}" title="Cancelar mi solicitud">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    `;
                }
            }

            return `
                <tr id="row-prestamo-${p.prestamoId}">
                    <td class="ps-4 fw-bold text-primary">#${p.prestamoId}</td>
                    <td>
                        <span class="fw-semibold d-block">${escapeHtml(p.nombreIntegrante || 'Miembro')}</span>
                        <small class="text-muted">${escapeHtml(p.emailIntegrante || '')}</small>
                    </td>
                    <td><span class="badge bg-light text-dark border">${escapeHtml(p.nombreProyecto || 'Prestamo General')}</span></td>
                    <td><div class="small fw-medium">${materialesText}</div></td>
                    <td><small class="text-muted"><i class="bi bi-calendar-event me-1"></i>${p.fechaDevolucionPrevista ? p.fechaDevolucionPrevista.substring(0, 10) : 'Pendiente'}</small></td>
                    <td><span class="badge ${badgeClass} py-1 px-2">${p.estado}</span></td>
                    <td class="text-end pe-4">
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-outline-info btn-ver-prestamo" data-id="${p.prestamoId}" title="Ver Detalle">
                                <i class="bi bi-eye"></i>
                            </button>
                            ${extraActions}
                            ${canManage ? `
                                <button class="btn btn-outline-primary btn-editar-prestamo" data-id="${p.prestamoId}" title="Gestionar / Estado">
                                    <i class="bi bi-sliders"></i>
                                </button>
                                <button class="btn btn-outline-danger btn-eliminar-prestamo" data-id="${p.prestamoId}" title="Cancelar / Eliminar">
                                    <i class="bi bi-trash"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        if (canManage) {
            attachModalAprobarListeners();
        }
        attachPrestamoActionListeners();
    }

    function attachPrestamoActionListeners() {
        document.querySelectorAll('.btn-ver-prestamo').forEach(btn => {
            btn.onclick = () => {
                const id = Number(btn.getAttribute('data-id'));
                const p = cachePrestamos.find(x => x.prestamoId === id);
                if (!p) return;

                document.getElementById('verPrestamoIdLabel').textContent = `#${p.prestamoId}`;
                const cuerpo = document.getElementById('cuerpoVerPrestamo');
                if (cuerpo) {
                    const badgeClass = getBadgeEstadoPrestamo(p.estado);
                    const detallesList = p.detalles && p.detalles.length > 0
                        ? p.detalles.map(d => `<li class="list-group-item d-flex justify-content-between align-items-center">
                            <div><strong>${escapeHtml(d.nombreMaterial)}</strong> <small class="text-muted">(${escapeHtml(d.marca || '')} ${escapeHtml(d.modelo || '')})</small></div>
                            <span class="badge bg-primary rounded-pill">Cantidad: ${d.cantidad}</span>
                          </li>`).join('')
                        : '<li class="list-group-item text-muted">Sin materiales detallados</li>';

                    cuerpo.innerHTML = `
                        <div class="d-flex align-items-center justify-content-between mb-3 border-bottom pb-2">
                            <div>
                                <h6 class="mb-0 fw-bold">Solicitante: ${escapeHtml(p.nombreIntegrante)}</h6>
                                <small class="text-muted">${escapeHtml(p.emailIntegrante || '')}</small>
                            </div>
                            <span class="badge ${badgeClass} fs-6">${p.estado}</span>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Proyecto Vinculado:</small>
                            <strong>${escapeHtml(p.nombreProyecto || 'Prestamo General de Laboratorio')}</strong>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block">Fechas del Prestamo:</small>
                            <div class="row g-2 small mt-1">
                                <div class="col-6"><strong>Solicitud:</strong> ${p.fechaSolicitud ? p.fechaSolicitud.replace('T', ' ') : '-'}</div>
                                <div class="col-6"><strong>Devolucion Prevista:</strong> ${p.fechaDevolucionPrevista ? p.fechaDevolucionPrevista.substring(0, 10) : 'Pendiente'}</div>
                                <div class="col-6"><strong>Entrega Real:</strong> ${p.fechaEntrega ? p.fechaEntrega.replace('T', ' ') : 'Aun no entregado'}</div>
                                <div class="col-6"><strong>Devolucion Real:</strong> ${p.fechaDevolucionReal ? p.fechaDevolucionReal.replace('T', ' ') : 'No devuelto'}</div>
                            </div>
                        </div>
                        <div class="mb-3">
                            <small class="text-muted d-block mb-1">Materiales Incluidos:</small>
                            <ul class="list-group list-group-flush border rounded-3">${detallesList}</ul>
                        </div>
                        ${p.descripcion ? `<div class="alert alert-light border small mb-0"><strong>Observaciones:</strong> ${escapeHtml(p.descripcion)}</div>` : ''}
                    `;
                }
                getModal('modalVerPrestamo')?.show();
            };
        });

        document.querySelectorAll('.btn-editar-prestamo').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para gestionar estados de prestamos.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                const p = cachePrestamos.find(x => x.prestamoId === id);
                if (!p) return;

                document.getElementById('editarPrestamoIdLabel').textContent = `#${p.prestamoId}`;
                document.getElementById('editarPrestamoIdInput').value = p.prestamoId;
                document.getElementById('editarPrestamoEstadoSelect').value = p.estado || 'SOLICITADO';
                document.getElementById('editarPrestamoFechaDevolucionInput').value = p.fechaDevolucionPrevista ? p.fechaDevolucionPrevista.substring(0, 10) : '';
                document.getElementById('editarPrestamoDescripcionInput').value = p.descripcion || '';

                getModal('modalEditarPrestamo')?.show();
            };
        });

        document.querySelectorAll('.btn-eliminar-prestamo').forEach(btn => {
            btn.onclick = () => {
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para cancelar o eliminar prestamos.', 'warning');
                    return;
                }
                const id = Number(btn.getAttribute('data-id'));
                abrirModalConfirmarEliminacion(
                    `Deseas cancelar o eliminar el prestamo <strong>#${id}</strong>? Si ya tiene transacciones activas, su estado cambiara a CANCELADO.`,
                    async () => {
                        try {
                            const res = await window.Auth.authFetch(`/prestamos/${id}`, { method: 'DELETE' });
                            if (!res.ok) {
                                const errData = await res.json().catch(() => null);
                                showNotification(errData?.message || 'No se pudo cancelar el prestamo.', 'danger');
                            } else {
                                showNotification(`Prestamo #${id} procesado correctamente.`, 'success');
                                await cargarPrestamos();
                                await cargarInventario();
                            }
                        } catch (e) {
                            showNotification('Error al procesar el prestamo.', 'danger');
                        }
                    }
                );
            };
        });

        document.querySelectorAll('.btn-entregar-prestamo').forEach(btn => {
            btn.onclick = () => entregarPrestamo(Number(btn.getAttribute('data-id')));
        });

        document.querySelectorAll('.btn-devolver-prestamo').forEach(btn => {
            btn.onclick = () => devolverPrestamo(Number(btn.getAttribute('data-id')));
        });

        document.querySelectorAll('.btn-cancelar-prestamo').forEach(btn => {
            btn.onclick = () => cancelarPrestamo(Number(btn.getAttribute('data-id')));
        });

        document.querySelectorAll('.btn-rechazar-prestamo').forEach(btn => {
            btn.onclick = () => rechazarPrestamo(Number(btn.getAttribute('data-id')));
        });
    }

    async function entregarPrestamo(id) {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            showNotification('No tienes autorizacion para entregar materiales.', 'warning');
            return;
        }
        abrirModalConfirmarEliminacion(
            `Confirmas la <strong>entrega física</strong> de los materiales del préstamo <strong>#${id}</strong> al solicitante? El estado pasará a <strong>ENTREGADO</strong>.`,
            async () => {
                try {
                    const res = await window.Auth.authFetch(`/prestamos/${id}/entregar`, { method: 'POST' });
                    const data = await res.json().catch(() => null);
                    if (res.ok) {
                        showNotification(`Préstamo #${id} marcado como ENTREGADO con éxito!`, 'success');
                        getModal('modalConfirmarEliminacion')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    } else {
                        showNotification(data?.message || 'Error al marcar como entregado.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error al comunicar con el servidor.', 'danger');
                }
            }
        );
    }

    async function devolverPrestamo(id) {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            showNotification('No tienes autorizacion para registrar devoluciones.', 'warning');
            return;
        }
        abrirModalConfirmarEliminacion(
            `Confirmas la <strong>devolución física</strong> de los componentes del préstamo <strong>#${id}</strong> al laboratorio? El estado pasará a <strong>DEVUELTO</strong> y se reincorporará el stock disponible.`,
            async () => {
                try {
                    const res = await window.Auth.authFetch(`/prestamos/${id}/devolver`, { method: 'POST' });
                    const data = await res.json().catch(() => null);
                    if (res.ok) {
                        showNotification(`Préstamo #${id} devuelto correctamente. Stock reincorporado!`, 'success');
                        getModal('modalConfirmarEliminacion')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    } else {
                        showNotification(data?.message || 'Error al procesar la devolución.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error al comunicar con el servidor.', 'danger');
                }
            }
        );
    }

    async function cancelarPrestamo(id) {
        abrirModalConfirmarEliminacion(
            `Deseas cancelar el préstamo <strong>#${id}</strong>? Las reservas de materiales se liberarán inmediatamente.`,
            async () => {
                try {
                    const res = await window.Auth.authFetch(`/prestamos/${id}/cancelar`, { method: 'POST' });
                    const data = await res.json().catch(() => null);
                    if (res.ok) {
                        showNotification(`Préstamo #${id} cancelado correctamente.`, 'info');
                        getModal('modalConfirmarEliminacion')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    } else {
                        showNotification(data?.message || 'Error al cancelar el préstamo.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error al comunicar con el servidor.', 'danger');
                }
            }
        );
    }

    async function rechazarPrestamo(id) {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            showNotification('No tienes autorizacion para rechazar solicitudes.', 'warning');
            return;
        }
        abrirModalConfirmarEliminacion(
            `Deseas rechazar la solicitud de préstamo <strong>#${id}</strong>?`,
            async () => {
                try {
                    const res = await window.Auth.authFetch(`/prestamos/${id}/rechazar`, { method: 'POST' });
                    const data = await res.json().catch(() => null);
                    if (res.ok) {
                        showNotification(`Solicitud #${id} rechazada.`, 'info');
                        getModal('modalConfirmarEliminacion')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    } else {
                        showNotification(data?.message || 'Error al rechazar la solicitud.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error al comunicar con el servidor.', 'danger');
                }
            }
        );
    }

    function renderTablaAprobaciones(prestamos) {
        const tbody = document.getElementById('tablaAprobarPrestamos');
        if (!tbody) return;

        const solicitados = prestamos.filter(p => p.estado === 'SOLICITADO');
        const totalSolicitados = cachePrestamos.filter(p => p.estado === 'SOLICITADO').length;

        const contador = document.getElementById('contadorAprobaciones');
        if (contador) {
            contador.innerHTML = `<i class="bi bi-hourglass-split text-warning me-1"></i> ${solicitados.length} de ${totalSolicitados} pendientes`;
        }

        if (solicitados.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted"><i class="bi bi-info-circle me-1"></i> No hay solicitudes pendientes que coincidan con el filtro.</td></tr>`;
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
                        <div class="btn-group btn-group-sm">
                            <button class="btn btn-sm btn-success fw-semibold btn-modal-aprobar" data-id="${p.prestamoId}">
                                <i class="bi bi-shield-check me-1"></i> Aprobar con Bloqueo
                            </button>
                            <button class="btn btn-sm btn-outline-danger fw-semibold btn-rechazar-prestamo" data-id="${p.prestamoId}">
                                <i class="bi bi-x-circle me-1"></i> Rechazar
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        attachModalAprobarListeners();
        tbody.querySelectorAll('.btn-rechazar-prestamo').forEach(btn => {
            btn.onclick = () => rechazarPrestamo(Number(btn.getAttribute('data-id')));
        });
    }

    function attachModalAprobarListeners() {
        const buttons = document.querySelectorAll('.btn-modal-aprobar');
        const modalEl = document.getElementById('modalAprobarPrestamo');
        const modalText = document.getElementById('modalPrestamoIdText');
        const btnConfirm = document.getElementById('btnConfirmarAprobacion');

        if (!modalEl || !buttons.length) return;
        const modalInstance = getModal('modalAprobarPrestamo');

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
                            observacion: 'Aprobacion autorizada desde el Panel Directivo'
                        })
                    });

                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        const errMsg = (data && data.message) ? data.message : 'Error al procesar la aprobacion.';
                        showNotification(errMsg, 'danger');
                    } else {
                        showNotification(`¡Préstamo #${prestamoSeleccionadoId} aprobado exitosamente!`, 'success');
                        modalInstance.hide();
                        await cargarPrestamos();
                    }
                } catch (e) {
                    showNotification('Error de comunicacion con el backend.', 'danger');
                } finally {
                    btnConfirm.disabled = false;
                    btnConfirm.innerHTML = `<i class="bi bi-check-lg me-1"></i> Aprobar Prestamo`;
                }
            };
        }
    }

    function getBadgeEstadoPrestamo(estado) {
        if (estado === 'SOLICITADO') return 'bg-warning text-dark';
        if (estado === 'APROBADO') return 'bg-info text-dark';
        if (estado === 'ENTREGADO') return 'bg-primary text-white';
        if (estado === 'DEVUELTO') return 'bg-success text-white';
        if (estado === 'CANCELADO') return 'bg-danger text-white';
        return 'bg-secondary text-white';
    }

    // ========================================================
    // 6. INICIALIZAR FORMULARIOS CRUD
    // ========================================================
    function initCrudForms() {
        // A) Boton Nuevo Material
        const btnNuevoMaterial = document.getElementById('btnNuevoMaterial');
        if (btnNuevoMaterial) {
            btnNuevoMaterial.onclick = () => {
                document.getElementById('modalMaterialTitulo').innerHTML = `<i class="bi bi-plus-circle me-2"></i>Nuevo Componente`;
                document.getElementById('formMaterial').reset();
                document.getElementById('materialIdInput').value = '';
                actualizarSelectCategorias();
                getModal('modalMaterial')?.show();
            };
        }

        // B) Boton Categorias
        const btnCategorias = document.getElementById('btnGestionarCategorias');
        if (btnCategorias) {
            btnCategorias.onclick = () => {
                renderTablaModalCategorias();
                getModal('modalGestionarCategorias')?.show();
            };
        }

        // Formulario Guardar Material (Crear / Editar)
        const formMaterial = document.getElementById('formMaterial');
        if (formMaterial) {
            formMaterial.onsubmit = async function (e) {
                e.preventDefault();
                const id = document.getElementById('materialIdInput').value;
                const isEdit = Boolean(id);

                const payload = {
                    nombre: document.getElementById('materialNombreInput').value.trim(),
                    categoriaId: Number(document.getElementById('materialCategoriaSelect').value),
                    marca: document.getElementById('materialMarcaInput').value.trim(),
                    modelo: document.getElementById('materialModeloInput').value.trim(),
                    cantidadTotal: Number(document.getElementById('materialCantidadInput').value),
                    estado: document.getElementById('materialEstadoSelect').value,
                    ubicacion: document.getElementById('materialUbicacionInput').value.trim(),
                    descripcion: document.getElementById('materialDescripcionInput').value.trim()
                };

                const url = isEdit ? `/materiales/${id}` : '/materiales';
                const method = isEdit ? 'PUT' : 'POST';

                try {
                    const res = await window.Auth.authFetch(url, {
                        method,
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        showNotification(data?.message || 'Error al guardar el material.', 'danger');
                    } else {
                        showNotification(isEdit ? 'Componente actualizado exitosamente!' : 'Nuevo componente registrado en inventario!', 'success');
                        getModal('modalMaterial')?.hide();
                        await cargarInventario();
                    }
                } catch (err) {
                    showNotification('Error de conexion al guardar material.', 'danger');
                }
            };
        }

        // Formulario Crear / Editar Categoria
        const formNuevaCat = document.getElementById('formNuevaCategoria');
        if (formNuevaCat) {
            formNuevaCat.onsubmit = async function (e) {
                e.preventDefault();
                const id = document.getElementById('inputCategoriaId').value;
                const nombre = document.getElementById('inputNombreCategoria').value.trim();
                const descripcion = document.getElementById('inputDescCategoria').value.trim();

                try {
                    const url = id ? `/categorias/${id}` : '/categorias';
                    const method = id ? 'PUT' : 'POST';
                    const res = await window.Auth.authFetch(url, {
                        method: method,
                        body: JSON.stringify({ nombre, descripcion })
                    });
                    if (res.ok) {
                        showNotification(id ? 'Categoria actualizada exitosamente.' : 'Categoria creada exitosamente.', 'success');
                        resetFormCategoria();
                        await cargarCategorias();
                        renderTablaModalCategorias();
                    } else {
                        const err = await res.json().catch(() => null);
                        showNotification(err?.message || 'Error al guardar categoria.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error al conectar con /api/categorias.', 'danger');
                }
            };
        }

        const btnCancelarCat = document.getElementById('btnCancelarEdicionCategoria');
        if (btnCancelarCat) {
            btnCancelarCat.onclick = () => {
                resetFormCategoria();
            };
        }

        // E) Mi Perfil de Usuario
        const btnAbrirMiPerfil = document.getElementById('btnAbrirMiPerfil');
        if (btnAbrirMiPerfil) {
            btnAbrirMiPerfil.onclick = async () => {
                try {
                    const res = await window.Auth.authFetch('/integrantes/perfil');
                    if (res.ok) {
                        const perfil = await res.json();
                        const setVal = (id, val) => {
                            const el = document.getElementById(id);
                            if (!el) return;
                            if ('value' in el) el.value = val;
                            el.textContent = val;
                        };

                        setVal('perfilNombreInput', perfil.nombre || '');
                        setVal('perfilApellidoInput', perfil.apellido || '');
                        setVal('perfilEmailInput', perfil.email || '');
                        setVal('perfilTelefonoInput', perfil.telefono || '');
                        setVal('perfilPasswordInput', '');
                        setVal('perfilConfirmPasswordInput', '');

                        setVal('perfilCiText', perfil.ci || '-');
                        setVal('perfilCarnetText', perfil.carnetUniversitario || '-');
                        setVal('perfilCarreraText', (perfil.carrera || '-').replace(/_/g, ' '));
                        setVal('perfilRolText', perfil.rol || '-');

                        const nfcEl = document.getElementById('perfilNfcText');
                        if (nfcEl) {
                            nfcEl.textContent = perfil.nfcUid || 'No asignada';
                        }

                        // Poblar Hero Card en Modal Mi Perfil
                        const heroAvatar = document.getElementById('perfilModalAvatar');
                        if (heroAvatar) {
                            const n1 = (perfil.nombre || 'U').charAt(0).toUpperCase();
                            const n2 = (perfil.apellido || '').charAt(0).toUpperCase();
                            heroAvatar.textContent = `${n1}${n2}`;
                        }
                        const heroNombre = document.getElementById('perfilModalNombre');
                        if (heroNombre) {
                            heroNombre.textContent = `${perfil.nombre || ''} ${perfil.apellido || ''}`.trim() || 'Mi Perfil';
                        }
                        const heroRolBadge = document.getElementById('perfilModalRolBadge');
                        if (heroRolBadge) {
                            heroRolBadge.textContent = perfil.rol || 'MIEMBRO';
                        }
                        const heroEmail = document.getElementById('perfilModalEmail');
                        if (heroEmail) {
                            heroEmail.textContent = perfil.email || '';
                        }

                        getModal('modalMiPerfil')?.show();
                    } else {
                        showNotification('No se pudo cargar la informacion de tu perfil.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error de conexion al consultar tu perfil.', 'danger');
                }
            };
        }

        const formMiPerfil = document.getElementById('formMiPerfil');
        if (formMiPerfil) {
            formMiPerfil.onsubmit = async function (e) {
                e.preventDefault();
                const pass = document.getElementById('perfilPasswordInput').value.trim();
                const confirmPass = document.getElementById('perfilConfirmPasswordInput').value.trim();

                if (pass && pass.length < 4) {
                    showNotification('La nueva contrasena debe tener al menos 4 caracteres.', 'warning');
                    return;
                }
                if (pass && pass !== confirmPass) {
                    showNotification('Las contrasenas ingresadas no coinciden.', 'warning');
                    return;
                }

                const payload = {
                    nombre: document.getElementById('perfilNombreInput').value.trim(),
                    apellido: document.getElementById('perfilApellidoInput').value.trim(),
                    email: document.getElementById('perfilEmailInput').value.trim(),
                    telefono: document.getElementById('perfilTelefonoInput').value.trim()
                };

                if (pass) {
                    payload.password = pass;
                }

                try {
                    const res = await window.Auth.authFetch('/integrantes/perfil', {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (res.ok) {
                        showNotification('Perfil actualizado exitosamente.', 'success');
                        getModal('modalMiPerfil')?.hide();

                        // Actualizar estado de sesion y UI
                        userName = `${data.nombre} ${data.apellido}`;
                        userEmail = data.email;
                        currentUser = window.Auth.getUser() || {};
                        currentUser.nombreCompleto = userName;
                        currentUser.email = userEmail;
                        if (window.Auth.setUser) {
                            window.Auth.setUser(currentUser);
                        }
                        initUserInfoUI();

                        await cargarIntegrantes();
                    } else {
                        showNotification(data?.message || 'Error al actualizar tu perfil.', 'danger');
                    }
                } catch (e) {
                    showNotification('Error de conexion al actualizar tu perfil.', 'danger');
                }
            };
        }

        // C) Boton Nuevo Integrante
        const btnNuevoIntegrante = document.getElementById('btnNuevoIntegrante');
        if (btnNuevoIntegrante) {
            btnNuevoIntegrante.onclick = () => {
                document.getElementById('modalIntegranteTitulo').innerHTML = `<i class="bi bi-person-plus-fill me-2"></i>Nuevo Integrante`;
                document.getElementById('formIntegrante').reset();
                document.getElementById('integranteIdInput').value = '';
                getModal('modalIntegrante')?.show();
            };
        }

        // Formulario Guardar Integrante (Crear / Editar)
        const formIntegrante = document.getElementById('formIntegrante');
        if (formIntegrante) {
            formIntegrante.onsubmit = async function (e) {
                e.preventDefault();
                const id = document.getElementById('integranteIdInput').value;
                const isEdit = Boolean(id);

                const payload = {
                    nombre: document.getElementById('integranteNombreInput').value.trim(),
                    apellido: document.getElementById('integranteApellidoInput').value.trim(),
                    ci: document.getElementById('integranteCiInput').value.trim(),
                    carnetUniversitario: document.getElementById('integranteCarnetInput').value.trim(),
                    telefono: document.getElementById('integranteTelefonoInput').value.trim(),
                    email: document.getElementById('integranteEmailInput').value.trim(),
                    carrera: document.getElementById('integranteCarreraSelect').value,
                    rol: document.getElementById('integranteRolSelect').value,
                    estado: document.getElementById('integranteEstadoSelect').value,
                    nfcUid: document.getElementById('integranteNfcInput').value.trim(),
                    password: document.getElementById('integrantePasswordInput').value.trim()
                };

                const url = isEdit ? `/integrantes/${id}` : '/integrantes';
                const method = isEdit ? 'PUT' : 'POST';

                try {
                    const res = await window.Auth.authFetch(url, {
                        method,
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        showNotification(data?.message || 'Error al guardar el integrante.', 'danger');
                    } else {
                        showNotification(isEdit ? 'Integrante actualizado correctamente!' : 'Nuevo integrante registrado exitosamente!', 'success');
                        getModal('modalIntegrante')?.hide();
                        await cargarIntegrantes();
                    }
                } catch (err) {
                    showNotification('Error de conexion al guardar integrante.', 'danger');
                }
            };
        }

        // D) Boton Nuevo Prestamo
        const btnNuevoPrestamo = document.getElementById('btnNuevoPrestamo');
        if (btnNuevoPrestamo) {
            btnNuevoPrestamo.onclick = () => {
                prepararSelectoresNuevoPrestamo();
                getModal('modalNuevoPrestamo')?.show();
            };
        }

        // Formulario Crear Prestamo
        const formNuevoPrestamo = document.getElementById('formNuevoPrestamo');
        if (formNuevoPrestamo) {
            formNuevoPrestamo.onsubmit = async function (e) {
                e.preventDefault();

                const integranteId = Number(document.getElementById('prestamoIntegranteSelect').value);
                const proyectoVal = document.getElementById('prestamoProyectoSelect').value;
                const materialId = Number(document.getElementById('prestamoMaterialSelect').value);
                const cantidad = Number(document.getElementById('prestamoCantidadInput').value) || 1;
                const fechaDev = document.getElementById('prestamoFechaDevolucionInput').value;
                const descripcion = document.getElementById('prestamoDescripcionInput').value.trim();

                if (!integranteId) {
                    showNotification('Debes seleccionar un integrante solicitante.', 'warning');
                    return;
                }
                if (!materialId) {
                    showNotification('Debes buscar y seleccionar un componente o material del inventario.', 'warning');
                    return;
                }

                const payload = {
                    integranteId,
                    proyectoId: proyectoVal ? Number(proyectoVal) : null,
                    materialId,
                    cantidad,
                    fechaDevolucionPrevista: fechaDev ? `${fechaDev}T18:00:00` : null,
                    descripcion
                };

                try {
                    const res = await window.Auth.authFetch('/prestamos', {
                        method: 'POST',
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        showNotification(data?.message || 'Error al registrar solicitud de prestamo.', 'danger');
                    } else {
                        showNotification('Solicitud de prestamo registrada exitosamente con estado SOLICITADO!', 'success');
                        getModal('modalNuevoPrestamo')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    }
                } catch (err) {
                    showNotification('Error al enviar la solicitud de prestamo.', 'danger');
                }
            };
        }

        // Formulario Editar Prestamo (Estado / Fecha)
        const formEditarPrestamo = document.getElementById('formEditarPrestamo');
        if (formEditarPrestamo) {
            formEditarPrestamo.onsubmit = async function (e) {
                e.preventDefault();
                if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
                    showNotification('No tienes autorizacion para gestionar o modificar estados de prestamos.', 'warning');
                    return;
                }
                const id = document.getElementById('editarPrestamoIdInput').value;
                const estado = document.getElementById('editarPrestamoEstadoSelect').value;
                const fechaDev = document.getElementById('editarPrestamoFechaDevolucionInput').value;
                const descripcion = document.getElementById('editarPrestamoDescripcionInput').value.trim();

                const payload = {
                    estado,
                    fechaDevolucionPrevista: fechaDev ? `${fechaDev}T18:00:00` : null,
                    descripcion
                };

                try {
                    const res = await window.Auth.authFetch(`/prestamos/${id}`, {
                        method: 'PUT',
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (!res.ok) {
                        showNotification(data?.message || 'Error al actualizar prestamo.', 'danger');
                    } else {
                        showNotification(`Prestamo #${id} actualizado a estado ${estado}!`, 'success');
                        getModal('modalEditarPrestamo')?.hide();
                        await cargarPrestamos();
                        await cargarInventario();
                    }
                } catch (err) {
                    showNotification('Error al actualizar el prestamo.', 'danger');
                }
            };
        }

        // C) Proyecto: Nuevo Proyecto
        const btnNuevoProyecto = document.getElementById('btnNuevoProyecto');
        if (btnNuevoProyecto) {
            btnNuevoProyecto.onclick = () => {
                document.getElementById('modalProyectoTitulo').innerHTML = `<i class="bi bi-kanban me-2"></i>Registrar Proyecto`;
                document.getElementById('formProyecto').reset();
                document.getElementById('proyectoId').value = '';
                document.getElementById('proyectoFechaInicio').value = new Date().toISOString().split('T')[0];
                document.getElementById('proyectoEstado').value = 'ACTIVO';
                getModal('modalProyecto')?.show();
            };
        }

        const modalProyectoEl = document.getElementById('modalProyecto');
        if (modalProyectoEl) {
            modalProyectoEl.addEventListener('show.bs.modal', () => {
                const idInput = document.getElementById('proyectoId');
                if (!idInput || !idInput.value) {
                    const titulo = document.getElementById('modalProyectoTitulo');
                    if (titulo) titulo.innerHTML = `<i class="bi bi-kanban me-2"></i>Registrar Proyecto`;
                    const form = document.getElementById('formProyecto');
                    if (form) form.reset();
                    if (idInput) idInput.value = '';
                    const fechaInput = document.getElementById('proyectoFechaInicio');
                    if (fechaInput) fechaInput.value = new Date().toISOString().split('T')[0];
                    const estadoInput = document.getElementById('proyectoEstado');
                    if (estadoInput) estadoInput.value = 'ACTIVO';
                }
            });
        }

        // Proyecto: Recargar
        const btnRecargarProyectos = document.getElementById('btnRecargarProyectos');
        if (btnRecargarProyectos) {
            btnRecargarProyectos.onclick = () => cargarProyectos();
        }

        // Proyecto: Guardar Formulario (Crear / Editar)
        const formProyecto = document.getElementById('formProyecto');
        if (formProyecto) {
            formProyecto.onsubmit = async function (e) {
                e.preventDefault();
                const id = document.getElementById('proyectoId').value;
                const nombre = document.getElementById('proyectoNombre').value.trim();
                const descripcion = document.getElementById('proyectoDescripcion').value.trim();
                const fechaInicio = document.getElementById('proyectoFechaInicio').value;
                const fechaFin = document.getElementById('proyectoFechaFin').value || null;
                const estado = document.getElementById('proyectoEstado').value;

                if (!nombre || !fechaInicio) {
                    showNotification('El nombre y la fecha de inicio son obligatorios.', 'warning');
                    return;
                }

                const payload = { nombre, descripcion, fechaInicio, fechaFin, estado };
                const btnSubmit = document.getElementById('btnGuardarProyecto');
                if (btnSubmit) btnSubmit.disabled = true;

                try {
                    const url = id ? `/proyectos/${id}` : '/proyectos';
                    const method = id ? 'PUT' : 'POST';
                    const res = await window.Auth.authFetch(url, {
                        method,
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json().catch(() => null);

                    if (res.ok) {
                        showNotification(id ? 'Proyecto actualizado exitosamente!' : 'Proyecto creado exitosamente!', 'success');
                        getModal('modalProyecto')?.hide();
                        await cargarProyectos();
                    } else {
                        showNotification(data?.message || 'Error al procesar el proyecto.', 'danger');
                    }
                } catch (err) {
                    showNotification('Error al conectar con el servidor.', 'danger');
                } finally {
                    if (btnSubmit) btnSubmit.disabled = false;
                }
            };
        }

        // Proyecto: Buscador en tiempo real
        const inputBuscarProyecto = document.getElementById('inputBuscarProyecto');
        if (inputBuscarProyecto) {
            inputBuscarProyecto.addEventListener('input', (e) => {
                const term = e.target.value.toLowerCase().trim();
                if (!term) {
                    renderTablaProyectos(cacheProyectos);
                } else {
                    const filtrados = cacheProyectos.filter(p =>
                        (p.nombre && p.nombre.toLowerCase().includes(term)) ||
                        (p.descripcion && p.descripcion.toLowerCase().includes(term)) ||
                        (p.estado && p.estado.toLowerCase().includes(term))
                    );
                    renderTablaProyectos(filtrados);
                }
            });
        }

        // Modal Confirmar Eliminacion Accion
        const btnConfirmElim = document.getElementById('btnConfirmarEliminacionAccion');
        if (btnConfirmElim) {
            btnConfirmElim.onclick = async () => {
                if (typeof accionEliminarPendiente === 'function') {
                    btnConfirmElim.disabled = true;
                    try {
                        await accionEliminarPendiente();
                    } finally {
                        btnConfirmElim.disabled = false;
                        accionEliminarPendiente = null;
                        getModal('modalConfirmarEliminacion')?.hide();
                    }
                }
            };
        }
    }

    function abrirModalConfirmarEliminacion(mensajeHtml, accionCallback) {
        const texto = document.getElementById('textoConfirmarEliminacion');
        if (texto) texto.innerHTML = mensajeHtml;
        accionEliminarPendiente = accionCallback;
        getModal('modalConfirmarEliminacion')?.show();
    }

    // ========================================================
    // 7. CONTROL DE ASISTENCIA Y SIMULADOR RFID
    // ========================================================
    async function cargarAsistencias() {
        if (!window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA')) {
            cacheAsistencias = [];
            return;
        }
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

        const hoy = new Date();
        const ingresosHoy = (asistencias || []).filter(a => {
            const tipo = a.tipoMarcacion || a.tipo || 'ENTRADA';
            if (tipo !== 'ENTRADA') return false;
            if (!a.fechaHora) return false;
            const f = new Date(a.fechaHora);
            return f.getDate() === hoy.getDate() &&
                   f.getMonth() === hoy.getMonth() &&
                   f.getFullYear() === hoy.getFullYear();
        });

        if (ingresosHoy.length === 0) {
            tbody.innerHTML = `<tr><td colspan="3" class="text-center py-3 text-muted"><i class="bi bi-info-circle me-1"></i>No se registran ingresos en el día de hoy.</td></tr>`;
            return;
        }

        const ordenadas = [...ingresosHoy].sort((x, y) => {
            const idX = x.asistenciaId || x.id || 0;
            const idY = y.asistenciaId || y.id || 0;
            return idY - idX;
        }).slice(0, 5);

        tbody.innerHTML = ordenadas.map(a => {
            const horaFormateada = a.fechaHora ? new Date(a.fechaHora).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
            const nombre = a.nombreIntegrante || a.integranteNombre || ('Integrante #' + (a.integranteId || '?'));
            const idIntegrante = a.integranteId || '-';

            return `
                <tr>
                    <td class="ps-3">
                        <span class="fw-semibold text-dark">${escapeHtml(nombre)}</span>
                        <div class="text-muted small">ID: #${idIntegrante}</div>
                    </td>
                    <td><small class="text-muted">${horaFormateada}</small></td>
                    <td><span class="badge bg-success py-1 px-2">ENTRADA</span></td>
                </tr>
            `;
        }).join('');
    }

    function renderTablaAsistencias(asistencias) {
        const tbody = document.getElementById('tablaAsistencias');
        if (!tbody) return;

        const contador = document.getElementById('contadorAsistencias');
        if (contador) {
            contador.innerHTML = `<i class="bi bi-list-check me-1"></i> ${asistencias ? asistencias.length : 0} de ${cacheAsistencias.length} marcaciones`;
        }

        if (!asistencias || asistencias.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted"><i class="bi bi-funnel me-1"></i>No se encontraron marcaciones con los filtros seleccionados.</td></tr>`;
            return;
        }

        const ordenadas = [...asistencias].sort((x, y) => {
            const idX = x.asistenciaId || x.id || 0;
            const idY = y.asistenciaId || y.id || 0;
            return idY - idX;
        });

        tbody.innerHTML = ordenadas.map(a => {
            const tipo = a.tipoMarcacion || a.tipo || 'ENTRADA';
            const badgeClass = tipo === 'ENTRADA' ? 'bg-success' : 'bg-primary';
            const iconTipo = tipo === 'ENTRADA' ? 'bi-box-arrow-in-right' : 'bi-box-arrow-right';
            const horaFormateada = a.fechaHora ? new Date(a.fechaHora).toLocaleString('es-PY') : '-';
            const nombre = a.nombreIntegrante || a.integranteNombre || ('Integrante #' + (a.integranteId || '?'));
            const idAsistencia = a.asistenciaId || a.id || '-';
            const idIntegrante = a.integranteId || '-';
            const carnet = a.carnetUniversitario || '';

            return `
                <tr>
                    <td class="ps-4">
                        <span class="badge bg-light text-dark border font-monospace">#${idAsistencia}</span>
                    </td>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <div class="rounded-circle bg-danger bg-opacity-10 text-danger p-2 d-flex align-items-center justify-content-center" style="width: 34px; height: 34px;">
                                <i class="bi bi-person-fill"></i>
                            </div>
                            <div>
                                <span class="fw-semibold text-dark d-block">${escapeHtml(nombre)}</span>
                                <div class="small text-muted d-flex align-items-center gap-1">
                                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">ID: #${idIntegrante}</span>
                                    ${carnet ? `<span class="badge bg-light text-muted border">${escapeHtml(carnet)}</span>` : ''}
                                </div>
                            </div>
                        </div>
                    </td>
                    <td>
                        <small class="text-muted"><i class="bi bi-clock me-1 text-primary"></i>${horaFormateada}</small>
                    </td>
                    <td>
                        <span class="badge ${badgeClass} py-1 px-2 d-inline-flex align-items-center gap-1">
                            <i class="bi ${iconTipo}"></i> ${tipo}
                        </span>
                    </td>
                    <td>
                        <code class="text-secondary small bg-light p-1 rounded border">${escapeHtml(a.dispositivo || 'ESP32')}</code>
                    </td>
                </tr>
            `;
        }).join('');
    }

    // ========================================================
    // MARCADOR DE ENTRADA Y ASISTENCIA MANUAL
    // ========================================================
    function initMarcadorManual() {
        const form = document.getElementById('formMarcacionManual');
        if (!form) return;

        const manualIntegranteId = document.getElementById('manualIntegranteId');
        const inputNfc = document.getElementById('inputManualNfcUid');
        const badgeNfc = document.getElementById('badgeEstadoNfc');
        const btnCapturarRfid = document.getElementById('btnCapturarRfidManual');
        const btnLimpiarRfid = document.getElementById('btnLimpiarRfidManual');
        const inputBuscar = document.getElementById('inputManualBuscarIntegrante');
        const listaResultados = document.getElementById('listaResultadosManual');
        const inputNombre = document.getElementById('inputManualNombre');
        const inputCi = document.getElementById('inputManualCi');
        const selectCarrera = document.getElementById('selectManualCarrera');
        const selectTipo = document.getElementById('selectManualTipoMarcacion');
        const inputDisp = document.getElementById('inputManualDispositivo');
        const btnLimpiar = document.getElementById('btnLimpiarManual');
        const btnRegistrar = document.getElementById('btnRegistrarManual');
        const resultadoDiv = document.getElementById('resultadoMarcacionManual');

        const collapseElem = document.getElementById('collapseMarcadorManual');
        const badgeColapso = document.getElementById('badgeEstadoMarcadorColapso');
        const textoBotonColapso = document.getElementById('textoBotonMarcadorColapso');

        if (collapseElem) {
            collapseElem.addEventListener('show.bs.collapse', () => {
                if (badgeColapso) {
                    badgeColapso.className = 'badge bg-success bg-opacity-10 text-success border border-success border-opacity-25';
                    badgeColapso.textContent = 'Formulario Abierto';
                }
                if (textoBotonColapso) {
                    textoBotonColapso.textContent = 'Ocultar';
                }
            });

            collapseElem.addEventListener('hide.bs.collapse', () => {
                if (badgeColapso) {
                    badgeColapso.className = 'badge bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25';
                    badgeColapso.textContent = 'Hacer clic para abrir';
                }
                if (textoBotonColapso) {
                    textoBotonColapso.textContent = 'Desplegar';
                }
            });
        }

        function autocompletarCampos(integ) {
            if (!integ) return;
            manualIntegranteId.value = integ.id || '';
            inputNombre.value = integ.nombreCompleto || `${integ.nombre || ''} ${integ.apellido || ''}`.trim();
            inputCi.value = integ.ci || '';
            if (integ.nfcUid) {
                inputNfc.value = integ.nfcUid;
            }
            if (integ.carrera && selectCarrera) {
                selectCarrera.value = integ.carrera;
            }
            if (badgeNfc) {
                badgeNfc.className = 'badge bg-success bg-opacity-10 text-success border border-success border-opacity-25 fw-semibold';
                badgeNfc.textContent = `Identificado: ${integ.nombre || ''} (${integ.ci || ''})`;
            }
        }

        function buscarPorUid(uidVal) {
            const uid = (uidVal || '').trim().toUpperCase();
            if (!uid) {
                if (badgeNfc) {
                    badgeNfc.className = 'badge bg-light text-secondary border fw-normal';
                    badgeNfc.textContent = 'Escanea o escribe UID';
                }
                return;
            }

            const match = cacheIntegrantes.find(i => (i.nfcUid || '').toUpperCase() === uid);
            if (match) {
                autocompletarCampos(match);
            } else {
                manualIntegranteId.value = '';
                if (badgeNfc) {
                    badgeNfc.className = 'badge bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25 fw-semibold';
                    badgeNfc.textContent = 'UID no registrado en padrón';
                }
            }
        }

        function buscarPorCi(ciVal) {
            const ci = (ciVal || '').trim();
            if (!ci) return;
            const match = cacheIntegrantes.find(i => (i.ci || '').trim() === ci);
            if (match) {
                autocompletarCampos(match);
            }
        }

        function limpiarTodo() {
            form.reset();
            manualIntegranteId.value = '';
            if (listaResultados) {
                listaResultados.style.display = 'none';
                listaResultados.innerHTML = '';
            }
            if (badgeNfc) {
                badgeNfc.className = 'badge bg-light text-secondary border fw-normal';
                badgeNfc.textContent = 'Escanea o escribe UID';
            }
            if (inputDisp) {
                inputDisp.value = 'REGISTRO_MANUAL_LAB';
            }
        }

        // 1. Escuchar cambios en campo UID
        if (inputNfc) {
            inputNfc.addEventListener('input', () => {
                buscarPorUid(inputNfc.value);
            });
            inputNfc.addEventListener('blur', () => {
                buscarPorUid(inputNfc.value);
            });
        }

        // 2. Limpiar solo UID
        if (btnLimpiarRfid) {
            btnLimpiarRfid.addEventListener('click', () => {
                inputNfc.value = '';
                buscarPorUid('');
            });
        }

        // 3. Capturar tarjeta del ESP32
        if (btnCapturarRfid) {
            btnCapturarRfid.addEventListener('click', async () => {
                btnCapturarRfid.disabled = true;
                btnCapturarRfid.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Leyendo...`;
                try {
                    const res = await window.Auth.authFetch('/asistencia/ultimo-escaneado');
                    if (res.ok) {
                        const data = await res.json();
                        if (data && data.nfcUid) {
                            if (collapseElem && !collapseElem.classList.contains('show') && typeof bootstrap !== 'undefined' && bootstrap.Collapse) {
                                bootstrap.Collapse.getOrCreateInstance(collapseElem).show();
                            }
                            inputNfc.value = data.nfcUid;
                            buscarPorUid(data.nfcUid);
                            showNotification(`Tarjeta capturada: ${data.nfcUid}`, 'success');
                        } else {
                            showNotification('No se ha detectado ninguna tarjeta reciente en el ESP32.', 'warning');
                        }
                    }
                } catch (e) {
                    showNotification('Error al consultar lecturas del ESP32.', 'danger');
                } finally {
                    btnCapturarRfid.disabled = false;
                    btnCapturarRfid.innerHTML = `<i class="bi bi-broadcast me-1"></i> Capturar`;
                }
            });
        }

        // 4. Búsqueda rápida de integrante en tiempo real (autocomplete)
        if (inputBuscar && listaResultados) {
            inputBuscar.addEventListener('input', () => {
                const term = inputBuscar.value.trim().toLowerCase();
                if (!term || term.length < 2) {
                    listaResultados.style.display = 'none';
                    listaResultados.innerHTML = '';
                    return;
                }

                const matches = cacheIntegrantes.filter(i => {
                    const n = (i.nombreCompleto || `${i.nombre || ''} ${i.apellido || ''}`).toLowerCase();
                    const ci = (i.ci || '').toLowerCase();
                    const carnet = (i.carnetUniversitario || '').toLowerCase();
                    return n.includes(term) || ci.includes(term) || carnet.includes(term);
                }).slice(0, 6);

                if (matches.length === 0) {
                    listaResultados.innerHTML = `<div class="p-2 text-muted small text-center">No se encontraron integrantes coincidentes</div>`;
                    listaResultados.style.display = 'block';
                    return;
                }

                listaResultados.innerHTML = matches.map(m => `
                    <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-2 px-3 item-sugerencia-asistente" data-id="${m.id}">
                        <div>
                            <strong class="d-block text-dark">${escapeHtml(m.nombreCompleto || m.nombre)}</strong>
                            <small class="text-muted">CI: ${escapeHtml(m.ci)} | Carrera: ${(m.carrera || '-').replace(/_/g, ' ')}</small>
                        </div>
                        <span class="badge ${m.nfcUid ? 'bg-primary' : 'bg-secondary'}">${m.nfcUid ? 'RFID OK' : 'Sin UID'}</span>
                    </button>
                `).join('');
                listaResultados.style.display = 'block';

                listaResultados.querySelectorAll('.item-sugerencia-asistente').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const id = parseInt(btn.getAttribute('data-id'));
                        const integ = cacheIntegrantes.find(x => x.id === id);
                        if (integ) {
                            autocompletarCampos(integ);
                            inputBuscar.value = '';
                            listaResultados.style.display = 'none';
                        }
                    });
                });
            });

            document.addEventListener('click', (e) => {
                if (!inputBuscar.contains(e.target) && !listaResultados.contains(e.target)) {
                    listaResultados.style.display = 'none';
                }
            });
        }

        // 5. Escuchar cambios en campo CI (si se tipea manualmente)
        if (inputCi) {
            inputCi.addEventListener('blur', () => {
                if (!manualIntegranteId.value && inputCi.value.trim()) {
                    buscarPorCi(inputCi.value.trim());
                }
            });
        }

        // 6. Botón Limpiar Todo
        if (btnLimpiar) {
            btnLimpiar.addEventListener('click', limpiarTodo);
        }

        // 7. Envío de la Marcación
        form.addEventListener('submit', async function (e) {
            e.preventDefault();

            const nombre = inputNombre ? inputNombre.value.trim() : '';
            const ci = inputCi ? inputCi.value.trim() : '';

            if (!nombre || !ci) {
                showNotification('Ingresa al menos el Nombre y la Cédula (CI) del asistente.', 'warning');
                return;
            }

            const payload = {
                integranteId: manualIntegranteId && manualIntegranteId.value ? parseInt(manualIntegranteId.value) : null,
                nfcUid: inputNfc && inputNfc.value.trim() ? inputNfc.value.trim().toUpperCase() : null,
                nombre: nombre,
                ci: ci,
                carrera: selectCarrera ? selectCarrera.value : 'COLABORADOR',
                tipoMarcacion: selectTipo ? selectTipo.value : 'AUTO',
                dispositivo: inputDisp && inputDisp.value.trim() ? inputDisp.value.trim() : 'REGISTRO_MANUAL_LAB'
            };

            btnRegistrar.disabled = true;
            btnRegistrar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Registrando...`;

            try {
                const res = await window.Auth.authFetch('/asistencia/marcar-manual', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });

                const data = await res.json().catch(() => null);

                if (!res.ok) {
                    const msg = (data && data.message) ? data.message : 'Error al registrar marcación manual.';
                    mostrarResultadoManual(false, msg, resultadoDiv);
                } else {
                    const tipo = data.tipoMarcacion || data.tipo || 'ENTRADA';
                    const nom = data.nombreIntegrante || nombre;
                    const hora = data.fechaHora ? new Date(data.fechaHora).toLocaleTimeString() : new Date().toLocaleTimeString();
                    mostrarResultadoManual(true, `Marcación de <strong>${tipo}</strong> registrada con éxito para <strong>${escapeHtml(nom)}</strong> a las ${hora}!`, resultadoDiv);

                    // Refrescar asistencias e integrantes
                    await cargarAsistencias();
                    if (!payload.integranteId) {
                        await cargarIntegrantes();
                    }
                    await cargarHorasDashboard();

                    // Limpiar formulario para la siguiente marcación
                    limpiarTodo();
                }
            } catch (err) {
                mostrarResultadoManual(false, 'Error de conexión al procesar la marcación manual.', resultadoDiv);
            } finally {
                btnRegistrar.disabled = false;
                btnRegistrar.innerHTML = `<i class="bi bi-check2-circle me-1"></i> Registrar`;
            }
        });
    }

    function mostrarResultadoManual(isSuccess, message, container) {
        if (!container) return;
        container.style.display = 'block';
        const alertClass = isSuccess ? 'alert-success' : 'alert-danger';
        const icon = isSuccess ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill';

        container.innerHTML = `
            <div class="alert ${alertClass} alert-dismissible fade show d-flex align-items-center mb-0 py-2 shadow-sm rounded-3" role="alert">
                <i class="bi ${icon} me-2 fs-5 flex-shrink-0"></i>
                <div class="flex-grow-1">${message}</div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
    }

    // ========================================================
    // 7.1 HORAS Y CUADRO DE HONOR / TIER LIST DASHBOARD
    // ========================================================
    async function cargarHorasDashboard() {
        try {
            const res = await window.Auth.authFetch('/asistencia/horas-dashboard', { method: 'GET' });
            if (res.ok) {
                cacheHorasDashboard = await res.json();
                renderHorasDashboard();
            }
        } catch (err) {
            console.error('[Dashboard] Error cargando horas y tier:', err);
        }
    }

    function renderHorasDashboard() {
        if (!cacheHorasDashboard) return;
        const esAdmin = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');
        const misHoras = cacheHorasDashboard.misHoras;

        // 1. Renderizar KPIs y Widget de Progreso para Miembro
        if (!esAdmin && misHoras) {
            const kpiHoras = document.getElementById('kpiMiembroHorasTotales');
            const kpiHorasSub = document.getElementById('kpiMiembroHorasSub');
            const kpiSesiones = document.getElementById('kpiMiembroSesiones');
            const kpiSesionesSub = document.getElementById('kpiMiembroSesionesSub');

            if (kpiHoras) kpiHoras.textContent = `${misHoras.horasTotales || 0} hrs`;
            if (kpiHorasSub) kpiHorasSub.innerHTML = `<span class="badge ${misHoras.tierBadgeClass || 'badge-tier-bronce'}">${escapeHtml(misHoras.tierNombre || 'Tier Bronce')}</span>`;
            if (kpiSesiones) kpiSesiones.textContent = `${misHoras.totalSesiones || 0} sesiones`;
            if (kpiSesionesSub) kpiSesionesSub.textContent = 'Acreditables FP-UNE';

            // Widget Mi Progreso de Horas
            const contadorGrande = document.getElementById('miContadorHorasGrande');
            const badgeGrande = document.getElementById('miTierBadgeGrande');
            const sesionesTexto = document.getElementById('miSesionesTexto');
            const barraProgreso = document.getElementById('miBarraProgresoTier');
            const progresoPorcentaje = document.getElementById('miProgresoPorcentaje');
            const progresoTextoTier = document.getElementById('miProgresoTextoTier');
            const consejoTexto = document.getElementById('miProximoTierConsejo');
            const estadoPresenciaBadge = document.getElementById('miEstadoPresenciaBadge');

            if (contadorGrande) contadorGrande.innerHTML = `${misHoras.horasTotales || 0} <span class="fs-6 fw-normal text-muted">hrs</span>`;
            if (badgeGrande) {
                badgeGrande.className = `badge ${misHoras.tierBadgeClass || 'badge-tier-bronce'} px-3 py-1 fs-6`;
                badgeGrande.textContent = misHoras.tierNombre || 'Tier Bronce';
            }
            if (sesionesTexto) sesionesTexto.textContent = `${misHoras.totalSesiones || 0} sesiones registradas en el laboratorio`;
            if (barraProgreso) {
                const pct = misHoras.porcentajeProgresoTier || 0;
                barraProgreso.style.width = `${pct}%`;
            }
            if (progresoPorcentaje) progresoPorcentaje.textContent = `${misHoras.porcentajeProgresoTier || 0}%`;
            if (progresoTextoTier) {
                if (misHoras.tier === 'DIAMANTE') {
                    progresoTextoTier.textContent = '¡Máximo Tier alcanzado! (Nivel Leyenda FP-UNE)';
                } else {
                    progresoTextoTier.textContent = `Progreso hacia el siguiente Tier (faltan ${misHoras.horasParaSiguienteTier || 0} hrs)`;
                }
            }
            if (consejoTexto && misHoras.tier !== 'DIAMANTE') {
                consejoTexto.innerHTML = `<i class="bi bi-info-circle me-1 text-primary"></i> Estás en <strong>${escapeHtml(misHoras.tierNombre || 'Tier Bronce')}</strong>. Al acumular ${misHoras.horasParaSiguienteTier || 0} horas más ascenderás de rango en el club.`;
            }
            if (estadoPresenciaBadge) {
                if (misHoras.enElLabHoy) {
                    estadoPresenciaBadge.innerHTML = '<span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2"><i class="bi bi-broadcast me-1"></i>Presente en el Laboratorio hoy</span>';
                } else {
                    estadoPresenciaBadge.innerHTML = '<span class="badge bg-light text-secondary border px-3 py-2"><i class="bi bi-door-closed me-1"></i>Sin marcación activa hoy</span>';
                }
            }
        }

        // 2. Renderizar Cuadro de Honor y Tier List para ADMIN y DIRECTIVA
        if (esAdmin) {
            const badgeTotal = document.getElementById('badgeTotalHorasLaboratorio');
            if (badgeTotal) {
                badgeTotal.innerHTML = `<i class="bi bi-clock-history text-primary me-1"></i> Total: <strong>${cacheHorasDashboard.totalHorasLaboratorioClub || 0} hrs</strong> acumuladas (${cacheHorasDashboard.totalMiembrosConHoras || 0} activos)`;
            }

            renderRankingCuadroHonor(cacheHorasDashboard.ranking || []);
        }
    }

    function renderRankingCuadroHonor(ranking) {
        const contenedorPodio = document.getElementById('contenedorPodioHonor');
        const tablaBody = document.getElementById('tablaRankingHoras');
        if (!tablaBody) return;

        if (!ranking || ranking.length === 0) {
            if (contenedorPodio) contenedorPodio.innerHTML = '';
            tablaBody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><i class="bi bi-info-circle me-1"></i>Aún no hay marcaciones de horas registradas en el laboratorio.</td></tr>';
            return;
        }

        // Renderizar Podio (Top 3)
        if (contenedorPodio) {
            const top3 = ranking.slice(0, 3);
            let htmlPodio = '';
            const medallas = [
                { pos: '1º Lugar', icon: 'bi-trophy-fill', border: 'border-warning', bg: 'bg-warning-subtle text-warning-emphasis' },
                { pos: '2º Lugar', icon: 'bi-award-fill', border: 'border-secondary', bg: 'bg-secondary-subtle text-secondary-emphasis' },
                { pos: '3º Lugar', icon: 'bi-award', border: 'border-danger border-opacity-50', bg: 'bg-danger-subtle text-danger-emphasis' }
            ];

            top3.forEach((m, idx) => {
                const med = medallas[idx] || medallas[2];
                htmlPodio += `
                    <div class="col-12 col-md-4">
                        <div class="card podio-card h-100 p-3 border ${med.border} shadow-sm position-relative overflow-hidden">
                            <div class="d-flex align-items-center gap-3">
                                <div class="podio-avatar ${med.bg}">
                                    <i class="bi ${med.icon}"></i>
                                </div>
                                <div class="flex-grow-1 min-w-0">
                                    <div class="d-flex align-items-center justify-content-between">
                                        <span class="badge ${med.bg} border fw-bold" style="font-size: 0.72rem;">${med.pos}</span>
                                        <span class="badge ${m.tierBadgeClass}" style="font-size: 0.68rem;"><i class="bi ${m.tierIcono} me-1"></i>${escapeHtml(m.tier)}</span>
                                    </div>
                                    <h6 class="fw-bold mb-0 mt-1 text-truncate" title="${escapeHtml(m.nombreCompleto)}">${escapeHtml(m.nombreCompleto)}</h6>
                                    <small class="text-muted d-block text-truncate" style="font-size: 0.75rem;">${escapeHtml(m.carrera || 'Politécnica')}</small>
                                </div>
                            </div>
                            <div class="d-flex align-items-baseline justify-content-between mt-3 pt-2 border-top">
                                <small class="text-muted"><i class="bi bi-clock me-1"></i>Horas Acumuladas</small>
                                <strong class="fs-5 text-primary fw-bold">${m.horasTotales || 0} <span class="fs-6 fw-normal text-muted">hrs</span></strong>
                            </div>
                        </div>
                    </div>
                `;
            });
            contenedorPodio.innerHTML = htmlPodio;
        }

        // Renderizar Tabla Completa
        const inputFiltro = document.getElementById('inputBuscarRanking');
        const query = (inputFiltro && inputFiltro.value ? inputFiltro.value.trim().toLowerCase() : '');

        const filtrados = ranking.filter(m => {
            if (!query) return true;
            return (m.nombreCompleto || '').toLowerCase().includes(query) ||
                   (m.carrera || '').toLowerCase().includes(query) ||
                   (m.carnet || '').toLowerCase().includes(query) ||
                   (m.tier || '').toLowerCase().includes(query);
        });

        if (filtrados.length === 0) {
            tablaBody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><i class="bi bi-search me-1"></i>No se encontraron miembros con el criterio de búsqueda.</td></tr>';
            return;
        }

        let htmlRows = '';
        filtrados.forEach(m => {
            let posBadge = `<span class="fw-bold text-muted">${m.posicion}º</span>`;
            if (m.posicion === 1) posBadge = '<span class="badge bg-warning text-dark"><i class="bi bi-trophy-fill me-1"></i>1º</span>';
            else if (m.posicion === 2) posBadge = '<span class="badge bg-secondary text-white"><i class="bi bi-award-fill me-1"></i>2º</span>';
            else if (m.posicion === 3) posBadge = '<span class="badge bg-danger-subtle text-danger"><i class="bi bi-award me-1"></i>3º</span>';

            const estadoPresencia = m.enElLabHoy
                ? '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-circle-fill me-1" style="font-size: 0.5rem;"></i>En el Lab</span>'
                : '<span class="badge bg-light text-muted border">Fuera</span>';

            htmlRows += `
                <tr>
                    <td class="ps-3 text-center">${posBadge}</td>
                    <td>
                        <div class="fw-bold text-dark">${escapeHtml(m.nombreCompleto)}</div>
                        <small class="text-muted" style="font-size: 0.72rem;">${escapeHtml(m.carnet || 'FP-UNE')}</small>
                    </td>
                    <td><span class="badge bg-light text-dark border" style="font-size: 0.72rem;">${escapeHtml(m.carrera || 'Politécnica')}</span></td>
                    <td class="text-center"><strong class="text-primary">${m.horasTotales || 0} hrs</strong></td>
                    <td class="text-center"><span class="badge bg-light text-secondary border">${m.totalSesiones || 0} sesiones</span></td>
                    <td class="text-center"><span class="badge ${m.tierBadgeClass}"><i class="bi ${m.tierIcono} me-1"></i>${escapeHtml(m.tierNombre || m.tier)}</span></td>
                    <td class="text-center pe-3">${estadoPresencia}</td>
                </tr>
            `;
        });

        tablaBody.innerHTML = htmlRows;
    }

    // ========================================================
    // AUTO-RELLENO DE TARJETA RFID EN MODAL INTEGRANTE
    // ========================================================
    let rfidPollingTimer = null;
    let rfidBaselineTimestamp = 0;

    function initRfidCaptura() {
        const modalEl = document.getElementById('modalIntegrante');
        const inputNfc = document.getElementById('integranteNfcInput');
        const btnCapturar = document.getElementById('btnCapturarRfid');
        const statusEl = document.getElementById('rfidCapturaStatus');

        if (!modalEl || !inputNfc) return;

        let grabacionInterval = null;
        let modoGrabacionActivo = false;

        async function checkUltimoEscaneo(manualClick = false) {
            if (modoGrabacionActivo) return; // No interrumpir mientras se graba

            try {
                const res = await window.Auth.authFetch('/asistencia/ultimo-escaneado', { method: 'GET' });
                if (!res.ok) return;
                const data = await res.json();

                if (!data || !data.nfcUid) {
                    if (manualClick && statusEl) {
                        statusEl.innerHTML = `<span class="text-warning"><i class="bi bi-exclamation-circle me-1"></i>Aun no se ha escaneado ninguna tarjeta en el ESP32.</span>`;
                    }
                    return;
                }

                const timestamp = data.timestamp || 0;
                if (manualClick || timestamp > rfidBaselineTimestamp) {
                    inputNfc.value = data.nfcUid;
                    inputNfc.classList.remove('is-invalid');
                    inputNfc.classList.add('is-valid');

                    // Destello visual verde en el campo
                    inputNfc.style.transition = 'background-color 0.3s ease';
                    inputNfc.style.backgroundColor = '#d1e7dd';
                    setTimeout(() => {
                        inputNfc.style.backgroundColor = '';
                    }, 800);

                    if (statusEl) {
                        const hora = timestamp > 0 ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '';
                        statusEl.innerHTML = `<span class="badge bg-success p-1 px-2"><i class="bi bi-check-circle-fill me-1"></i> Tarjeta ${escapeHtml(data.nfcUid)} detectada (${hora})</span>`;
                    }

                    rfidBaselineTimestamp = timestamp;
                }
            } catch (err) {
                console.warn('[RFID] Error al consultar /asistencia/ultimo-escaneado:', err);
            }
        }

        async function iniciarModoGrabacion(uidParaGrabar) {
            if (!uidParaGrabar) {
                showNotification('Ingresa o genera un UID para grabar en la tarjeta.', 'warning');
                return false;
            }

            const btnGrabar = document.getElementById('btnGrabarRfid');
            try {
                if (btnGrabar) {
                    btnGrabar.disabled = true;
                    btnGrabar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Grabar`;
                }

                const res = await window.Auth.authFetch('/asistencia/modo-grabacion', {
                    method: 'POST',
                    body: JSON.stringify({ nfcUid: uidParaGrabar })
                });

                if (res.ok) {
                    modoGrabacionActivo = true;
                    if (statusEl) {
                        statusEl.innerHTML = `
                            <div class="alert alert-warning py-1 px-2 mb-0 d-flex align-items-center gap-2 mt-1">
                                <span class="spinner-grow spinner-grow-sm text-warning" role="status"></span>
                                <span class="small fw-bold">MODO GRABACION ACTIVO: Acerca la tarjeta fisica al lector ESP32 para grabar [${escapeHtml(uidParaGrabar)}]...</span>
                            </div>
                        `;
                    }

                    if (grabacionInterval) clearInterval(grabacionInterval);
                    const inicioEsperado = Date.now();
                    grabacionInterval = setInterval(async () => {
                        try {
                            const checkRes = await window.Auth.authFetch('/asistencia/estado-grabacion', { method: 'GET' });
                            if (checkRes.ok) {
                                const estado = await checkRes.json();
                                if (estado.ultimoUidGrabado === uidParaGrabar && estado.timestampGrabado >= inicioEsperado) {
                                    clearInterval(grabacionInterval);
                                    grabacionInterval = null;
                                    modoGrabacionActivo = false;
                                    rfidBaselineTimestamp = Date.now();

                                    if (statusEl) {
                                        statusEl.innerHTML = `
                                            <div class="alert alert-success py-1 px-2 mb-0 d-flex align-items-center gap-2 mt-1">
                                                <i class="bi bi-check-circle-fill text-success fs-5"></i>
                                                <span class="small fw-bold">¡Tarjeta física grabada exitosamente con UID [${escapeHtml(uidParaGrabar)}]!</span>
                                            </div>
                                        `;
                                    }
                                    showNotification(`¡Tarjeta física grabada exitosamente con UID ${uidParaGrabar}!`, 'success');
                                    if (btnGrabar) {
                                        btnGrabar.disabled = false;
                                        btnGrabar.innerHTML = `<i class="bi bi-sd-card me-1"></i>Grabar`;
                                    }
                                } else if (!estado.activo && Date.now() - inicioEsperado > 60000) {
                                    clearInterval(grabacionInterval);
                                    grabacionInterval = null;
                                    modoGrabacionActivo = false;
                                    if (statusEl) {
                                        statusEl.innerHTML = `<span class="text-danger small"><i class="bi bi-exclamation-triangle me-1"></i>Tiempo de grabacion agotado (60s). Presiona Grabar para reintentar.</span>`;
                                    }
                                    if (btnGrabar) {
                                        btnGrabar.disabled = false;
                                        btnGrabar.innerHTML = `<i class="bi bi-sd-card me-1"></i>Grabar`;
                                    }
                                }
                            }
                        } catch (e) {
                            console.warn('[RFID] Error verificando estado de grabacion:', e);
                        }
                    }, 800);
                    return true;
                } else {
                    showNotification('No se pudo activar el modo grabacion en el servidor.', 'danger');
                    if (btnGrabar) {
                        btnGrabar.disabled = false;
                        btnGrabar.innerHTML = `<i class="bi bi-sd-card me-1"></i>Grabar`;
                    }
                    return false;
                }
            } catch (err) {
                showNotification('Error al conectar con el servidor para modo grabacion.', 'danger');
                if (btnGrabar) {
                    btnGrabar.disabled = false;
                    btnGrabar.innerHTML = `<i class="bi bi-sd-card me-1"></i>Grabar`;
                }
                return false;
            }
        }

        modalEl.addEventListener('shown.bs.modal', async () => {
            modoGrabacionActivo = false;
            if (statusEl) {
                statusEl.innerHTML = `<span class="text-primary"><span class="spinner-grow spinner-grow-sm me-1" role="status"></span> Esperando pase de tarjeta en el ESP32...</span>`;
            }
            inputNfc.classList.remove('is-valid', 'is-invalid');

            try {
                const res = await window.Auth.authFetch('/asistencia/ultimo-escaneado', { method: 'GET' });
                if (res.ok) {
                    const data = await res.json();
                    rfidBaselineTimestamp = (data && data.timestamp) ? data.timestamp : Date.now();
                } else {
                    rfidBaselineTimestamp = Date.now();
                }
            } catch (e) {
                rfidBaselineTimestamp = Date.now();
            }

            if (rfidPollingTimer) clearInterval(rfidPollingTimer);
            rfidPollingTimer = setInterval(() => {
                checkUltimoEscaneo(false);
            }, 1200);
        });

        modalEl.addEventListener('hidden.bs.modal', () => {
            modoGrabacionActivo = false;
            if (rfidPollingTimer) {
                clearInterval(rfidPollingTimer);
                rfidPollingTimer = null;
            }
            if (grabacionInterval) {
                clearInterval(grabacionInterval);
                grabacionInterval = null;
            }
            window.Auth.authFetch('/asistencia/cancelar-grabacion', { method: 'POST' }).catch(() => {});

            if (statusEl) {
                statusEl.innerHTML = `<i class="bi bi-broadcast text-primary me-1"></i>Acerca tarjeta al ESP32 | Generar | Grabar`;
            }
            inputNfc.classList.remove('is-valid');
        });

        if (btnCapturar) {
            btnCapturar.onclick = (e) => {
                e.preventDefault();
                checkUltimoEscaneo(true);
            };
        }

        const btnGenerar = document.getElementById('btnGenerarRfid');
        if (btnGenerar) {
            btnGenerar.onclick = async (e) => {
                e.preventDefault();
                let nuevoUid = '';
                try {
                    btnGenerar.disabled = true;
                    btnGenerar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>`;
                    const res = await window.Auth.authFetch('/asistencia/generar-uid', { method: 'GET' });
                    if (res.ok) {
                        const data = await res.json();
                        nuevoUid = data.nfcUid;
                    } else {
                        const arr = new Uint8Array(4);
                        crypto.getRandomValues(arr);
                        nuevoUid = Array.from(arr, b => b.toString(16).padStart(2, '0')).join('').toUpperCase();
                    }
                    inputNfc.value = nuevoUid;
                    inputNfc.classList.remove('is-invalid');
                    inputNfc.classList.add('is-valid');
                    inputNfc.style.transition = 'background-color 0.3s ease';
                    inputNfc.style.backgroundColor = '#d1e7dd';
                    setTimeout(() => { inputNfc.style.backgroundColor = ''; }, 800);

                    // Activar automaticamente el modo grabacion para que al acercar la tarjeta se grabe de una vez!
                    await iniciarModoGrabacion(nuevoUid);
                } catch (err) {
                    const arr = new Uint8Array(4);
                    crypto.getRandomValues(arr);
                    const fallbackUid = Array.from(arr, b => b.toString(16).padStart(2, '0')).join('').toUpperCase();
                    inputNfc.value = fallbackUid;
                    inputNfc.classList.remove('is-invalid');
                    inputNfc.classList.add('is-valid');
                    await iniciarModoGrabacion(fallbackUid);
                } finally {
                    btnGenerar.disabled = false;
                    btnGenerar.innerHTML = `<i class="bi bi-magic me-1"></i>Generar`;
                }
            };
        }

        const btnGrabar = document.getElementById('btnGrabarRfid');
        if (btnGrabar) {
            btnGrabar.onclick = async (e) => {
                e.preventDefault();
                let uidParaGrabar = inputNfc.value.trim().toUpperCase();
                if (!uidParaGrabar) {
                    if (btnGenerar) await btnGenerar.click();
                    return;
                }
                await iniciarModoGrabacion(uidParaGrabar);
            };
        }
    }

    // ========================================================
    // 8. BUSQUEDA Y FILTRADO INTEGRAL EN TODAS LAS VISTAS
    // ========================================================
    function getLocalDateString(date) {
        const d = date || new Date();
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    let modoRangoAsistencia = null; // '7dias' | 'mes' | null

    function aplicarFiltrosAsistencia() {
        if (!cacheAsistencias) return;

        const texto = (document.getElementById('filtroAsistenciaTexto')?.value || '').trim().toLowerCase();
        const fecha = (document.getElementById('filtroAsistenciaFecha')?.value || '').trim();
        const horaDesde = (document.getElementById('filtroAsistenciaHoraDesde')?.value || '').trim();
        const horaHasta = (document.getElementById('filtroAsistenciaHoraHasta')?.value || '').trim();
        const tipo = (document.getElementById('filtroAsistenciaTipo')?.value || '').trim().toUpperCase();

        const hoy = new Date();
        const hace7Dias = new Date();
        hace7Dias.setDate(hace7Dias.getDate() - 7);
        const strHace7Dias = getLocalDateString(hace7Dias);
        const strHoy = getLocalDateString(hoy);
        const strMesActual = strHoy.substring(0, 7);

        const filtradas = cacheAsistencias.filter(a => {
            // 1. Filtro por texto libre (nombre, carnet, dispositivo, id)
            if (texto) {
                const nombre = (a.nombreIntegrante || a.integranteNombre || '').toLowerCase();
                const carnet = (a.carnetUniversitario || '').toLowerCase();
                const disp = (a.dispositivo || a.dispositivoUtilizado || '').toLowerCase();
                const id = String(a.asistenciaId || a.id || '');
                if (!nombre.includes(texto) && !carnet.includes(texto) && !disp.includes(texto) && !id.includes(texto)) {
                    return false;
                }
            }

            // 2. Filtro por tipo de marcacion (ENTRADA o SALIDA)
            if (tipo) {
                const tipoMarc = (a.tipoMarcacion || a.tipo || '').toUpperCase();
                if (tipoMarc !== tipo) return false;
            }

            // 3. Filtro por fecha y hora
            if (a.fechaHora) {
                let aDateStr = '';
                let aTimeStr = '';

                if (typeof a.fechaHora === 'string' && a.fechaHora.includes('T')) {
                    aDateStr = a.fechaHora.substring(0, 10);
                    if (a.fechaHora.length >= 16) {
                        aTimeStr = a.fechaHora.substring(11, 16);
                    }
                } else {
                    const d = new Date(a.fechaHora);
                    aDateStr = getLocalDateString(d);
                    const hours = String(d.getHours()).padStart(2, '0');
                    const minutes = String(d.getMinutes()).padStart(2, '0');
                    aTimeStr = `${hours}:${minutes}`;
                }

                // Fecha exacta si esta seleccionada
                if (fecha) {
                    if (aDateStr !== fecha) return false;
                } else if (modoRangoAsistencia === '7dias') {
                    if (aDateStr < strHace7Dias || aDateStr > strHoy) return false;
                } else if (modoRangoAsistencia === 'mes') {
                    if (!aDateStr.startsWith(strMesActual)) return false;
                }

                // Rango de horas
                if (horaDesde && aTimeStr && aTimeStr < horaDesde) return false;
                if (horaHasta && aTimeStr && aTimeStr > horaHasta) return false;
            } else if (fecha || horaDesde || horaHasta || modoRangoAsistencia) {
                return false;
            }

            return true;
        });

        renderTablaAsistencias(filtradas);
    }

    function aplicarFiltrosMateriales() {
        if (!cacheMateriales) return;
        const term = (document.getElementById('inputBuscarMaterial')?.value || '').toLowerCase().trim();
        const categoria = (document.getElementById('filtroMaterialCategoria')?.value || '').toLowerCase().trim();
        const stock = (document.getElementById('filtroMaterialStock')?.value || '').trim();

        const filtrados = cacheMateriales.filter(m => {
            if (term) {
                const nombre = (m.nombre || '').toLowerCase();
                const cat = (m.categoriaNombre || '').toLowerCase();
                const marca = (m.marca || '').toLowerCase();
                const modelo = (m.modelo || '').toLowerCase();
                const ubicacion = (m.ubicacion || '').toLowerCase();
                const desc = (m.descripcion || '').toLowerCase();
                if (!nombre.includes(term) && !cat.includes(term) && !marca.includes(term) && !modelo.includes(term) && !ubicacion.includes(term) && !desc.includes(term)) {
                    return false;
                }
            }
            if (categoria) {
                const cat = (m.categoriaNombre || '').toLowerCase();
                if (cat !== categoria) return false;
            }
            if (stock === 'disponible') {
                const cant = m.cantidadDisponible !== undefined ? m.cantidadDisponible : (m.cantidadTotal || 0);
                if (cant <= 0) return false;
            }
            if (stock === 'agotado') {
                const cant = m.cantidadDisponible !== undefined ? m.cantidadDisponible : (m.cantidadTotal || 0);
                if (cant > 0) return false;
            }
            return true;
        });

        renderTablaInventario(filtrados);
    }

    function aplicarFiltrosProyectos() {
        if (!cacheProyectos) return;
        const term = (document.getElementById('inputBuscarProyecto')?.value || '').toLowerCase().trim();
        const estado = (document.getElementById('filtroProyectoEstado')?.value || '').trim().toUpperCase();

        const filtrados = cacheProyectos.filter(p => {
            if (term) {
                const nombre = (p.nombre || '').toLowerCase();
                const desc = (p.descripcion || '').toLowerCase();
                const id = String(p.id || '');
                if (!nombre.includes(term) && !desc.includes(term) && !id.includes(term)) {
                    return false;
                }
            }
            if (estado && (p.estado || '').toUpperCase() !== estado) {
                return false;
            }
            return true;
        });

        renderTablaProyectos(filtrados);
    }

    function aplicarFiltrosIntegrantes() {
        if (!cacheIntegrantes) return;
        const term = (document.getElementById('inputBuscarIntegrante')?.value || '').toLowerCase().trim();
        const rol = (document.getElementById('filtroIntegranteRol')?.value || '').trim().toUpperCase();
        const estado = (document.getElementById('filtroIntegranteEstado')?.value || '').trim().toUpperCase();

        const filtrados = cacheIntegrantes.filter(i => {
            if (term) {
                const nombre = (i.nombreCompleto || (i.nombre + ' ' + i.apellido) || '').toLowerCase();
                const ci = (i.ci || '').toLowerCase();
                const carnet = (i.carnetUniversitario || '').toLowerCase();
                const carrera = (i.carrera || '').toLowerCase();
                const email = (i.email || '').toLowerCase();
                const rfid = (i.nfcUid || '').toLowerCase();
                if (!nombre.includes(term) && !ci.includes(term) && !carnet.includes(term) && !carrera.includes(term) && !email.includes(term) && !rfid.includes(term)) {
                    return false;
                }
            }
            if (rol && (i.rol || '').toUpperCase() !== rol) return false;
            if (estado && (i.estado || '').toUpperCase() !== estado) return false;
            return true;
        });

        renderTablaIntegrantes(filtrados);
    }

    function aplicarFiltrosPrestamos() {
        if (!cachePrestamos) return;
        const term = (document.getElementById('inputBuscarPrestamo')?.value || '').toLowerCase().trim();
        const estado = (document.getElementById('filtroPrestamoEstado')?.value || '').trim().toUpperCase();

        const filtrados = cachePrestamos.filter(p => {
            if (term) {
                const integrante = (p.nombreIntegrante || '').toLowerCase();
                const proyecto = (p.nombreProyecto || '').toLowerCase();
                const desc = (p.descripcion || '').toLowerCase();
                const id = String(p.prestamoId || '');
                let mats = '';
                if (p.detalles && Array.isArray(p.detalles)) {
                    mats = p.detalles.map(d => d.nombreMaterial || '').join(' ').toLowerCase();
                }
                if (!integrante.includes(term) && !proyecto.includes(term) && !desc.includes(term) && !id.includes(term) && !mats.includes(term)) {
                    return false;
                }
            }
            if (estado && (p.estado || '').toUpperCase() !== estado) return false;
            return true;
        });

        renderTablaPrestamos(filtrados);
    }

    function aplicarFiltrosAprobaciones() {
        if (!cachePrestamos) return;
        const term = (document.getElementById('inputBuscarAprobacion')?.value || '').toLowerCase().trim();
        const solicitados = cachePrestamos.filter(p => p.estado === 'SOLICITADO');

        const filtrados = solicitados.filter(p => {
            if (term) {
                const integrante = (p.nombreIntegrante || '').toLowerCase();
                const proyecto = (p.nombreProyecto || '').toLowerCase();
                const desc = (p.descripcion || '').toLowerCase();
                const id = String(p.prestamoId || '');
                let mats = '';
                if (p.detalles && Array.isArray(p.detalles)) {
                    mats = p.detalles.map(d => d.nombreMaterial || '').join(' ').toLowerCase();
                }
                if (!integrante.includes(term) && !proyecto.includes(term) && !desc.includes(term) && !id.includes(term) && !mats.includes(term)) {
                    return false;
                }
            }
            return true;
        });

        renderTablaAprobaciones(filtrados);
    }

    function initSearchFilters() {
        // --- 1. FILTROS DE ASISTENCIA ---
        const inputAsistTexto = document.getElementById('filtroAsistenciaTexto');
        const inputAsistFecha = document.getElementById('filtroAsistenciaFecha');
        const inputAsistHoraDesde = document.getElementById('filtroAsistenciaHoraDesde');
        const inputAsistHoraHasta = document.getElementById('filtroAsistenciaHoraHasta');
        const selectAsistTipo = document.getElementById('filtroAsistenciaTipo');
        const btnLimpiarAsist = document.getElementById('btnLimpiarFiltrosAsistencia');

        if (inputAsistTexto) inputAsistTexto.addEventListener('input', () => aplicarFiltrosAsistencia());
        if (inputAsistFecha) {
            inputAsistFecha.addEventListener('input', () => {
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            });
            inputAsistFecha.addEventListener('change', () => {
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            });
        }
        if (inputAsistHoraDesde) {
            inputAsistHoraDesde.addEventListener('input', () => aplicarFiltrosAsistencia());
            inputAsistHoraDesde.addEventListener('change', () => aplicarFiltrosAsistencia());
        }
        if (inputAsistHoraHasta) {
            inputAsistHoraHasta.addEventListener('input', () => aplicarFiltrosAsistencia());
            inputAsistHoraHasta.addEventListener('change', () => aplicarFiltrosAsistencia());
        }
        if (selectAsistTipo) selectAsistTipo.addEventListener('change', () => aplicarFiltrosAsistencia());

        if (btnLimpiarAsist) {
            btnLimpiarAsist.onclick = () => {
                if (inputAsistTexto) inputAsistTexto.value = '';
                if (inputAsistFecha) inputAsistFecha.value = '';
                if (inputAsistHoraDesde) inputAsistHoraDesde.value = '';
                if (inputAsistHoraHasta) inputAsistHoraHasta.value = '';
                if (selectAsistTipo) selectAsistTipo.value = '';
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            };
        }

        // Atajos rapidos de fecha para Asistencia
        const btnAtajoHoy = document.getElementById('btnAtajoHoy');
        const btnAtajoAyer = document.getElementById('btnAtajoAyer');
        const btnAtajo7Dias = document.getElementById('btnAtajo7Dias');
        const btnAtajoMes = document.getElementById('btnAtajoMes');
        const btnAtajoTodos = document.getElementById('btnAtajoTodos');

        if (btnAtajoHoy) {
            btnAtajoHoy.onclick = () => {
                if (inputAsistFecha) inputAsistFecha.value = getLocalDateString(new Date());
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            };
        }
        if (btnAtajoAyer) {
            btnAtajoAyer.onclick = () => {
                const d = new Date();
                d.setDate(d.getDate() - 1);
                if (inputAsistFecha) inputAsistFecha.value = getLocalDateString(d);
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            };
        }
        if (btnAtajo7Dias) {
            btnAtajo7Dias.onclick = () => {
                if (inputAsistFecha) inputAsistFecha.value = '';
                modoRangoAsistencia = '7dias';
                aplicarFiltrosAsistencia();
            };
        }
        if (btnAtajoMes) {
            btnAtajoMes.onclick = () => {
                if (inputAsistFecha) inputAsistFecha.value = '';
                modoRangoAsistencia = 'mes';
                aplicarFiltrosAsistencia();
            };
        }
        if (btnAtajoTodos) {
            btnAtajoTodos.onclick = () => {
                if (inputAsistFecha) inputAsistFecha.value = '';
                modoRangoAsistencia = null;
                aplicarFiltrosAsistencia();
            };
        }

        // --- 2. FILTROS DE INVENTARIO / MATERIALES ---
        const inputMat = document.getElementById('inputBuscarMaterial');
        const filtroCat = document.getElementById('filtroMaterialCategoria');
        const filtroStock = document.getElementById('filtroMaterialStock');
        const btnLimpiarMat = document.getElementById('btnLimpiarFiltrosMaterial');

        if (inputMat) inputMat.addEventListener('input', () => aplicarFiltrosMateriales());
        if (filtroCat) filtroCat.addEventListener('change', () => aplicarFiltrosMateriales());
        if (filtroStock) filtroStock.addEventListener('change', () => aplicarFiltrosMateriales());
        if (btnLimpiarMat) {
            btnLimpiarMat.onclick = () => {
                if (inputMat) inputMat.value = '';
                if (filtroCat) filtroCat.value = '';
                if (filtroStock) filtroStock.value = '';
                aplicarFiltrosMateriales();
            };
        }

        // --- 3. FILTROS DE PROYECTOS ---
        const inputProy = document.getElementById('inputBuscarProyecto');
        const filtroProyEstado = document.getElementById('filtroProyectoEstado');
        const btnLimpiarProy = document.getElementById('btnLimpiarFiltrosProyecto');

        if (inputProy) inputProy.addEventListener('input', () => aplicarFiltrosProyectos());
        if (filtroProyEstado) filtroProyEstado.addEventListener('change', () => aplicarFiltrosProyectos());
        if (btnLimpiarProy) {
            btnLimpiarProy.onclick = () => {
                if (inputProy) inputProy.value = '';
                if (filtroProyEstado) filtroProyEstado.value = '';
                aplicarFiltrosProyectos();
            };
        }

        // --- 4. FILTROS DE INTEGRANTES ---
        const inputInt = document.getElementById('inputBuscarIntegrante');
        const filtroIntRol = document.getElementById('filtroIntegranteRol');
        const filtroIntEstado = document.getElementById('filtroIntegranteEstado');
        const btnLimpiarInt = document.getElementById('btnLimpiarFiltrosIntegrante');

        if (inputInt) inputInt.addEventListener('input', () => aplicarFiltrosIntegrantes());
        if (filtroIntRol) filtroIntRol.addEventListener('change', () => aplicarFiltrosIntegrantes());
        if (filtroIntEstado) filtroIntEstado.addEventListener('change', () => aplicarFiltrosIntegrantes());
        if (btnLimpiarInt) {
            btnLimpiarInt.onclick = () => {
                if (inputInt) inputInt.value = '';
                if (filtroIntRol) filtroIntRol.value = '';
                if (filtroIntEstado) filtroIntEstado.value = '';
                aplicarFiltrosIntegrantes();
            };
        }

        // --- 5. FILTROS DE PRESTAMOS ---
        const inputPrest = document.getElementById('inputBuscarPrestamo');
        const filtroPrestEstado = document.getElementById('filtroPrestamoEstado');
        const btnLimpiarPrest = document.getElementById('btnLimpiarFiltrosPrestamo');

        if (inputPrest) inputPrest.addEventListener('input', () => aplicarFiltrosPrestamos());
        if (filtroPrestEstado) filtroPrestEstado.addEventListener('change', () => aplicarFiltrosPrestamos());
        if (btnLimpiarPrest) {
            btnLimpiarPrest.onclick = () => {
                if (inputPrest) inputPrest.value = '';
                if (filtroPrestEstado) filtroPrestEstado.value = '';
                aplicarFiltrosPrestamos();
            };
        }

        // --- 6. FILTRO DE APROBAR PRESTAMOS ---
        const inputAprob = document.getElementById('inputBuscarAprobacion');
        if (inputAprob) inputAprob.addEventListener('input', () => aplicarFiltrosAprobaciones());

        // --- 6.1 FILTRO DE CUADRO DE HONOR / RANKING ---
        const inputBuscarRanking = document.getElementById('inputBuscarRanking');
        if (inputBuscarRanking) {
            inputBuscarRanking.addEventListener('input', () => {
                if (cacheHorasDashboard && cacheHorasDashboard.ranking) {
                    renderRankingCuadroHonor(cacheHorasDashboard.ranking);
                }
            });
        }

        // --- 7. BOTONES DE RECARGA ---
        const btnRecargarPrestamos = document.getElementById('btnRecargarPrestamos');
        const btnRecargarAprobaciones = document.getElementById('btnRecargarAprobaciones');
        const btnRecargarInventario = document.getElementById('btnRecargarInventario');
        const btnRecargarAsistencias = document.getElementById('btnRecargarAsistencias');
        const btnRecargarIntegrantes = document.getElementById('btnRecargarIntegrantes');
        const btnRecargarProyectos = document.getElementById('btnRecargarProyectos');

        if (btnRecargarPrestamos) btnRecargarPrestamos.onclick = async () => { await cargarPrestamos(); aplicarFiltrosPrestamos(); };
        if (btnRecargarAprobaciones) btnRecargarAprobaciones.onclick = async () => { await cargarPrestamos(); aplicarFiltrosAprobaciones(); };
        if (btnRecargarInventario) btnRecargarInventario.onclick = async () => { await cargarInventario(); aplicarFiltrosMateriales(); };
        if (btnRecargarAsistencias) btnRecargarAsistencias.onclick = async () => { await cargarAsistencias(); aplicarFiltrosAsistencia(); };
        if (btnRecargarIntegrantes) btnRecargarIntegrantes.onclick = async () => { await cargarIntegrantes(); aplicarFiltrosIntegrantes(); };
        if (btnRecargarProyectos) btnRecargarProyectos.onclick = async () => { await cargarProyectos(); aplicarFiltrosProyectos(); };

        // --- 8. BOTONES DE EXPORTACION A CSV / EXCEL ---
        const btnExpRanking = document.getElementById('btnExportarRankingCSV');
        if (btnExpRanking) {
            btnExpRanking.onclick = () => {
                const ranking = (cacheHorasDashboard && cacheHorasDashboard.ranking) || [];
                const filas = ranking.map(m => [
                    m.posicion,
                    m.nombreCompleto,
                    m.carnet || '',
                    m.carrera || '',
                    m.horasTotales,
                    m.totalSesiones,
                    m.tierNombre || m.tier,
                    m.enElLabHoy ? 'Presente' : 'Fuera'
                ]);
                exportarAExcelCSV('Ranking_Horas_Club_Robotica_FPUNE', ['Puesto', 'Integrante', 'Carnet', 'Carrera', 'Horas Totales', 'Sesiones', 'Nivel Tier', 'Estado Lab'], filas);
            };
        }

        const btnExpInventario = document.getElementById('btnExportarInventarioCSV');
        if (btnExpInventario) {
            btnExpInventario.onclick = () => {
                const filas = cacheMateriales.map(m => [
                    m.id,
                    m.codigoInventario || '',
                    m.nombre,
                    m.categoriaNombre || (m.categoria && m.categoria.nombre) || 'General',
                    m.marca || '',
                    m.modelo || '',
                    m.cantidadTotal || 0,
                    m.cantidadDisponible !== undefined ? m.cantidadDisponible : (m.cantidadTotal || 0),
                    m.estado || 'DISPONIBLE'
                ]);
                exportarAExcelCSV('Inventario_Materiales_Club_Robotica_FPUNE', ['ID', 'Codigo', 'Material', 'Categoria', 'Marca', 'Modelo', 'Stock Total', 'Stock Disponible', 'Estado'], filas);
            };
        }

        const btnExpPrestamos = document.getElementById('btnExportarPrestamosCSV');
        if (btnExpPrestamos) {
            btnExpPrestamos.onclick = () => {
                const filas = cachePrestamos.map(p => [
                    p.id,
                    p.nombreIntegrante || p.integranteNombre || '',
                    (p.detalles || []).map(d => `${d.materialNombre || 'Mat'} (x${d.cantidad || 1})`).join('; '),
                    p.fechaPrestamo ? new Date(p.fechaPrestamo).toLocaleDateString() : '',
                    p.fechaDevolucionEstimada ? new Date(p.fechaDevolucionEstimada).toLocaleDateString() : '',
                    p.fechaDevolucionReal ? new Date(p.fechaDevolucionReal).toLocaleDateString() : '',
                    p.estado
                ]);
                exportarAExcelCSV('Prestamos_Club_Robotica_FPUNE', ['ID Prestamo', 'Solicitante', 'Materiales Prestados', 'Fecha Prestamo', 'Fecha Limite Devolucion', 'Fecha Real Devolucion', 'Estado'], filas);
            };
        }

        const btnExpAsistencias = document.getElementById('btnExportarAsistenciasCSV');
        if (btnExpAsistencias) {
            btnExpAsistencias.onclick = () => {
                const filas = cacheAsistencias.map(a => [
                    a.id || a.asistenciaId,
                    a.nombreIntegrante || a.integranteNombre || '',
                    a.carnetUniversitario || '',
                    a.tipoMarcacion || a.tipo || 'ENTRADA',
                    a.fechaHora ? new Date(a.fechaHora).toLocaleString('es-PY') : '',
                    a.dispositivoUtilizado || 'ESP32'
                ]);
                exportarAExcelCSV('Planilla_Asistencias_Extension_FPUNE', ['ID Asistencia', 'Integrante', 'Carnet', 'Tipo Marcacion', 'Fecha y Hora', 'Dispositivo'], filas);
            };
        }
    }

    function exportarAExcelCSV(nombreArchivo, cabeceras, filas) {
        if (!filas || filas.length === 0) {
            showNotification('No hay registros disponibles para exportar.', 'warning');
            return;
        }

        const escapeCSV = (val) => {
            if (val === null || val === undefined) return '""';
            let str = String(val).replace(/"/g, '""');
            return `"${str}"`;
        };

        const lineas = [];
        lineas.push(cabeceras.map(escapeCSV).join(';'));

        filas.forEach(fila => {
            lineas.push(fila.map(escapeCSV).join(';'));
        });

        const csvContent = '\uFEFF' + lineas.join('\r\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.setAttribute('href', url);
        link.setAttribute('download', `${nombreArchivo}_${new Date().toISOString().slice(0, 10)}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        showNotification(`Reporte <strong>${nombreArchivo}.csv</strong> generado y descargado correctamente.`, 'success');
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
                <div>${message}</div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        container.appendChild(alertDiv);
        setTimeout(() => {
            alertDiv.classList.remove('show');
            setTimeout(() => alertDiv.remove(), 250);
        }, 5000);
    }

    // ========================================================
    // ASISTENTE DE INTELIGENCIA ARTIFICIAL (ROBOBOT - GEMINI)
    // ========================================================
    function initChatbotIa() {
        const formChat = document.getElementById('formChatIa');
        const inputMensaje = document.getElementById('inputMensajeIa');
        const btnEnviar = document.getElementById('btnEnviarMensajeIa');
        const contenedorMensajes = document.getElementById('chatIaMensajes');
        const typingIndicator = document.getElementById('chatIaTyping');
        const btnToggleConfig = document.getElementById('btnToggleConfigIa');
        const panelConfig = document.getElementById('panelConfigApiKey');
        const inputApiKey = document.getElementById('inputApiKeyIa');
        const btnGuardarApiKey = document.getElementById('btnGuardarApiKeyIa');
        const btnBorrarApiKey = document.getElementById('btnBorrarApiKeyIa');
        const badgeEstadoApiKey = document.getElementById('badgeEstadoApiKey');
        const infoEstadoServidor = document.getElementById('infoEstadoServidorKey');
        const btnLimpiarChat = document.getElementById('btnLimpiarChatIa');
        const promptChips = document.querySelectorAll('.chip-prompt');

        if (!formChat || !inputMensaje) return;

        let historialChatIa = [];
        const LOCAL_STORAGE_KEY = 'robo_gest_gemini_api_key';

        // Cargar clave guardada en localStorage si existe
        const claveGuardada = localStorage.getItem(LOCAL_STORAGE_KEY) || '';
        if (inputApiKey && claveGuardada) {
            inputApiKey.value = claveGuardada;
        }

        // Consultar estado de la clave en el servidor
        async function consultarEstadoIa() {
            try {
                const res = await window.Auth.authFetch('/asistente/estado');
                if (res.ok) {
                    const data = await res.json();
                    const hasServerKey = data.claveServidorConfigurada;
                    const hasLocalKey = !!localStorage.getItem(LOCAL_STORAGE_KEY);

                    if (badgeEstadoApiKey) {
                        if (hasServerKey) {
                            badgeEstadoApiKey.className = 'badge bg-success bg-opacity-10 text-success border border-success border-opacity-25';
                            badgeEstadoApiKey.textContent = 'Clave Servidor Activa';
                        } else if (hasLocalKey) {
                            badgeEstadoApiKey.className = 'badge bg-info bg-opacity-10 text-info border border-info border-opacity-25';
                            badgeEstadoApiKey.textContent = 'Clave Local Activa';
                        } else {
                            badgeEstadoApiKey.className = 'badge bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25';
                            badgeEstadoApiKey.textContent = 'Requiere Clave (Gratis)';
                        }
                    }

                    if (infoEstadoServidor) {
                        if (hasServerKey) {
                            infoEstadoServidor.textContent = 'El servidor cuenta con una clave global configurada (GEMINI_API_KEY).';
                        } else {
                            infoEstadoServidor.textContent = 'El servidor no tiene GEMINI_API_KEY configurada. Puedes ingresar una clave propia arriba.';
                        }
                    }
                }
            } catch (e) {
                console.warn('[Chatbot IA] Error consultando estado del servicio:', e);
            }
        }

        consultarEstadoIa();

        // Configuración de clave en cliente: Solo disponible para Administradores
        const isAdmin = userRole === 'ADMIN' || (window.Auth && window.Auth.hasAnyRole('ADMIN'));
        if (isAdmin && btnToggleConfig) {
            btnToggleConfig.classList.remove('d-none');
            btnToggleConfig.style.removeProperty('display');
            btnToggleConfig.addEventListener('click', () => {
                if (panelConfig) panelConfig.classList.toggle('show');
            });
        } else {
            if (btnToggleConfig) btnToggleConfig.style.setProperty('display', 'none', 'important');
            if (panelConfig) panelConfig.classList.remove('show');
        }

        if (btnGuardarApiKey && inputApiKey) {
            btnGuardarApiKey.addEventListener('click', () => {
                const keyVal = inputApiKey.value.trim();
                if (!keyVal) {
                    showNotification('Por favor ingresa una clave de API válida.', 'warning');
                    return;
                }
                localStorage.setItem(LOCAL_STORAGE_KEY, keyVal);
                showNotification('Clave de Gemini API guardada localmente.', 'success');
                consultarEstadoIa();
            });
        }

        if (btnBorrarApiKey && inputApiKey) {
            btnBorrarApiKey.addEventListener('click', () => {
                localStorage.removeItem(LOCAL_STORAGE_KEY);
                inputApiKey.value = '';
                showNotification('Clave local eliminada.', 'info');
                consultarEstadoIa();
            });
        }

        const LOCAL_STORAGE_MODEL_KEY = 'robogest_gemini_model';
        const selectModeloIa = document.getElementById('selectModeloIa');
        if (selectModeloIa) {
            const savedModel = localStorage.getItem(LOCAL_STORAGE_MODEL_KEY);
            if (savedModel) {
                selectModeloIa.value = savedModel;
            }
            selectModeloIa.addEventListener('change', () => {
                localStorage.setItem(LOCAL_STORAGE_MODEL_KEY, selectModeloIa.value);
                showNotification(`Modelo seleccionado: ${selectModeloIa.value}`, 'info');
            });
        }

        // Detector de emocion segun el contenido de la respuesta o estado del asistente
        function determinarEmocion(texto, exito) {
            if (!exito) return 'frustrado';
            if (!texto) return 'respuesta';
            const t = texto.toLowerCase();

            // Frustrado: agotado, sin stock, sanciones, advertencias, fallos
            if (t.includes('no disponible') || t.includes('agotado') || t.includes('sin stock') ||
                t.includes('no contamos') || t.includes('sanción') || t.includes('infracción') ||
                t.includes('multa') || t.includes('penalización') || t.includes('dañado') ||
                t.includes('advertencia') || t.includes('error')) {
                return 'frustrado';
            }

            // Emocionado: proyectos innovadores, felicitaciones, entusiasmo, concursos
            if (t.includes('¡excelente') || t.includes('¡genial') || t.includes('¡increíble') ||
                t.includes('¡manos a la obra') || t.includes('¡fantástico') || t.includes('¡buena idea') ||
                t.includes('¡bienvenido') || t.includes('¡felicidades') || t.includes('¡vamos') ||
                t.includes('competencia') || t.includes('ganador') || t.includes('innovador') ||
                t.includes('🚀') || t.includes('🎉') || t.includes('🔥') || t.includes('🏆') || t.includes('💡')) {
                return 'emocionado';
            }

            return 'respuesta';
        }

        // Control dinámico de las expresiones y animaciones de Polito Companion al costado del chat
        function setPolitoSidekickState(estado, textoSpeech) {
            const sidekickImg = document.getElementById('politoSidekickImg');
            const sidekickSpeech = document.getElementById('politoSidekickSpeech');
            if (!sidekickImg) return;

            // Limpiar clases de animación previas
            sidekickImg.classList.remove('polito-pulse', 'polito-bounce', 'polito-shake');

            if (estado === 'pensando') {
                sidekickImg.src = 'assets/images/polito/polito_pensando.png';
                if (sidekickSpeech) sidekickSpeech.textContent = textoSpeech || 'Consultando inventario y pensando... 💡';
                sidekickImg.classList.add('polito-pulse');
            } else if (estado === 'emocionado' || estado === 'emocion') {
                sidekickImg.src = 'assets/images/polito/polito_emocionado.png?v=20261018_9';
                if (sidekickSpeech) sidekickSpeech.textContent = textoSpeech || '¡Qué gran proyecto! ¡Vamos con todo! 🚀🎉';
                sidekickImg.classList.add('polito-bounce');
                setTimeout(() => {
                    const currentImg = document.getElementById('politoSidekickImg');
                    if (currentImg && (currentImg.src.includes('polito_emocionado') || currentImg.src.includes('polito_emocion'))) {
                        setPolitoSidekickState('saludando', '¿Tienes otra consulta o proyecto?');
                    }
                }, 6000);
            } else if (estado === 'frustrado') {
                sidekickImg.src = 'assets/images/polito/polito_frustrado.png';
                if (sidekickSpeech) sidekickSpeech.textContent = textoSpeech || '¡Ups! Revisa el aviso o componente ⚠️';
                sidekickImg.classList.add('polito-shake');
                setTimeout(() => {
                    const currentImg = document.getElementById('politoSidekickImg');
                    if (currentImg && currentImg.src.includes('polito_frustrado')) {
                        setPolitoSidekickState('saludando', '¿Probamos con otra alternativa?');
                    }
                }, 6000);
            } else if (estado === 'respuesta') {
                sidekickImg.src = 'assets/images/polito/polito_respuesta.png';
                if (sidekickSpeech) sidekickSpeech.textContent = textoSpeech || '¡Aquí tienes la información! 👇';
                setTimeout(() => {
                    const currentImg = document.getElementById('politoSidekickImg');
                    if (currentImg && currentImg.src.includes('polito_respuesta')) {
                        setPolitoSidekickState('saludando', '¿Tienes otra duda o consulta?');
                    }
                }, 5000);
            } else {
                // 'saludando' (estado de reposo)
                sidekickImg.src = 'assets/images/polito/polito_saludando.png';
                if (sidekickSpeech) sidekickSpeech.textContent = textoSpeech || '¡Hola! ¿En qué te ayudo hoy?';
            }
        }

        // Eventos del offcanvas y de la mascota compañera
        const offcanvasEl = document.getElementById('offcanvasChatIa');
        if (offcanvasEl) {
            offcanvasEl.addEventListener('show.bs.offcanvas', () => {
                setPolitoSidekickState('saludando', '¡Hola! ¿En qué te ayudo hoy?');
            });
        }

        const sidekickAvatar = document.getElementById('politoSidekickImg');
        const expresionesInteractivas = [
            { estado: 'emocionado', frase: '¡Me encanta la robótica en la FP-UNE! 🚀🎉' },
            { estado: 'saludando', frase: '¡Hola! Conozco todo el inventario del laboratorio 🤖' },
            { estado: 'pensando', frase: '¿Pensando en armar un seguidor de línea o minisumo? 💡' },
            { estado: 'frustrado', frase: '¡Recuerda cuidar los materiales y devolverlos a tiempo! 📋⚠️' },
            { estado: 'respuesta', frase: '¡Cualquier duda técnica con sensores o ESP32, pregúntame! 📡' },
            { estado: 'emocionado', frase: '¡Qué buen proyecto! La FP-UNE va a brillar en la competencia 🏆🔥' },
            { estado: 'pensando', frase: 'Mmm... ¿usamos puente H L298N o driver TB6612 para más eficiencia? 🤔⚙️' },
            { estado: 'saludando', frase: '¡Pasa adelante! No olvides registrar tu asistencia con la tarjeta RFID 🏷️✅' },
            { estado: 'frustrado', frase: '¡Cuidado con la polaridad invertida! No queremos quemar ningún integrado 🔌💥' },
            { estado: 'emocionado', frase: '¡Compiló sin errores a la primera! Eso sí que es magia politécnica ⚡😎' },
            { estado: 'respuesta', frase: 'Tenemos placas ESP32 y sensores ultrasónicos listos para retirar en stock 📦✨' },
            { estado: 'pensando', frase: 'Calculando cinemática inversa para el brazo robótico... 🤖📐' },
            { estado: 'saludando', frase: '¡Buen día! ¿En qué circuito o código te doy una mano hoy? 💻👋' },
            { estado: 'emocionado', frase: '¡Vamos por ese podio en la competencia de robótica! 🥇🚀' },
            { estado: 'frustrado', frase: '¡Ojo con dejar el cautín encendido en la mesa de trabajo! 🔥⚠️' },
            { estado: 'respuesta', frase: 'Puedes pedir préstamos de componentes por hasta 14 días renovables 📅📑' },
            { estado: 'pensando', frase: '¿Y si le agregamos telemetría Bluetooth con una app móvil? 📱💡' },
            { estado: 'emocionado', frase: '¡La creatividad de los estudiantes de la politécnica no tiene límites! 🌟👏' },
            { estado: 'saludando', frase: '¡Haz clic sobre mí cuando quieras inspiración o ideas! 😃👋' },
            { estado: 'respuesta', frase: 'Pregúntame en el chat por cualquier componente y te diré si hay stock 💬🔍' }
        ];
        let idxExpresion = 0;
        if (sidekickAvatar) {
            sidekickAvatar.addEventListener('click', () => {
                const exp = expresionesInteractivas[idxExpresion % expresionesInteractivas.length];
                idxExpresion++;
                setPolitoSidekickState(exp.estado, exp.frase);
            });
        }

        // Limpiar conversación
        if (btnLimpiarChat && contenedorMensajes) {
            btnLimpiarChat.addEventListener('click', () => {
                if (confirm('¿Deseas reiniciar la conversación con Polito?')) {
                    historialChatIa = [];
                    contenedorMensajes.innerHTML = `
                        <div class="chat-bubble chat-bubble-bot shadow-sm">
                            <div class="d-flex align-items-center gap-2 mb-2 pb-1 border-bottom">
                                <img src="assets/images/polito/polito_pfp.png" alt="Polito" class="rounded-circle border" style="width: 28px; height: 28px; object-fit: cover; background: #EEF2F6;">
                                <div>
                                    <strong class="small d-block text-dark fw-bold">Polito</strong>
                                    <small class="text-muted" style="font-size: 0.68rem;">Asistente FP-UNE</small>
                                </div>
                                <span class="badge bg-success-subtle text-success border border-success-subtle ms-auto" style="font-size: 0.65rem;">
                                    <i class="bi bi-circle-fill me-1" style="font-size: 0.45rem;"></i>En línea
                                </span>
                            </div>
                            ¡Conversación reiniciada! ¿Qué proyecto o duda sobre el inventario y normativas del club deseas consultar?
                        </div>
                    `;
                    setPolitoSidekickState('saludando', '¡Listo para empezar de nuevo!');
                }
            });
        }

        // Enviar mensaje al asistente
        async function enviarConsulta(texto) {
            const consulta = (texto || '').trim();
            if (!consulta) return;

            // 1. Añadir burbuja de usuario
            const userBubble = document.createElement('div');
            userBubble.className = 'chat-bubble chat-bubble-user shadow-sm';
            userBubble.textContent = consulta;
            contenedorMensajes.appendChild(userBubble);

            const turnosPrevios = historialChatIa.slice(-8);
            historialChatIa.push({ rol: 'user', contenido: consulta });

            // Limpiar input y activar animación pensando
            inputMensaje.value = '';
            if (btnEnviar) btnEnviar.disabled = true;
            if (typingIndicator) typingIndicator.style.setProperty('display', 'flex', 'important');
            setPolitoSidekickState('pensando', 'Consultando inventario y pensando...');
            contenedorMensajes.scrollTop = contenedorMensajes.scrollHeight;

            try {
                const savedKey = localStorage.getItem(LOCAL_STORAGE_KEY);
                const savedModel = localStorage.getItem(LOCAL_STORAGE_MODEL_KEY);
                const payload = {
                    mensaje: consulta,
                    historial: turnosPrevios,
                    apiKey: (savedKey && savedKey.trim().length > 0) ? savedKey.trim() : null,
                    modelo: (savedModel && savedModel.trim().length > 0) ? savedModel.trim() : null
                };

                const res = await window.Auth.authFetch('/asistente/chat', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });

                const data = await res.json().catch(() => null);

                if (!res.ok || !data || !data.exito) {
                    const errMsg = (data && (data.error || data.respuesta)) || `Error al consultar asistente (HTTP ${res.status}).`;
                    setPolitoSidekickState('frustrado', '¡Ups! Ocurrió un inconveniente 🔌');
                    const botErrBubble = document.createElement('div');
                    botErrBubble.className = 'chat-bubble chat-bubble-bot border-danger border-opacity-50 text-danger shadow-sm';
                    botErrBubble.innerHTML = `
                        <div class="d-flex align-items-center gap-2 mb-1">
                            <i class="bi bi-exclamation-triangle-fill text-danger"></i>
                            <strong class="small text-danger">Aviso de Polito</strong>
                        </div>
                        <div>${escapeHtml(errMsg)}</div>
                        ${isAdmin && (errMsg.includes('API') || errMsg.includes('clave')) ? '<div class="mt-2"><button type="button" class="btn btn-xs btn-outline-danger" style="font-size: 0.75rem;" onclick="document.getElementById(\'panelConfigApiKey\').classList.add(\'show\');"><i class="bi bi-key-fill me-1"></i>Configurar API Key</button></div>' : ''}
                    `;
                    contenedorMensajes.appendChild(botErrBubble);
                } else {
                    const respuestaTexto = data.respuesta || '';
                    historialChatIa.push({ rol: 'model', contenido: respuestaTexto });

                    const emocion = determinarEmocion(respuestaTexto, true);
                    if (emocion === 'emocionado') {
                        setPolitoSidekickState('emocionado', '¡Excelente proyecto! 🚀🎉');
                    } else if (emocion === 'frustrado') {
                        setPolitoSidekickState('frustrado', 'Componente agotado o aviso importante ⚠️');
                    } else {
                        setPolitoSidekickState('respuesta', '¡Aquí tengo la respuesta! 👇');
                    }

                    const botBubble = document.createElement('div');
                    botBubble.className = 'chat-bubble chat-bubble-bot shadow-sm';
                    botBubble.innerHTML = `
                        <div class="d-flex align-items-center gap-2 mb-2 pb-1 border-bottom">
                            <img src="assets/images/polito/polito_pfp.png" alt="Polito" class="rounded-circle border" style="width: 28px; height: 28px; object-fit: cover; background: #EEF2F6;">
                            <div>
                                <strong class="small d-block text-dark fw-bold">Polito</strong>
                            </div>
                            <span class="badge bg-light text-secondary border ms-auto" style="font-size: 0.65rem;">${escapeHtml(data.proveedor || 'Gemini Flash')}</span>
                        </div>
                        <div class="bot-markdown-content">${formatearMarkdown(respuestaTexto)}</div>
                    `;
                    contenedorMensajes.appendChild(botBubble);
                }
            } catch (err) {
                console.error('[Chatbot IA Polito] Error en petición:', err);
                setPolitoSidekickState('frustrado', 'No se pudo conectar con el servidor 🔌');
                const botErr = document.createElement('div');
                botErr.className = 'chat-bubble chat-bubble-bot text-danger border-danger border-opacity-50 shadow-sm';
                botErr.innerHTML = `
                    <div class="d-flex align-items-center gap-1 mb-1">
                        <i class="bi bi-wifi-off text-danger me-1"></i>
                        <strong class="small text-danger">Error de Conexión</strong>
                    </div>
                    <div>No se pudo conectar con el servidor de Robo_Gest (${escapeHtml((err && err.message) || 'Error de red')}). Si el servidor se estaba reiniciando o tu sesión expiró, por favor intenta nuevamente o recarga la página.</div>
                    <div class="mt-2"><button type="button" class="btn btn-xs btn-outline-primary" style="font-size: 0.75rem;" onclick="location.reload();"><i class="bi bi-arrow-clockwise me-1"></i>Recargar página</button></div>
                `;
                contenedorMensajes.appendChild(botErr);
            } finally {
                if (btnEnviar) btnEnviar.disabled = false;
                if (typingIndicator) typingIndicator.style.setProperty('display', 'none', 'important');
                contenedorMensajes.scrollTop = contenedorMensajes.scrollHeight;
                inputMensaje.focus();
            }
        }

        formChat.addEventListener('submit', (e) => {
            e.preventDefault();
            enviarConsulta(inputMensaje.value);
        });

        // Manejo de chips de sugerencia rápida
        promptChips.forEach(chip => {
            chip.addEventListener('click', () => {
                const prompt = chip.getAttribute('data-prompt');
                if (prompt) {
                    enviarConsulta(prompt);
                }
            });
        });

        // Formateador robusto de Markdown seguro para el chat
        function formatearMarkdown(texto) {
            if (!texto) return '';
            let html = escapeHtml(texto);

            // Separadores horizontales ---
            html = html.replace(/^---+$/gim, '<hr class="my-2 border-secondary-subtle">');
            // Bloques de código ```codigo```
            html = html.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
            // Código en línea `codigo`
            html = html.replace(/`([^`]+)`/g, '<code class="bg-light px-1 py-0.5 rounded text-primary">$1</code>');
            // Negritas **texto**
            html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
            // Cursivas *texto*
            html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
            // Títulos ### Título y ## Título
            html = html.replace(/^### (.*$)/gim, '<h6 class="fw-bold text-primary mt-2 mb-1">$1</h6>');
            html = html.replace(/^## (.*$)/gim, '<h6 class="fw-bold text-dark mt-2 mb-1">$1</h6>');
            // Listas numeradas 1. item
            html = html.replace(/^\s*(\d+)\.\s+(.*$)/gim, '<div class="d-flex align-items-start gap-1 ms-1 mb-1"><span class="badge bg-light text-primary border me-1" style="font-size: 0.7rem;">$1</span><div>$2</div></div>');
            // Listas no ordenadas (- item o * item)
            html = html.replace(/^\s*[-*]\s+(.*$)/gim, '<div class="d-flex align-items-start gap-1 ms-1 mb-1"><i class="bi bi-check2 text-primary me-1" style="font-size: 0.85rem; margin-top: 2px;"></i><div>$1</div></div>');
            // Enlaces markdown [texto](url)
            html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-primary fw-medium text-decoration-underline">$1</a>');
            // Saltos de línea
            html = html.replace(/\n{2,}/g, '<div class="my-2"></div>');
            html = html.replace(/\n/g, '<br>');

            return html;
        }
    }
}

// Inicializador robusto compatible con recargas de pagina y cache
function startDashboard() {
    initDashboard().catch(err => {
        console.error('[Dashboard] Error fatal en inicializacion:', err);
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startDashboard);
} else {
    startDashboard();
}
