// ========================================
// CONTROLADOR DE PAGINA LOGIN
// Club de Robotica - FP-UNE
// ========================================

document.addEventListener("DOMContentLoaded", function () {

    // 1. Si el usuario ya cuenta con sesion activa, redirigir al Dashboard
    if (window.Auth && typeof window.Auth.redirectIfAuthenticated === "function") {
        if (window.Auth.redirectIfAuthenticated()) {
            return;
        }
    }

    // 2. Mostrar / Ocultar contrasena
    const passwordInput = document.querySelector("#password");
    const showPasswordCheckbox = document.querySelector("#show-password");

    if (passwordInput && showPasswordCheckbox) {
        showPasswordCheckbox.addEventListener("change", function () {
            if (showPasswordCheckbox.checked) {
                passwordInput.type = "text";
            } else {
                passwordInput.type = "password";
            }
        });
    }

    // 3. Inicializar captura y autenticacion del formulario con auth.js
    if (window.Auth && typeof window.Auth.initLoginForm === "function") {
        window.Auth.initLoginForm({
            form: ".login-form",
            identificador: "#cedula",
            password: "#password",
            alertContainer: ".login-card__intro"
        });
    }
});