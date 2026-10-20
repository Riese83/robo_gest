/**
 * Gestión del formulario de contacto y consultas hacia fpune.clubderobotica@gmail.com
 * Envío de consultas mediante FormSubmit (Opción C2).
 */
document.addEventListener('DOMContentLoaded', () => {
    const contactForm = document.getElementById('contactForm');
    const statusDiv = document.getElementById('contactFormStatus');
    const submitBtn = document.getElementById('btnSubmitContact');
    const submitText = document.getElementById('btnSubmitText');

    if (!contactForm) return;

    contactForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        // Ocultar feedback previo
        if (statusDiv) {
            statusDiv.style.display = 'none';
            statusDiv.className = 'contact-form__status';
            statusDiv.innerHTML = '';
        }

        const name = (document.getElementById('full-name')?.value || '').trim();
        const phone = (document.getElementById('phone')?.value || '').trim();
        const email = (document.getElementById('email')?.value || '').trim();
        const message = (document.getElementById('message')?.value || '').trim();

        if (!name || !email || !message) {
            showStatus('Por favor complete todos los campos requeridos (Nombre, Email y Consulta).', 'error');
            return;
        }

        // Estado visual de carga
        if (submitBtn) submitBtn.disabled = true;
        if (submitText) submitText.textContent = 'Enviando...';

        const formData = new FormData(contactForm);

        try {
            const formAction = contactForm.action;
            const response = await fetch(formAction, {
                method: 'POST',
                body: formData,
                headers: {
                    'Accept': 'application/json'
                }
            });

            const data = await response.json().catch(() => ({}));

            if (response.ok && (data.success === 'true' || data.success === true)) {
                showStatus(
                    '<strong>¡Consulta enviada con éxito!</strong><br>' +
                    'Muchas gracias por escribirnos, ' + escapeHtml(name) + '. ' +
                    'Tu mensaje fue entregado a <strong>fpune.clubderobotica@gmail.com</strong> y te responderemos a la brevedad a tu correo <strong>' + escapeHtml(email) + '</strong>.',
                    'success'
                );
                contactForm.reset();
            } else if (data.message && data.message.includes('Activation')) {
                showActivationStatus(name, phone, email, message);
            } else {
                let errorMsg = data.message || 'No se pudo completar el envío automático por correo en este momento.';
                showFallbackStatus(name, phone, email, message, errorMsg);
            }
        } catch (error) {
            console.warn('Fallo en la conexión con el servicio de correo:', error);
            showFallbackStatus(name, phone, email, message, 'No se pudo conectar con el servidor de correo.');
        } finally {
            if (submitBtn) submitBtn.disabled = false;
            if (submitText) submitText.textContent = 'Enviar mensaje';
        }
    });

    function showStatus(html, type) {
        if (!statusDiv) return;
        statusDiv.innerHTML = html;
        statusDiv.className = `contact-form__status contact-form__status--${type}`;
        statusDiv.style.display = 'block';
        statusDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function showActivationStatus(name, phone, email, message) {
        const mailSubject = encodeURIComponent(`Consulta de ${name} - Club de Robótica FP-UNE`);
        const mailBody = encodeURIComponent(
            `Nombre: ${name}\n` +
            `Email: ${email}\n` +
            `Teléfono: ${phone}\n\n` +
            `Consulta:\n${message}`
        );
        const mailUrl = `mailto:fpune.clubderobotica@gmail.com?subject=${mailSubject}&body=${mailBody}`;

        const activationHtml = `
            <strong>¡Casi listo! Se envió un correo de activación a fpune.clubderobotica@gmail.com</strong><br>
            El servicio FormSubmit envió un enlace ('Activate Form') a la casilla oficial del club para autorizar la recepción de mensajes (solo se hace una vez).<br>
            <div style="margin-top: 0.75rem;">
                <a href="${mailUrl}" class="btn-fallback btn-fallback-mail">
                    Enviar directamente por correo
                </a>
            </div>
        `;
        showStatus(activationHtml, 'error');
    }

    function showFallbackStatus(name, phone, email, message, reason) {
        const mailSubject = encodeURIComponent(`Consulta de ${name} - Club de Robótica FP-UNE`);
        const mailBody = encodeURIComponent(
            `Nombre: ${name}\n` +
            `Email: ${email}\n` +
            `Teléfono: ${phone}\n\n` +
            `Consulta:\n${message}`
        );
        const mailUrl = `mailto:fpune.clubderobotica@gmail.com?subject=${mailSubject}&body=${mailBody}`;

        const fallbackHtml = `
            <strong>${escapeHtml(reason)}</strong><br>
            Puedes enviarnos tu consulta directamente por correo electrónico:
            <div style="margin-top: 0.75rem;">
                <a href="${mailUrl}" class="btn-fallback btn-fallback-mail">
                    Enviar a fpune.clubderobotica@gmail.com
                </a>
            </div>
        `;
        showStatus(fallbackHtml, 'error');
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
