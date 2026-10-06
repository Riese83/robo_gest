// ========================================
// MOSTRAR Y OCULTAR CONTRASEÑA
// ========================================

// Buscamos el campo de contraseña
const passwordInput = document.querySelector("#password");

// Buscamos el checkbox
const showPasswordCheckbox = document.querySelector(
    "#show-password"
);

// Solo ejecutamos esta lógica si ambos elementos existen
if (passwordInput && showPasswordCheckbox) {

    showPasswordCheckbox.addEventListener(
        "change",
        function () {

            // Si el checkbox está marcado,
            // mostramos la contraseña
            if (showPasswordCheckbox.checked) {

                passwordInput.type = "text";

            }

            // Si no está marcado,
            // ocultamos la contraseña
            else {

                passwordInput.type = "password";

            }

        }
    );

}