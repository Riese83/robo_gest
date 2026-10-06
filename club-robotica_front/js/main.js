// ========================================
// HERO - CONTROLES DEL SLIDER
// ========================================

// Buscamos todas las imágenes del Hero
const heroSlides = document.querySelectorAll(".hero-slide");

// Buscamos todos los indicadores
const heroIndicators = document.querySelectorAll(".hero-indicator");

// Buscamos los botones anterior y siguiente
const previousButton = document.querySelector(".hero-arrow--previous");

const nextButton = document.querySelector(".hero-arrow--next");


// Solo ejecutamos el código del Hero si sus elementos existen
if (
    heroSlides.length > 0 &&
    heroIndicators.length > 0 &&
    previousButton &&
    nextButton
) {

    // Guardamos cuál es la imagen actual
    let currentSlide = 0;

    // Guardaremos aquí el temporizador automático
    let slideInterval;


    // ========================================
    // FUNCIÓN PARA MOSTRAR UNA IMAGEN
    // ========================================

    function showSlide(index) {

        // Si llegamos después de la última imagen,
        // volvemos a la primera
        if (index >= heroSlides.length) {

            currentSlide = 0;

        }

        // Si intentamos ir antes de la primera imagen,
        // vamos a la última
        else if (index < 0) {

            currentSlide = heroSlides.length - 1;

        }

        // En cualquier otro caso,
        // usamos el índice recibido
        else {

            currentSlide = index;

        }


        // Quitamos la clase activa de todas las imágenes
        heroSlides.forEach(function (slide) {

            slide.classList.remove("hero-slide--active");

        });


        // Quitamos la clase activa de todos los indicadores
        heroIndicators.forEach(function (indicator) {

            indicator.classList.remove("hero-indicator--active");

        });


        // Activamos la imagen actual
        heroSlides[currentSlide].classList.add(
            "hero-slide--active"
        );


        // Activamos el indicador correspondiente
        heroIndicators[currentSlide].classList.add(
            "hero-indicator--active"
        );

    }


    // ========================================
    // FUNCIÓN PARA INICIAR EL CAMBIO AUTOMÁTICO
    // ========================================

    function startAutoSlide() {

        slideInterval = setInterval(function () {

            showSlide(currentSlide + 1);

        }, 5000);

    }


    // ========================================
    // FUNCIÓN PARA REINICIAR EL TEMPORIZADOR
    // ========================================

    function resetAutoSlide() {

        // Detenemos el temporizador actual
        clearInterval(slideInterval);

        // Creamos un nuevo temporizador
        startAutoSlide();

    }


    // ========================================
    // BOTÓN SIGUIENTE
    // ========================================

    nextButton.addEventListener("click", function () {

        showSlide(currentSlide + 1);

        resetAutoSlide();

    });


    // ========================================
    // BOTÓN ANTERIOR
    // ========================================

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
    // INICIAMOS EL CAMBIO AUTOMÁTICO
    // ========================================

    startAutoSlide();

}


// ========================================
// RECORRIDO DE LÍNEA DE PASOS
// ========================================

const learningProcess = document.querySelector(
    ".learning-process"
);


// Comprobamos si la sección existe
if (learningProcess) {

    const observer = new IntersectionObserver(
        function (entries) {

            entries.forEach(function (entry) {

                // Cuando la sección entra en la pantalla
                if (entry.isIntersecting) {

                    // Activamos la animación
                    learningProcess.classList.add(
                        "learning-process--active"
                    );

                    // Dejamos de observar la sección
                    // para que la animación ocurra una sola vez
                    observer.unobserve(learningProcess);

                }

            });

        },
        {
            threshold: 0.35
        }
    );


    // Comenzamos a observar la sección
    observer.observe(learningProcess);

}


// ========================================
// TRANSICIÓN DE MÁS CATEGORÍAS
// ========================================

const comingSoon = document.querySelector(
    ".coming-soon"
);


// Solo ejecutamos esta lógica si la sección existe
if (comingSoon) {

    // Buscamos el reloj de arena
    const comingSoonHourglass = comingSoon.querySelector(
        ".coming-soon__hourglass"
    );

    // Buscamos el título
    const comingSoonTitle = comingSoon.querySelector(
        ".section-label"
    );

    // Buscamos la descripción
    const comingSoonDescription = comingSoon.querySelector(
        "p:not(.section-label)"
    );


    // ========================================
    // FUNCIÓN PARA LIMITAR UN VALOR
    // ========================================

    function clamp(value, min, max) {

        return Math.min(
            Math.max(value, min),
            max
        );

    }


    // ========================================
    // FUNCIÓN PARA ACTUALIZAR LA TRANSICIÓN
    // ========================================

    function updateComingSoon() {

        // Posición actual de la sección
        const sectionRect = comingSoon.getBoundingClientRect();

        // Altura visible de la ventana
        const viewportHeight = window.innerHeight;


        // ========================================
        // PROGRESO GENERAL DEL SCROLL
        // ========================================

        // La animación comienza cuando la sección
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
        // TÍTULO
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
        // DESCRIPCIÓN
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
    // OPTIMIZACIÓN DEL SCROLL
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


    // Ejecutamos una primera actualización
    updateComingSoon();

}