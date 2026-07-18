(function () {
    var CHAT_ENDPOINT = 'http://localhost:5000/chat';
    var SESSION_KEY = 'petClinicChatbotSessionId';
    var sessionId = window.localStorage.getItem(SESSION_KEY);

    if (!sessionId) {
        sessionId = 'web-' + Date.now() + '-' + Math.random().toString(16).slice(2);
        window.localStorage.setItem(SESSION_KEY, sessionId);
    }

    function createMessage(text, type) {
        var message = document.createElement('div');
        message.className = 'vet-chatbot__message vet-chatbot__message--' + type;
        message.textContent = text;
        return message;
    }

    function scrollToBottom(messages) {
        messages.scrollTop = messages.scrollHeight;
    }

    function createTypingMessage() {
        var message = document.createElement('div');
        message.className = 'vet-chatbot__message vet-chatbot__message--bot vet-chatbot__typing';
        message.innerHTML = '<span></span><span></span><span></span><em>Escribiendo...</em>';
        return message;
    }

    document.addEventListener('DOMContentLoaded', function () {
        var widget = document.querySelector('.vet-chatbot');

        if (!widget) {
            return;
        }

        var toggle = widget.querySelector('.vet-chatbot__toggle');
        var close = widget.querySelector('.vet-chatbot__close');
        var panel = widget.querySelector('.vet-chatbot__panel');
        var form = widget.querySelector('.vet-chatbot__form');
        var input = widget.querySelector('.vet-chatbot__input');
        var send = widget.querySelector('.vet-chatbot__send');
        var messages = widget.querySelector('.vet-chatbot__messages');

        function setOpen(isOpen) {
            widget.classList.toggle('is-open', isOpen);
            toggle.setAttribute('aria-expanded', String(isOpen));
            panel.setAttribute('aria-hidden', String(!isOpen));

            if (isOpen) {
                input.focus();
                scrollToBottom(messages);
            }
        }

        toggle.addEventListener('click', function () {
            setOpen(!widget.classList.contains('is-open'));
        });

        close.addEventListener('click', function () {
            setOpen(false);
        });

        form.addEventListener('submit', function (event) {
            event.preventDefault();

            var text = input.value.trim();

            if (!text) {
                return;
            }

            messages.appendChild(createMessage(text, 'user'));
            var typing = createTypingMessage();
            messages.appendChild(typing);
            input.value = '';
            input.disabled = true;
            send.disabled = true;
            scrollToBottom(messages);

            fetch(CHAT_ENDPOINT, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: text,
                    session_id: sessionId
                })
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('No se pudo contactar al asistente.');
                    }

                    return response.json();
                })
                .then(function (data) {
                    if (data.session_id) {
                        sessionId = data.session_id;
                        window.localStorage.setItem(SESSION_KEY, sessionId);
                    }

                    messages.appendChild(createMessage(data.response || 'No tengo una respuesta disponible en este momento.', 'bot'));
                })
                .catch(function () {
                    messages.appendChild(createMessage('Ahora no puedo responder desde el asistente. Intentalo nuevamente en unos segundos o comunicate directamente con recepcion.', 'bot vet-chatbot__message--error'));
                })
                .finally(function () {
                    if (typing.parentNode) {
                        typing.parentNode.removeChild(typing);
                    }

                    input.disabled = false;
                    send.disabled = false;
                    input.focus();
                    scrollToBottom(messages);
                });
        });
    });
})();
