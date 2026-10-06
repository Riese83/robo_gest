const navToggle = document.querySelector(".nav-toggle");

const navMenu = document.querySelector(".nav-menu");

const siteHeader = document.querySelector(".site-header");


/* ========================================
   CERRAR MENÚ MÓVIL
======================================== */

function closeMenu() {
    navMenu.classList.remove("nav-menu--open");

    navToggle.classList.remove("nav-toggle--open");

    navToggle.setAttribute("aria-expanded", "false");
}


/* ========================================
   ABRIR Y CERRAR MENÚ MÓVIL
======================================== */

navToggle.addEventListener("click", function () {
    navMenu.classList.toggle("nav-menu--open");

    navToggle.classList.toggle("nav-toggle--open");

    const menuIsOpen =
        navMenu.classList.contains("nav-menu--open");

    navToggle.setAttribute("aria-expanded", menuIsOpen);
});


/* ========================================
   CERRAR MENÚ AL VOLVER A LA PÁGINA
======================================== */

window.addEventListener("pageshow", closeMenu);


/* ========================================
   MOSTRAR Y OCULTAR NAVBAR CON SCROLL
======================================== */

let lastScrollPosition = window.scrollY;

window.addEventListener("scroll", function () {
    const currentScrollPosition = window.scrollY;


    /* Cerca del inicio, el menú siempre está visible */

    if (currentScrollPosition <= 80) {
        siteHeader.classList.remove("site-header--hidden");
    }


    /* Si el usuario baja, ocultamos el menú */

    else if (currentScrollPosition > lastScrollPosition) {
        siteHeader.classList.add("site-header--hidden");
    }


    /* Si el usuario sube, mostramos el menú */

    else {
        siteHeader.classList.remove("site-header--hidden");
    }


    lastScrollPosition = currentScrollPosition;
});