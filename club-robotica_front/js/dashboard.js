/**
 * dashboard.js - Lógica interactiva para el Panel de Control Privado de Robo_Gest
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

    // Estado global de préstamos
    let prestamosData = [];
    let prestamoSeleccionadoId = null;

    // Inicializar Componentes de UI
    initUserInfoUI();
    initRoleAccessControl();
    initSidebarToggle();
    initLogoutHandlers();
    initRfidSimulator();

    // 3. Petición GET inicial para verificar la API y cargar datos con el wrapper JWT
    await executeInitialGetAndLoadData();

    /**
     * Renderiza los datos del usuario en la barra de navegación, sidebar y banner.
     */
    function initUserInfoUI() {
        // Iniciales para el avatar
        const initials = userName
            .split(' ')
            .filter(Boolean)
            .map(n => n[0])
            .slice(0, 2)
            .join('')
            .toUpperCase() || 'U';

        // Elementos de la interfaz
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

        // Badges con estilo según el rol
        applyRoleBadge('sidebarUserRoleBadge', userRole);
        applyRoleBadge('topbarUserRoleBadge', userRole);
        applyRoleBadge('welcomeRoleBadge', userRole);

        const adminRoleName = document.getElementById('adminDirectivaRoleName');
        if (adminRoleName) adminRoleName.textContent = userRole;

        // Fecha actual en español
        const currentDateElem = document.getElementById('currentDateDisplay');
        if (currentDateElem) {
            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            currentDateElem.textContent = new Date().toLocaleDateString('es-PY', options);
        }
    }

    /**
     * Aplica estilos visuales y texto a los badges de rol.
     */
    function applyRoleBadge(elemId, role) {
        const badge = document.getElementById(elemId);
        if (!badge) return;

        badge.textContent = role;
        badge.className = 'badge';

        if (role === 'ADMIN') {
            badge.classList.add('badge-role-ADMIN');
        } else if (role === 'DIRECTIVA') {
            badge.classList.add('badge-role-DIRECTIVA');
        } else {
            badge.classList.add('badge-role-MIEMBRO');
        }
    }

    /**
     * REGLA DE NEGOCIO CRÍTICA: Control de acceso por rol en el Frontend.
     * Renderiza u oculta elementos del menú y botones basándose en el rol extraído del JWT.
     */
    function initRoleAccessControl() {
        const restrictedElements = document.querySelectorAll('.role-restricted, [data-allowed-roles]');

        restrictedElements.forEach(el => {
            const allowedAttr = el.getAttribute('data-allowed-roles');
            if (!allowedAttr) return;

            const allowedRoles = allowedAttr.split(',').map(r => r.trim().toUpperCase());
            const hasAccess = allowedRoles.includes(userRole);

            if (!hasAccess) {
                // Si el usuario no tiene el rol permitido, ocultamos el elemento completamente
                el.style.display = 'none';
            } else {
                // Si tiene el rol y es un elemento de lista o bloque, asegurar visualización adecuada
                el.style.removeProperty('display');
            }
        });
    }

    /**
     * Inicializa el comportamiento responsivo del Sidebar.
     */
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

    /**
     * Vincula los botones de cierre de sesión al método Auth.logout().
     */
    function initLogoutHandlers() {
        const btnTopbarLogout = document.getElementById('btnTopbarLogout');
        const btnSidebarLogout = document.getElementById('btnSidebarLogout');

        const handleLogout = (e) => {
            e.preventDefault();
            if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
                window.Auth.logout();
            }
        };

        if (btnTopbarLogout) btnTopbarLogout.addEventListener('click', handleLogout);
        if (btnSidebarLogout) btnSidebarLogout.addEventListener('click', handleLogout);
    }

    /**
     * Realiza una petición GET inicial al backend con el envoltorio fetch y el JWT
     * para verificar la conectividad de la API y renderizar la vista de préstamos.
     */
    async function executeInitialGetAndLoadData() {
        try {
            console.log('[Dashboard] Ejecutando petición GET inicial utilizando authFetch con JWT...');

            // Petición GET al endpoint raíz de la API para certificar el estado y los claims
            const apiInfoResponse = await window.Auth.authFetch('/', { method: 'GET' });

            if (apiInfoResponse.ok) {
                const apiInfo = await apiInfoResponse.json();
                console.log('[Dashboard] API conectada exitosamente:', apiInfo);
            }

            // Simulación / Carga estructurada de préstamos activos consumiendo los datos
            await loadLoansSummary();

        } catch (error) {
            console.error('[Dashboard] Error en petición inicial:', error);
            showNotification('No se pudo sincronizar con el servidor REST. Verifique que Apache Tomcat esté en ejecución.', 'danger');
            renderLoansError('Error de conexión con el backend.');
        }
    }

    /**
     * Carga y renderiza el resumen de préstamos activos.
     */
    async function loadLoansSummary() {
        const tbody = document.getElementById('loansTableBody');
        const loansCountBadge = document.getElementById('loansCountBadge');

        // Datos de muestra con la estructura exacta de nuestro esquema y DTOs
        prestamosData = [
            {
                id: 101,
                integranteNombre: "Lucas Benítez",
                integranteCarrera: "INGENIERIA_DE_SISTEMAS",
                proyectoNombre: "Brazo Robótico Autónomo",
                materialNombre: "Arduino Mega 2560 R3",
                cantidad: 2,
                fechaPrevista: "2026-10-15",
                estado: "PENDIENTE"
            },
            {
                id: 102,
                integranteNombre: "María Almada",
                integranteCarrera: "INGENIERIA_ELECTRICA",
                proyectoNombre: "Vehículo Seguidor de Línea",
                materialNombre: "Sensor Ultrasonido HC-SR04",
                cantidad: 4,
                fechaPrevista: "2026-10-12",
                estado: "PENDIENTE"
            },
            {
                id: 103,
                integranteNombre: "Carlos Villalba",
                integranteCarrera: "ANALISIS_DE_SISTEMAS",
                proyectoNombre: "Drone de Reconocimiento",
                materialNombre: "Batería LiPo 3S 2200mAh",
                cantidad: 1,
                fechaPrevista: "2026-10-20",
                estado: "ENTREGADO"
            },
            {
                id: 104,
                integranteNombre: "Sofía Martínez",
                integranteCarrera: "COLABORADOR",
                proyectoNombre: "Estación Meteorológica IoT",
                materialNombre: "Módulo ESP32 DevKit V1",
                cantidad: 1,
                fechaPrevista: "2026-10-18",
                estado: "APROBADO"
            }
        ];

        // Actualizar KPIs numéricos
        const pendientesCount = prestamosData.filter(p => p.estado === 'PENDIENTE').length;
        const activosCount = prestamosData.filter(p => p.estado === 'ENTREGADO' || p.estado === 'APROBADO').length;

        const kpiPrestamos = document.getElementById('kpiPrestamosActivos');
        const kpiAsistencias = document.getElementById('kpiAsistenciasMes');
        const kpiStock = document.getElementById('kpiComponentesStock');

        if (kpiPrestamos) kpiPrestamos.textContent = `${activosCount} (${pendientesCount} pend.)`;
        if (kpiAsistencias) kpiAsistencias.textContent = '14 marcaciones';
        if (kpiStock) kpiStock.textContent = '84 unidades';

        const pendingBadge = document.getElementById('pendingLoansBadge');
        if (pendingBadge) {
            pendingBadge.textContent = pendientesCount;
            pendingBadge.style.display = pendientesCount > 0 ? 'inline-block' : 'none';
        }

        if (loansCountBadge) {
            loansCountBadge.textContent = `${prestamosData.length} registros en total`;
        }

        renderLoansTable(prestamosData);
    }

    /**
     * Renderiza la tabla HTML de préstamos aplicando la regla de visibilidad de botones por rol.
     */
    function renderLoansTable(loans) {
        const tbody = document.getElementById('loansTableBody');
        if (!tbody) return;

        if (!loans || loans.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">
                        No hay solicitudes de préstamos registradas.
                    </td>
                </tr>
            `;
            return;
        }

        const canApprove = window.Auth.hasAnyRole('ADMIN', 'DIRECTIVA');

        tbody.innerHTML = loans.map(p => {
            let statusBadgeClass = 'bg-secondary';
            if (p.estado === 'PENDIENTE') statusBadgeClass = 'bg-warning text-dark';
            else if (p.estado === 'APROBADO') statusBadgeClass = 'bg-info text-dark';
            else if (p.estado === 'ENTREGADO') statusBadgeClass = 'bg-primary text-white';
            else if (p.estado === 'DEVUELTO') statusBadgeClass = 'bg-success text-white';

            // REGLA CRÍTICA: Botón "Aprobar" solo visible para ADMIN / DIRECTIVA en préstamos PENDIENTES
            let actionBtnHtml = '';
            if (canApprove && p.estado === 'PENDIENTE') {
                actionBtnHtml = `
                    <button class="btn btn-sm btn-success fw-semibold btn-aprobar-prestamo" data-prestamo-id="${p.id}">
                        <i class="bi bi-check-circle me-1"></i> Aprobar
                    </button>
                `;
            } else if (p.estado === 'PENDIENTE') {
                actionBtnHtml = `
                    <span class="badge bg-light text-muted border">En Espera</span>
                `;
            } else {
                actionBtnHtml = `
                    <span class="badge bg-light text-secondary border">Procesado</span>
                `;
            }

            return `
                <tr id="row-prestamo-${p.id}">
                    <td class="ps-4 fw-bold text-primary">#${p.id}</td>
                    <td>
                        <span class="fw-semibold d-block">${escapeHtml(p.integranteNombre)}</span>
                        <small class="text-muted" style="font-size:0.75rem;">${escapeHtml(p.integranteCarrera)}</small>
                    </td>
                    <td><span class="badge bg-light text-dark border">${escapeHtml(p.proyectoNombre)}</span></td>
                    <td>
                        <div class="fw-medium">${escapeHtml(p.materialNombre)}</div>
                        <small class="text-muted">${p.cantidad} unidad(es)</small>
                    </td>
                    <td><small class="text-muted"><i class="bi bi-calendar-event me-1"></i>${p.fechaPrevista}</small></td>
                    <td><span class="badge ${statusBadgeClass} py-1 px-2" id="badge-estado-${p.id}">${p.estado}</span></td>
                    <td class="text-end pe-4" id="action-cell-${p.id}">
                        ${actionBtnHtml}
                    </td>
                </tr>
            `;
        }).join('');

        // Adjuntar eventos a los botones de aprobación
        attachApproveButtonListeners();
    }

    /**
     * Adjunta listeners a los botones "Aprobar" renderizados.
     */
    function attachApproveButtonListeners() {
        const approveButtons = document.querySelectorAll('.btn-aprobar-prestamo');
        const modalElement = document.getElementById('modalAprobarPrestamo');
        const modalIdText = document.getElementById('modalPrestamoIdText');
        const btnConfirmar = document.getElementById('btnConfirmarAprobacion');

        if (!modalElement || !approveButtons.length) return;

        const modalInstance = new bootstrap.Modal(modalElement);

        approveButtons.forEach(btn => {
            btn.addEventListener('click', function () {
                prestamoSeleccionadoId = this.getAttribute('data-prestamo-id');
                if (modalIdText) modalIdText.textContent = `#${prestamoSeleccionadoId}`;
                modalInstance.show();
            });
        });

        // Evento de confirmación en el Modal
        if (btnConfirmar) {
            btnConfirmar.onclick = async function () {
                if (!prestamoSeleccionadoId) return;

                btnConfirmar.disabled = true;
                btnConfirmar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Aprobando...`;

                try {
                    await ejecutarAprobacionPrestamo(prestamoSeleccionadoId);
                    modalInstance.hide();
                } finally {
                    btnConfirmar.disabled = false;
                    btnConfirmar.innerHTML = `<i class="bi bi-check-lg me-1"></i> Confirmar Aprobación`;
                }
            };
        }
    }

    /**
     * Invoca el endpoint protegido POST /api/prestamos/{id}/aprobar utilizando el JWT
     */
    async function ejecutarAprobacionPrestamo(id) {
        try {
            console.log(`[Dashboard] Solicitando aprobación transaccional para préstamo ID ${id}...`);

            const response = await window.Auth.authFetch(`/prestamos/${id}/aprobar`, {
                method: 'POST',
                body: JSON.stringify({
                    prestamoId: Number(id),
                    observacion: 'Aprobación autorizada desde el Panel Web Frontend'
                })
            });

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                let errorMsg = 'Error al aprobar el préstamo.';
                if (data && data.message) {
                    errorMsg = data.message;
                } else if (response.status === 409) {
                    errorMsg = 'Stock insuficiente en los materiales para aprobar la solicitud.';
                } else if (response.status === 403) {
                    errorMsg = 'No posees permisos de ADMIN o DIRECTIVA para autorizar esta operación.';
                }
                showNotification(errorMsg, 'warning');
                return;
            }

            // Actualización visual exitosa
            showNotification(`¡Préstamo #${id} aprobado exitosamente con bloqueo pesimista de stock!`, 'success');

            const badgeEstado = document.getElementById(`badge-estado-${id}`);
            const cellAction = document.getElementById(`action-cell-${id}`);

            if (badgeEstado) {
                badgeEstado.className = 'badge bg-info text-dark py-1 px-2';
                badgeEstado.textContent = 'APROBADO';
            }

            if (cellAction) {
                cellAction.innerHTML = `<span class="badge bg-light text-success border"><i class="bi bi-check2"></i> Aprobado</span>`;
            }

            // Descontar del badge de pendientes
            const pendingBadge = document.getElementById('pendingLoansBadge');
            if (pendingBadge) {
                const current = parseInt(pendingBadge.textContent, 10) || 1;
                const nextVal = Math.max(0, current - 1);
                pendingBadge.textContent = nextVal;
                if (nextVal === 0) pendingBadge.style.display = 'none';
            }

        } catch (error) {
            console.error('[Dashboard] Error al aprobar préstamo:', error);
            showNotification('Error al comunicarse con el servidor REST.', 'danger');
        }
    }

    /**
     * Inicializa el formulario interactivo para simular lecturas RFID del ESP32
     */
    function initRfidSimulator() {
        const form = document.getElementById('formMarcacionRfid');
        const inputNfcUid = document.getElementById('inputNfcUid');
        const inputDispositivo = document.getElementById('inputDispositivo');
        const resultadoContainer = document.getElementById('resultadoMarcacion');
        const btnMarcar = document.getElementById('btnMarcarRfid');

        if (!form) return;

        form.addEventListener('submit', async function (e) {
            e.preventDefault();

            const nfcUid = inputNfcUid ? inputNfcUid.value.trim() : '';
            const dispositivo = inputDispositivo ? inputDispositivo.value.trim() : 'ESP32_LAB';

            if (!nfcUid) {
                alert('Ingrese el UID de la tarjeta.');
                return;
            }

            if (btnMarcar) {
                btnMarcar.disabled = true;
                btnMarcar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Enviando...`;
            }

            try {
                const response = await window.Auth.authFetch('/asistencia/marcar', {
                    method: 'POST',
                    body: JSON.stringify({
                        nfcUid: nfcUid,
                        dispositivo: dispositivo
                    })
                });

                const data = await response.json().catch(() => null);

                if (!response.ok) {
                    const msg = (data && data.message) ? data.message : 'Error al registrar marcación RFID.';
                    renderRfidResult(false, msg, resultadoContainer);
                } else {
                    const tipo = data.tipo || 'ENTRADA';
                    const nombre = data.integranteNombre || 'Integrante';
                    const hora = data.fechaHora ? new Date(data.fechaHora).toLocaleTimeString() : new Date().toLocaleTimeString();
                    const msg = `Marcación registrada: <strong>${tipo}</strong> para <strong>${escapeHtml(nombre)}</strong> a las ${hora}.`;
                    renderRfidResult(true, msg, resultadoContainer);
                }
            } catch (err) {
                renderRfidResult(false, 'No se pudo conectar con el endpoint /api/asistencia/marcar.', resultadoContainer);
            } finally {
                if (btnMarcar) {
                    btnMarcar.disabled = false;
                    btnMarcar.innerHTML = `<i class="bi bi-send me-1"></i> Simular Marcación`;
                }
            }
        });
    }

    function renderRfidResult(isSuccess, message, container) {
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

    /**
     * Muestra notificaciones flotantes en el contenedor superior.
     */
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

    function renderLoansError(msg) {
        const tbody = document.getElementById('loansTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="text-center py-4 text-danger">
                        <i class="bi bi-exclamation-triangle me-1"></i> ${escapeHtml(msg)}
                    </td>
                </tr>
            `;
        }
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

    // Botón refrescar préstamos
    const btnRefresh = document.getElementById('btnRefreshLoans');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', async function () {
            btnRefresh.disabled = true;
            btnRefresh.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Actualizando...`;
            await loadLoansSummary();
            btnRefresh.disabled = false;
            btnRefresh.innerHTML = `<i class="bi bi-arrow-clockwise me-1"></i> Actualizar Solicitudes`;
        });
    }
});
