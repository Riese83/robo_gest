// ========================================
// HERO - CONTROLES DEL SLIDER
// ========================================

// Buscamos todas las imagenes del Hero
const heroSlides = document.querySelectorAll(".hero-slide");

// Buscamos todos los indicadores
const heroIndicators = document.querySelectorAll(".hero-indicator");

// Buscamos los botones anterior y siguiente
const previousButton = document.querySelector(".hero-arrow--previous");

const nextButton = document.querySelector(".hero-arrow--next");


// Solo ejecutamos el codigo del Hero si sus elementos existen
if (
    heroSlides.length > 0 &&
    heroIndicators.length > 0 &&
    previousButton &&
    nextButton
) {

    const heroSection = document.querySelector(".hero");

    // Guardamos cual es la imagen actual
    let currentSlide = 0;

    // Guardaremos aqui el temporizador automatico
    let slideInterval = null;

    // ========================================
    // FUNCION PARA MOSTRAR UNA IMAGEN
    // ========================================

    function showSlide(index) {

        // Si llegamos despues de la ultima imagen, volvemos a la primera
        if (index >= heroSlides.length) {
            currentSlide = 0;
        }
        // Si intentamos ir antes de la primera imagen, vamos a la ultima
        else if (index < 0) {
            currentSlide = heroSlides.length - 1;
        }
        // En cualquier otro caso, usamos el indice recibido
        else {
            currentSlide = index;
        }

        // Actualizamos las imagenes
        heroSlides.forEach(function (slide, i) {
            if (i === currentSlide) {
                slide.classList.add("hero-slide--active");
                slide.setAttribute("aria-hidden", "false");
            } else {
                slide.classList.remove("hero-slide--active");
                slide.setAttribute("aria-hidden", "true");
            }
        });

        // Actualizamos los indicadores
        heroIndicators.forEach(function (indicator, i) {
            if (i === currentSlide) {
                indicator.classList.add("hero-indicator--active");
                indicator.setAttribute("aria-current", "true");
            } else {
                indicator.classList.remove("hero-indicator--active");
                indicator.removeAttribute("aria-current");
            }
        });

    }


    // ========================================
    // CONTROL DEL TEMPORIZADOR AUTOMATICO
    // ========================================

    function startAutoSlide() {
        if (slideInterval) return;
        slideInterval = setInterval(function () {
            showSlide(currentSlide + 1);
        }, 5000);
    }

    function pauseAutoSlide() {
        if (slideInterval) {
            clearInterval(slideInterval);
            slideInterval = null;
        }
    }

    function resetAutoSlide() {
        pauseAutoSlide();
        startAutoSlide();
    }


    // ========================================
    // BOTON SIGUIENTE Y ANTERIOR
    // ========================================

    nextButton.addEventListener("click", function () {
        showSlide(currentSlide + 1);
        resetAutoSlide();
    });

    previousButton.addEventListener("click", function () {
        showSlide(currentSlide - 1);
        resetAutoSlide();
    });


    // ========================================
    // INDICADORES
    // ========================================

    heroIndicators.forEach(function (indicator, index) {
        indicator.addEventListener("click", function () {
            showSlide(index);
            resetAutoSlide();
        });
    });


    // ========================================
    // ACCESIBILIDAD: PAUSA EN HOVER Y EN FOCO
    // ========================================

    if (heroSection) {
        // Pausar cuando el cursor esta sobre el slider
        heroSection.addEventListener("mouseenter", pauseAutoSlide);
        heroSection.addEventListener("mouseleave", startAutoSlide);

        // Pausar cuando un elemento interno recibe foco (navegacion por teclado)
        heroSection.addEventListener("focusin", pauseAutoSlide);
        heroSection.addEventListener("focusout", startAutoSlide);

        // Navegacion con flechas del teclado
        heroSection.addEventListener("keydown", function (e) {
            if (e.key === "ArrowLeft") {
                e.preventDefault();
                showSlide(currentSlide - 1);
                resetAutoSlide();
            } else if (e.key === "ArrowRight") {
                e.preventDefault();
                showSlide(currentSlide + 1);
                resetAutoSlide();
            }
        });

        // ========================================
        // GESTOS TACTILES (TOUCH SWIPE) EN MOVILES
        // ========================================

        let touchStartX = 0;
        let touchStartY = 0;

        heroSection.addEventListener("touchstart", function (e) {
            if (e.changedTouches && e.changedTouches.length > 0) {
                touchStartX = e.changedTouches[0].clientX;
                touchStartY = e.changedTouches[0].clientY;
                pauseAutoSlide();
            }
        }, { passive: true });

        heroSection.addEventListener("touchend", function (e) {
            if (e.changedTouches && e.changedTouches.length > 0) {
                const touchEndX = e.changedTouches[0].clientX;
                const touchEndY = e.changedTouches[0].clientY;

                const deltaX = touchEndX - touchStartX;
                const deltaY = touchEndY - touchStartY;

                // Solo detectamos swipe si el desplazamiento horizontal es significativo y mayor al vertical
                if (Math.abs(deltaX) > 40 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    if (deltaX < 0) {
                        // Deslizar hacia la izquierda -> siguiente
                        showSlide(currentSlide + 1);
                    } else {
                        // Deslizar hacia la derecha -> anterior
                        showSlide(currentSlide - 1);
                    }
                }

                resetAutoSlide();
            }
        }, { passive: true });
    }


    // ========================================
    // INICIAMOS EL CAMBIO AUTOMATICO
    // ========================================

    startAutoSlide();

}


// ========================================
// RECORRIDO DE LINEA DE PASOS
// ========================================

const learningProcess = document.querySelector(
    ".learning-process"
);


// Comprobamos si la seccion existe
if (learningProcess) {

    const observer = new IntersectionObserver(
        function (entries) {

            entries.forEach(function (entry) {

                // Cuando la seccion entra en la pantalla
                if (entry.isIntersecting) {

                    // Activamos la animacion
                    learningProcess.classList.add(
                        "learning-process--active"
                    );

                    // Dejamos de observar la seccion
                    // para que la animacion ocurra una sola vez
                    observer.unobserve(learningProcess);

                }

            });

        },
        {
            threshold: 0.35
        }
    );


    // Comenzamos a observar la seccion
    observer.observe(learningProcess);

}


// ========================================
// TRANSICION DE MAS CATEGORIAS
// ========================================

const comingSoon = document.querySelector(
    ".coming-soon"
);


// Solo ejecutamos esta logica si la seccion existe
if (comingSoon) {

    // Buscamos el reloj de arena
    const comingSoonHourglass = comingSoon.querySelector(
        ".coming-soon__hourglass"
    );

    // Buscamos el titulo
    const comingSoonTitle = comingSoon.querySelector(
        ".section-label"
    );

    // Buscamos la descripcion
    const comingSoonDescription = comingSoon.querySelector(
        "p:not(.section-label)"
    );


    // ========================================
    // FUNCION PARA LIMITAR UN VALOR
    // ========================================

    function clamp(value, min, max) {

        return Math.min(
            Math.max(value, min),
            max
        );

    }


    // ========================================
    // FUNCION PARA ACTUALIZAR LA TRANSICION
    // ========================================

    function updateComingSoon() {

        // Posicion actual de la seccion
        const sectionRect = comingSoon.getBoundingClientRect();

        // Altura visible de la ventana
        const viewportHeight = window.innerHeight;


        // ========================================
        // PROGRESO GENERAL DEL SCROLL
        // ========================================

        // La animacion comienza cuando la seccion
        // empieza a entrar por la parte inferior
        const revealStart = viewportHeight * 0.25;

        const revealDistance = viewportHeight * 0.65;

        const progress = clamp(
            (
                viewportHeight - sectionRect.top - revealStart
            ) / revealDistance,
            0,
            1
        );


        // ========================================
        // RELOJ DE ARENA
        // ========================================

        if (comingSoonHourglass) {

            const hourglassProgress = clamp(
                progress / 0.35,
                0,
                1
            );

            comingSoonHourglass.style.opacity =
                0.3 + (0.7 * hourglassProgress);

            comingSoonHourglass.style.transform =
                `translateY(${
                    80 * (1 - hourglassProgress)
                }px)`;

        }


        // ========================================
        // TITULO
        // ========================================

        if (comingSoonTitle) {

            const titleProgress = clamp(
                (progress - 0.20) / 0.40,
                0,
                1
            );

            comingSoonTitle.style.opacity =
                0.3 + (0.7 * titleProgress);

            comingSoonTitle.style.transform =
                `translateY(${
                    80 * (1 - titleProgress)
                }px)`;

        }


        // ========================================
        // DESCRIPCION
        // ========================================

        if (comingSoonDescription) {

            const descriptionProgress = clamp(
                (progress - 0.35) / 0.35,
                0,
                1
            );

            comingSoonDescription.style.opacity =
                descriptionProgress;

            comingSoonDescription.style.transform =
                `translateY(${
                    80 * (1 - descriptionProgress)
                }px)`;

        }

    }


    // ========================================
    // OPTIMIZACION DEL SCROLL
    // ========================================

    let isUpdating = false;


    function requestComingSoonUpdate() {

        if (isUpdating) {

            return;

        }

        isUpdating = true;


        window.requestAnimationFrame(function () {

            updateComingSoon();

            isUpdating = false;

        });

    }


    // ========================================
    // EVENTOS
    // ========================================

    window.addEventListener(
        "scroll",
        requestComingSoonUpdate,
        {
            passive: true
        }
    );


    window.addEventListener(
        "resize",
        requestComingSoonUpdate
    );


    // Ejecutamos una primera actualizacion
    updateComingSoon();

}