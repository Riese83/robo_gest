const navToggle = document.querySelector(".nav-toggle");
const navMenu = document.querySelector(".nav-menu");
const siteHeader = document.querySelector(".site-header");

/* ========================================
   CERRAR MENU MOVIL
======================================== */
function closeMenu() {
    if (navMenu) {
        navMenu.classList.remove("nav-menu--open");
    }
    if (navToggle) {
        navToggle.classList.remove("nav-toggle--open");
        navToggle.setAttribute("aria-expanded", "false");
    }
}

/* ========================================
   ABRIR Y CERRAR MENU MOVIL
======================================== */
if (navToggle && navMenu) {
    navToggle.addEventListener("click", function () {
        navMenu.classList.toggle("nav-menu--open");
        navToggle.classList.toggle("nav-toggle--open");
        const menuIsOpen = navMenu.classList.contains("nav-menu--open");
        navToggle.setAttribute("aria-expanded", menuIsOpen);
    });
}

/* ========================================
   CERRAR MENU AL VOLVER A LA PAGINA
======================================== */
window.addEventListener("pageshow", closeMenu);

/* ========================================
   MOSTRAR Y OCULTAR NAVBAR CON SCROLL
======================================== */
let lastScrollPosition = window.scrollY;

window.addEventListener("scroll", function () {
    if (!siteHeader) return;
    const currentScrollPosition = window.scrollY;

    /* Cerca del inicio, el menu siempre esta visible */
    if (currentScrollPosition <= 80) {
        siteHeader.classList.remove("site-header--hidden");
    }
    /* Si el usuario baja, ocultamos el menu */
    else if (currentScrollPosition > lastScrollPosition) {
        siteHeader.classList.add("site-header--hidden");
    }
    /* Si el usuario sube, mostramos el menu */
    else {
        siteHeader.classList.remove("site-header--hidden");
    }

    lastScrollPosition = currentScrollPosition;
});

/* ========================================
   ESTADO DE SESION: MOSTRAR ROBOGEST
======================================== */
(function () {
    function actualizarBotonAcceso() {
        const token = sessionStorage.getItem("robogest_jwt_token") || localStorage.getItem("robogest_jwt_token");
        if (token) {
            const botones = document.querySelectorAll(".nav-login");
            botones.forEach(function (btn) {
                btn.textContent = "RoboGest";
                btn.setAttribute("href", "dashboard.html");
                btn.setAttribute("title", "Ir al panel de gestion RoboGest");
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", actualizarBotonAcceso);
    } else {
        actualizarBotonAcceso();
    }
})();