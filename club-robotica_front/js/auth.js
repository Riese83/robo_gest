/**
 * auth.js - Modulo de Autenticacion y Manejo de JWT para Robo_Gest
 * Club de Robotica - FP-UNE
 */

(function (global) {
    'use strict';

    // Claves de almacenamiento
    const STORAGE_KEYS = {
        TOKEN: 'robogest_jwt_token',
        USER: 'robogest_user_data'
    };

    /**
     * Utilidad global para escapar caracteres HTML especiales y prevenir inyecciones XSS
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
    global.escapeHtml = escapeHtml;

    /**
     * Resuelve dinamicamente la URL base de la API REST segun el entorno.
     */
    function resolveApiBaseUrl() {
        const protocol = (window.location.protocol || '').toLowerCase();
        const origin = window.location.origin || '';
        const port = window.location.port || '';
        const path = (window.location.pathname || '').toLowerCase();

        // 1. Si se abre directamente desde el sistema de archivos (file://)
        if (protocol === 'file:' || !origin || origin === 'null' || origin.startsWith('file')) {
            return 'http://localhost:8080/robo_gest/api';
        }

        // 2. Si corre en un servidor estatico de desarrollo frontend (ej: Live Server 5500, Vite 5173, Python 8000)
        if (port && port !== '8080' && port !== '80' && port !== '443') {
            return 'http://localhost:8080/robo_gest/api';
        }

        // 3. Si la ruta contiene /robo_gest (despliegue estandar en Tomcat bajo contexto /robo_gest)
        if (path.includes('/robo_gest')) {
            return `${origin}/robo_gest/api`;
        }

        // 4. Si se ejecuta en la raiz de un host o detras de un reverse proxy
        return `${origin}/api`;
    }

    const API_BASE_URL = resolveApiBaseUrl();

    /**
     * Decodifica de forma segura la carga util (payload) de un JWT en formato JSON.
     * Soportando Base64Url sin padding y decodificacion de caracteres UTF-8 en cualquier navegador.
     * @param {string} token
     * @returns {object|null}
     */
    function parseJwt(token) {
        if (!token || typeof token !== 'string') return null;
        try {
            const parts = token.split('.');
            if (parts.length < 2) return null;
            let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            // Completar relleno padding '=' de Base64 si falta
            while (base64.length % 4 !== 0) {
                base64 += '=';
            }
            // Decodificar Base64 a bytes binarios y convertirlos a texto UTF-8 seguro
            const binaryString = atob(base64);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            let jsonPayload;
            if (typeof TextDecoder !== 'undefined') {
                jsonPayload = new TextDecoder('utf-8').decode(bytes);
            } else {
                jsonPayload = decodeURIComponent(
                    Array.prototype.map.call(bytes, b => '%' + ('00' + b.toString(16)).slice(-2)).join('')
                );
            }
            return JSON.parse(jsonPayload);
        } catch (e) {
            console.warn('[Auth] Error al decodificar JWT en cliente:', e);
            return null;
        }
    }

    /**
     * Verifica si el token JWT ha expirado.
     * IMPORTANTE: No asume expiracion en el cliente ante fallos de parseo o desincronizacion de reloj;
     * delega la verificacion criptografica estricta al backend (HTTP 401).
     * @param {string} token
     * @returns {boolean}
     */
    function isTokenExpired(token) {
        if (!token || typeof token !== 'string') return true;
        const claims = parseJwt(token);
        if (!claims || typeof claims.exp !== 'number') {
            // Si no podemos leer los claims o exp no es numerico, NO asumimos expirado prematuramente;
            // dejamos que el backend valide via 401 si realmente no es valido.
            return false;
        }

        const nowSeconds = Math.floor(Date.now() / 1000);

        // Si el token tiene fecha de emision (iat) y difiere en mas de 12 horas del reloj local del navegador,
        // existe desincronizacion de reloj (clock skew) entre cliente y servidor.
        // Confiamos en el backend para no bloquear al usuario por fecha local erronea.
        if (typeof claims.iat === 'number' && Math.abs(nowSeconds - claims.iat) > 12 * 3600) {
            console.warn('[Auth] Desincronizacion detectada entre reloj cliente y servidor. Delegando validacion al backend.');
            return false;
        }

        return nowSeconds >= claims.exp;
    }

    /**
     * Guarda el token y la informacion del usuario en sessionStorage y localStorage.
     */
    function setSession(authData) {
        if (!authData || !authData.token) return;

        sessionStorage.setItem(STORAGE_KEYS.TOKEN, authData.token);
        localStorage.setItem(STORAGE_KEYS.TOKEN, authData.token);

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

        const userJson = JSON.stringify(userData);
        sessionStorage.setItem(STORAGE_KEYS.USER, userJson);
        localStorage.setItem(STORAGE_KEYS.USER, userJson);
    }

    /**
     * Actualiza los datos del usuario en la sesion actual.
     */
    function setUser(userData) {
        if (!userData) return;
        const userJson = JSON.stringify(userData);
        sessionStorage.setItem(STORAGE_KEYS.USER, userJson);
        localStorage.setItem(STORAGE_KEYS.USER, userJson);
    }

    /**
     * Obtiene el token JWT actual almacenado (consulta sessionStorage con fallback a localStorage).
     * @returns {string|null}
     */
    function getToken() {
        let token = sessionStorage.getItem(STORAGE_KEYS.TOKEN);
        if (!token) {
            token = localStorage.getItem(STORAGE_KEYS.TOKEN);
            if (token && !isTokenExpired(token)) {
                sessionStorage.setItem(STORAGE_KEYS.TOKEN, token);
            }
        }
        if (!token) {
            return null;
        }
        if (isTokenExpired(token)) {
            console.warn('[Auth] Token JWT expirado detectado.');
            clearSession();
            return null;
        }
        return token;
    }

    /**
     * Obtiene los datos del usuario actual almacenado.
     * @returns {object|null}
     */
    function getUser() {
        let userStr = sessionStorage.getItem(STORAGE_KEYS.USER);
        if (!userStr) {
            userStr = localStorage.getItem(STORAGE_KEYS.USER);
            if (userStr) {
                sessionStorage.setItem(STORAGE_KEYS.USER, userStr);
            }
        }
        if (userStr) {
            try {
                return JSON.parse(userStr);
            } catch (e) {
                console.warn('[Auth] Error al parsear datos de usuario en sesion:', e);
            }
        }
        // Fallback: si hay token pero no se guardo el objeto USER, extraerlo del JWT
        const token = getToken();
        if (token) {
            const claims = parseJwt(token);
            if (claims) {
                return {
                    id: claims.integranteId || null,
                    nombreCompleto: claims.nombreCompleto || 'Usuario',
                    email: claims.sub || '',
                    ci: claims.ci || '',
                    rol: (claims.rol || 'MIEMBRO').toUpperCase(),
                    tokenType: 'Bearer'
                };
            }
        }
        return null;
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
     * Verifica si el usuario cuenta con una sesion valida no expirada.
     * @returns {boolean}
     */
    function isAuthenticated() {
        return getToken() !== null;
    }

    /**
     * Limpia la sesion actual en todos los almacenamientos.
     */
    function clearSession() {
        sessionStorage.removeItem(STORAGE_KEYS.TOKEN);
        sessionStorage.removeItem(STORAGE_KEYS.USER);
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.USER);
    }

    /**
     * Cierra sesion y redirige a la pagina de login.
     */
    function logout() {
        clearSession();
        window.location.href = 'login.html';
    }

    /**
     * Envoltorio interceptor para Fetch API.
     * Inyecta automaticamente la cabecera 'Authorization: Bearer <token>'
     * y captura respuestas HTTP 401 para redirigir a login.html si el token expiro.
     *
     * @param {string} endpoint - Ruta relativa (ej: '/prestamos/1/aprobar') o absoluta
     * @param {RequestInit} [options={}] - Opciones de fetch estandar
     * @returns {Promise<Response>}
     */
    async function authFetch(endpoint, options = {}) {
        // Resolver URL completa dinamicamente
        let url = endpoint;
        if (!endpoint.startsWith('http://') && !endpoint.startsWith('https://')) {
            const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
            const baseUrl = resolveApiBaseUrl();
            url = `${baseUrl}${cleanEndpoint}`;
        }

        // Preparar cabeceras
        const headers = new Headers(options.headers || {});
        if (!headers.has('Accept')) {
            headers.set('Accept', 'application/json');
        }

        // Si es POST, PUT o PATCH y no es FormData, asegurar Content-Type application/json
        const method = (options.method || 'GET').toUpperCase();
        if ((options.body || method === 'POST' || method === 'PUT' || method === 'PATCH') && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        // Inyectar token JWT si existe sesion activa
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

            // Interceptar error 401 Unauthorized (Token expirado o invalido)
            if (response.status === 401) {
                console.warn('[Auth] Error HTTP 401 recibido. Redirigiendo a login por sesion expirada.');
                clearSession();
                // Redirigir al usuario al login con parametro para mostrar notificacion
                window.location.href = 'login.html?expired=true';
                return response;
            }

            return response;
        } catch (error) {
            console.error('[Auth] Error en peticion authFetch:', error);
            throw error;
        }
    }

    /**
     * Realiza el inicio de sesion enviando LoginRequestDTO a /api/auth/login.
     * @param {string} identificador - Cedula o Email
     * @param {string} password - Contrasena en texto plano
     * @returns {Promise<object>} AuthResponseDTO
     */
    async function login(identificador, password) {
        if (!identificador || !password) {
            throw new Error('Debe ingresar el identificador (cedula o email) y la contrasena.');
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
            let errorMsg = 'Error al iniciar sesion.';
            if (data && data.message) {
                errorMsg = data.message;
            } else if (response.status === 401) {
                errorMsg = 'Credenciales invalidas. Verifique su numero de cedula y contrasena.';
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
     * Guardia de ruta para proteger paginas privadas (ej: dashboard.html).
     * Si el usuario no esta autenticado, redirige a login.html.
     */
    function requireAuth() {
        if (!isAuthenticated()) {
            console.warn('[Auth] Acceso denegado: sesion inactiva o no iniciada. Redirigiendo a login.html.');
            window.location.href = 'login.html';
            return false;
        }
        return true;
    }

    /**
     * Guardia para la pagina de login.html.
     * Si ya esta autenticado, redirige automaticamente a dashboard.html.
     * Si la pagina se abrio con ?expired=true o ?logout=true, no redirige y limpia la sesion.
     */
    function redirectIfAuthenticated() {
        try {
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('expired') === 'true' || urlParams.get('logout') === 'true') {
                clearSession();
                return false;
            }
        } catch (e) {
            // Entorno sin URLSearchParams
        }

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

        // Mensaje si venia de sesion expirada
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('expired') === 'true') {
            mostrarMensajeAlerta(
                'Su sesion ha expirado por seguridad. Por favor, ingrese sus credenciales nuevamente.',
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

            // Estado de carga en el boton
            const textoOriginal = submitBtn ? submitBtn.innerHTML : 'Iniciar sesion';
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = `
                    <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true" style="display:inline-block; width:1rem; height:1rem; border:2px solid currentColor; border-right-color:transparent; border-radius:50%; animation:spin .75s linear infinite;"></span>
                    Iniciando sesion...
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

    // Exportar modulo en objeto global
    global.Auth = {
        API_BASE_URL,
        login,
        logout,
        authFetch,
        getToken,
        getUser,
        setUser,
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
