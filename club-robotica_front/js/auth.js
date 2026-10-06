/**
 * auth.js - Módulo de Autenticación y Manejo de JWT para Robo_Gest
 * Club de Robótica - FP-UNE
 */

(function (global) {
    'use strict';

    // Claves de almacenamiento
    const STORAGE_KEYS = {
        TOKEN: 'robogest_jwt_token',
        USER: 'robogest_user_data'
    };

    /**
     * Resuelve dinámicamente la URL base de la API REST según el entorno.
     */
    function resolveApiBaseUrl() {
        const path = window.location.pathname;
        const origin = window.location.origin;

        // Si se ejecuta desplegado en Tomcat bajo el contexto /robo_gest/
        if (path.startsWith('/robo_gest/')) {
            return `${origin}/robo_gest/api`;
        }
        // Si Tomcat o un reverse proxy corre en la raíz (puerto 8080)
        if (origin.includes(':8080')) {
            return `${origin}/api`;
        }
        // Entorno de desarrollo local independiente (Live Server puerto 5500, etc.)
        return 'http://localhost:8080/robo_gest/api';
    }

    const API_BASE_URL = resolveApiBaseUrl();

    /**
     * Decodifica de forma segura la carga útil (payload) de un JWT en formato JSON.
     * @param {string} token
     * @returns {object|null}
     */
    function parseJwt(token) {
        if (!token || typeof token !== 'string') return null;
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;
            const base64Url = parts[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            return JSON.parse(jsonPayload);
        } catch (e) {
            console.error('[Auth] Error al decodificar JWT:', e);
            return null;
        }
    }

    /**
     * Verifica si el token JWT ha expirado.
     * @param {string} token
     * @returns {boolean}
     */
    function isTokenExpired(token) {
        const claims = parseJwt(token);
        if (!claims || !claims.exp) return true;
        // exp está en segundos, Date.now() en milisegundos
        const expirationTimeMs = claims.exp * 1000;
        return Date.now() >= expirationTimeMs;
    }

    /**
     * Guarda el token y la información del usuario en sessionStorage (o localStorage).
     */
    function setSession(authData) {
        if (!authData || !authData.token) return;

        sessionStorage.setItem(STORAGE_KEYS.TOKEN, authData.token);

        // Si viene un objeto DTO estructurado, lo enriquecemos con los claims del JWT
        const claims = parseJwt(authData.token) || {};
        const userData = {
            id: authData.integranteId || claims.integranteId || null,
            nombreCompleto: authData.nombreCompleto || claims.nombreCompleto || 'Usuario',
            email: authData.email || claims.sub || '',
            ci: authData.ci || claims.ci || '',
            rol: (authData.rol || claims.rol || 'MIEMBRO').toUpperCase(),
            tokenType: authData.tokenType || 'Bearer'
        };

        sessionStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(userData));
    }

    /**
     * Obtiene el token JWT actual almacenado.
     * @returns {string|null}
     */
    function getToken() {
        const token = sessionStorage.getItem(STORAGE_KEYS.TOKEN);
        if (!token || isTokenExpired(token)) {
            if (token) clearSession();
            return null;
        }
        return token;
    }

    /**
     * Obtiene los datos del usuario actual almacenado.
     * @returns {object|null}
     */
    function getUser() {
        const token = getToken();
        if (!token) return null;
        const userStr = sessionStorage.getItem(STORAGE_KEYS.USER);
        if (!userStr) return null;
        try {
            return JSON.parse(userStr);
        } catch (e) {
            return null;
        }
    }

    /**
     * Obtiene el rol del usuario autenticado ('ADMIN', 'DIRECTIVA', 'MIEMBRO').
     * @returns {string}
     */
    function getUserRole() {
        const user = getUser();
        return user && user.rol ? user.rol.toUpperCase() : 'INVITADO';
    }

    /**
     * Comprueba si el usuario autenticado tiene al menos uno de los roles especificados.
     * @param  {...string} allowedRoles
     * @returns {boolean}
     */
    function hasAnyRole(...allowedRoles) {
        const currentRole = getUserRole();
        return allowedRoles.map(r => r.toUpperCase()).includes(currentRole);
    }

    /**
     * Verifica si el usuario cuenta con una sesión válida no expirada.
     * @returns {boolean}
     */
    function isAuthenticated() {
        return getToken() !== null;
    }

    /**
     * Limpia la sesión actual.
     */
    function clearSession() {
        sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
        sessionStorage.removeItem(STORAGE_KEYS.USER);
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.USER);
    }

    /**
     * Cierra sesión y redirige a la página de login.
     */
    function logout() {
        clearSession();
        window.location.href = 'login.html';
    }

    /**
     * Envoltorio interceptor para Fetch API.
     * Inyecta automáticamente la cabecera 'Authorization: Bearer <token>'
     * y captura respuestas HTTP 401 para redirigir a login.html si el token expiró.
     *
     * @param {string} endpoint - Ruta relativa (ej: '/prestamos/1/aprobar') o absoluta
     * @param {RequestInit} [options={}] - Opciones de fetch estándar
     * @returns {Promise<Response>}
     */
    async function authFetch(endpoint, options = {}) {
        // Resolver URL completa
        let url = endpoint;
        if (!endpoint.startsWith('http://') && !endpoint.startsWith('https://')) {
            const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
            url = `${API_BASE_URL}${cleanEndpoint}`;
        }

        // Preparar cabeceras
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) {
            headers.set('Accept', 'application/json');
        }

        // Si se envía un cuerpo no-FormData, asegurar Content-Type application/json
        if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        // Inyectar token JWT si existe sesión activa
        const token = getToken();
        if (token) {
            headers.set('Authorization', `Bearer ${token}`);
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Interceptar error 401 Unauthorized (Token expirado o inválido)
            if (response.status === 401) {
                console.warn('[Auth] Error HTTP 401 recibido. Redirigiendo a login por sesión expirada.');
                clearSession();
                // Redirigir al usuario al login con parámetro para mostrar notificación
                window.location.href = 'login.html?expired=true';
                return response;
            }

            return response;
        } catch (error) {
            console.error('[Auth] Error en petición authFetch:', error);
            throw error;
        }
    }

    /**
     * Realiza el inicio de sesión enviando LoginRequestDTO a /api/auth/login.
     * @param {string} identificador - Cédula o Email
     * @param {string} password - Contraseña en texto plano
     * @returns {Promise<object>} AuthResponseDTO
     */
    async function login(identificador, password) {
        if (!identificador || !password) {
            throw new Error('Debe ingresar el identificador (cédula o email) y la contraseña.');
        }

        const payload = {
            identificador: identificador.trim(),
            password: password
        };

        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            let errorMsg = 'Error al iniciar sesión.';
            if (data && data.message) {
                errorMsg = data.message;
            } else if (response.status === 401) {
                errorMsg = 'Credenciales inválidas. Verifique su número de cédula y contraseña.';
            } else if (response.status === 403) {
                errorMsg = 'Su cuenta se encuentra inactiva. Contacte a la directiva del club.';
            }
            throw new Error(errorMsg);
        }

        // Almacenar token y datos del usuario
        setSession(data);
        return data;
    }

    /**
     * Guardia de ruta para proteger páginas privadas (ej: dashboard.html).
     * Si el usuario no está autenticado, redirige a login.html.
     */
    function requireAuth() {
        if (!isAuthenticated()) {
            window.location.href = 'login.html';
            return false;
        }
        return true;
    }

    /**
     * Guardia para la página de login.html.
     * Si ya está autenticado, redirige automáticamente a dashboard.html.
     */
    function redirectIfAuthenticated() {
        if (isAuthenticated()) {
            window.location.href = 'dashboard.html';
            return true;
        }
        return false;
    }

    /**
     * Inicializa la captura del formulario de login en login.html.
     * @param {object} [selectors]
     */
    function initLoginForm(selectors = {}) {
        const formSelector = selectors.form || '.login-form';
        const idSelector = selectors.identificador || '#cedula';
        const passSelector = selectors.password || '#password';
        const alertContainerSelector = selectors.alertContainer || '.login-card__intro';

        const form = document.querySelector(formSelector);
        if (!form) return;

        // Mensaje si venía de sesión expirada
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('expired') === 'true') {
            mostrarMensajeAlerta(
                'Su sesión ha expirado por seguridad. Por favor, ingrese sus credenciales nuevamente.',
                'warning',
                alertContainerSelector
            );
        }

        form.addEventListener('submit', async function (e) {
            e.preventDefault();

            const idInput = form.querySelector(idSelector);
            const passInput = form.querySelector(passSelector);
            const submitBtn = form.querySelector('button[type="submit"]');

            const identificador = idInput ? idInput.value.trim() : '';
            const password = passInput ? passInput.value : '';

            limpiarAlertas();

            if (!identificador || !password) {
                mostrarMensajeAlerta(
                    'Por favor complete todos los campos requeridos.',
                    'danger',
                    alertContainerSelector
                );
                return;
            }

            // Estado de carga en el botón
            const textoOriginal = submitBtn ? submitBtn.innerHTML : 'Iniciar sesión';
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `
                    <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true" style="display:inline-block; width:1rem; height:1rem; border:2px solid currentColor; border-right-color:transparent; border-radius:50%; animation:spin .75s linear infinite;"></span>
                    Iniciando sesión...
                `;
            }

            try {
                await login(identificador, password);
                // Redirigir al Dashboard privado
                window.location.href = 'dashboard.html';
            } catch (err) {
                mostrarMensajeAlerta(err.message, 'danger', alertContainerSelector);
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = textoOriginal;
                }
            }
        });
    }

    function mostrarMensajeAlerta(mensaje, tipo, contenedorSelector) {
        limpiarAlertas();
        const contenedor = document.querySelector(contenedorSelector);
        if (!contenedor) {
            alert(mensaje);
            return;
        }

        const alertaDiv = document.createElement('div');
        alertaDiv.className = `auth-alert auth-alert--${tipo}`;
        alertaDiv.id = 'auth-alert-box';
        alertaDiv.style.padding = '0.75rem 1rem';
        alertaDiv.style.margin = '1rem 0';
        alertaDiv.style.borderRadius = '0.5rem';
        alertaDiv.style.fontSize = '0.9rem';
        alertaDiv.style.fontWeight = '500';
        alertaDiv.style.textAlign = 'left';

        if (tipo === 'danger') {
            alertaDiv.style.backgroundColor = '#FEE2E2';
            alertaDiv.style.color = '#991B1B';
            alertaDiv.style.border = '1px solid #FCA5A5';
        } else if (tipo === 'warning') {
            alertaDiv.style.backgroundColor = '#FEF3C7';
            alertaDiv.style.color = '#92400E';
            alertaDiv.style.border = '1px solid #FCD34D';
        } else {
            alertaDiv.style.backgroundColor = '#D1FAE5';
            alertaDiv.style.color = '#065F46';
            alertaDiv.style.border = '1px solid #6EE7B7';
        }

        alertaDiv.textContent = mensaje;
        contenedor.insertAdjacentElement('afterend', alertaDiv);
    }

    function limpiarAlertas() {
        const existente = document.getElementById('auth-alert-box');
        if (existente) {
            existente.remove();
        }
    }

    // Exportar módulo en objeto global
    global.Auth = {
        API_BASE_URL,
        login,
        logout,
        authFetch,
        getToken,
        getUser,
        getUserRole,
        hasAnyRole,
        isAuthenticated,
        parseJwt,
        isTokenExpired,
        requireAuth,
        redirectIfAuthenticated,
        initLoginForm,
        clearSession
    };

})(typeof window !== 'undefined' ? window : this);
