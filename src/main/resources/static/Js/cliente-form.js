// ===================== FORMULARIO DE CLIENTE + MAPA =====================
// Mapa: Leaflet + OpenStreetMap. Geocodificación: Nominatim (gratuito, sin API key).
document.addEventListener('DOMContentLoaded', function () {
    const $ = (id) => document.getElementById(id);
    const form = $('clienteForm');
    if (!form) return;

    // ---------------- Validación en vivo ----------------
    const nombre = $('nombre');
    const telefono = $('telefono');
    const email = $('email');
    const avatar = $('avatarPreview');
    const EMAIL_RE = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    const validar = {
        nombre: () => nombre.value.trim().length >= 3,
        telefono: () => /^[0-9]{9,15}$/.test(telefono.value.trim()),
        email: () => !email.value.trim() || EMAIL_RE.test(email.value.trim())
    };
    const marcar = (el, ok) => {
        el.classList.toggle('is-invalid', !ok);
        el.classList.toggle('is-valid', ok && el.value.trim() !== '');
    };

    telefono.addEventListener('input', () => {
        telefono.value = telefono.value.replace(/\D/g, '').slice(0, 15);
        if (telefono.classList.contains('is-invalid') || telefono.value.length >= 9) marcar(telefono, validar.telefono());
    });
    nombre.addEventListener('input', () => {
        if (avatar) avatar.textContent = nombre.value.trim() ? nombre.value.trim()[0].toUpperCase() : '+';
        if (nombre.classList.contains('is-invalid')) marcar(nombre, validar.nombre());
    });
    nombre.addEventListener('blur', () => marcar(nombre, validar.nombre()));
    telefono.addEventListener('blur', () => marcar(telefono, validar.telefono()));
    email.addEventListener('blur', () => marcar(email, validar.email()));

    form.addEventListener('submit', function (e) {
        const okN = validar.nombre(), okT = validar.telefono(), okE = validar.email();
        marcar(nombre, okN); marcar(telefono, okT); marcar(email, okE);
        if (!(okN && okT && okE)) {
            e.preventDefault();
            (!okN ? nombre : !okT ? telefono : email).focus();
            return;
        }
        const btn = $('btnGuardarCliente');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Guardando...';
    });

    // ---------------- Mapa ----------------
    const direccion = $('direccion');
    const lat = $('latitud');
    const lng = $('longitud');
    const buscar = $('buscarDireccion');
    const sugerencias = $('sugerencias');
    const cargando = $('mapaCargando');

    if (typeof L === 'undefined') {
        const wrap = document.querySelector('.cf-map-wrap');
        if (wrap) wrap.innerHTML = '<div class="cf-map-error"><i class="fas fa-map"></i> No se pudo cargar el mapa. Escriba la dirección manualmente.</div>';
        return;
    }

    const NOMINATIM = 'https://nominatim.openstreetmap.org';
    const CENTRO = [-5.1945, -80.6328]; // Piura (sede de la clínica)
    const tieneCoords = lat.value && lng.value;
    const inicio = tieneCoords ? [parseFloat(lat.value), parseFloat(lng.value)] : CENTRO;

    const map = L.map('mapaCliente', { scrollWheelZoom: false }).setView(inicio, tieneCoords ? 17 : 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);
    map.on('focus', () => map.scrollWheelZoom.enable());
    map.on('blur', () => map.scrollWheelZoom.disable());

    const icono = L.divIcon({
        className: 'cf-marker',
        html: '<i class="fas fa-map-marker-alt"></i>',
        iconSize: [36, 36],
        iconAnchor: [18, 34]
    });
    let marker = null;

    function ponerMarcador(latlng) {
        if (!marker) {
            marker = L.marker(latlng, { draggable: true, icon: icono }).addTo(map);
            marker.on('dragend', () => ubicar(marker.getLatLng(), true));
        } else {
            marker.setLatLng(latlng);
        }
        lat.value = Number(latlng.lat).toFixed(7);
        lng.value = Number(latlng.lng).toFixed(7);
    }
    if (tieneCoords) ponerMarcador({ lat: inicio[0], lng: inicio[1] });

    /** Convierte la respuesta de Nominatim en una dirección corta y legible. */
    function formatear(data) {
        const a = data.address || {};
        const via = a.road || a.pedestrian || a.footway || a.residential || '';
        const calle = [via, a.house_number].filter(Boolean).join(' ');
        const zona = a.neighbourhood || a.suburb || a.quarter || a.hamlet || '';
        const ciudad = a.city || a.town || a.village || a.county || '';
        const region = a.state && a.state !== ciudad ? a.state : '';
        const partes = [calle, zona, ciudad, region].filter(Boolean);
        // Sin coincidencias útiles: se usa el nombre completo
        return (partes.length ? partes.join(', ') : (data.display_name || '')).slice(0, 200);
    }

    let ultimaPeticion = 0;
    async function reverse(latlng) {
        // Nominatim permite 1 petición por segundo
        const espera = 1000 - (Date.now() - ultimaPeticion);
        if (espera > 0) await new Promise(r => setTimeout(r, espera));
        ultimaPeticion = Date.now();
        const url = `${NOMINATIM}/reverse?format=jsonv2&addressdetails=1&accept-language=es&lat=${latlng.lat}&lon=${latlng.lng}`;
        const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!res.ok) throw new Error('reverse ' + res.status);
        return res.json();
    }

    /** Coloca el marcador y actualiza automáticamente el campo de dirección. */
    async function ubicar(latlng, actualizarDireccion) {
        ponerMarcador(latlng);
        if (!actualizarDireccion) return;
        cargando.classList.remove('d-none');
        try {
            const data = await reverse(latlng);
            const texto = formatear(data);
            if (texto) {
                direccion.value = texto;
                direccion.classList.add('cf-flash');
                setTimeout(() => direccion.classList.remove('cf-flash'), 900);
            }
        } catch (e) {
            console.warn('No se pudo obtener la dirección:', e);
        } finally {
            cargando.classList.add('d-none');
        }
    }

    map.on('click', (e) => ubicar(e.latlng, true));

    // ---------------- Autocompletado de búsqueda ----------------
    let timer = null;
    let controller = null;

    function ocultarSugerencias() {
        sugerencias.classList.add('d-none');
        sugerencias.innerHTML = '';
    }

    function mostrarSugerencias(items) {
        if (!items.length) {
            sugerencias.innerHTML = '<li class="cf-sug-empty">Sin resultados. Pruebe con otra referencia o marque en el mapa.</li>';
            sugerencias.classList.remove('d-none');
            return;
        }
        sugerencias.innerHTML = '';
        items.forEach(item => {
            const li = document.createElement('li');
            li.className = 'cf-sug-item';
            const titulo = document.createElement('div');
            titulo.className = 'fw-semibold';
            titulo.textContent = formatear(item);
            const sub = document.createElement('small');
            sub.className = 'text-muted';
            sub.textContent = item.display_name;
            li.append(titulo, sub);
            li.addEventListener('mousedown', (e) => {
                e.preventDefault();
                const latlng = { lat: parseFloat(item.lat), lng: parseFloat(item.lon) };
                map.setView(latlng, 17);
                ponerMarcador(latlng);
                direccion.value = formatear(item);
                buscar.value = '';
                ocultarSugerencias();
            });
            sugerencias.appendChild(li);
        });
        sugerencias.classList.remove('d-none');
    }

    buscar.addEventListener('input', function () {
        clearTimeout(timer);
        const q = buscar.value.trim();
        if (q.length < 4) { ocultarSugerencias(); return; }
        timer = setTimeout(async () => {
            if (controller) controller.abort();
            controller = new AbortController();
            sugerencias.innerHTML = '<li class="cf-sug-empty"><span class="spinner-border spinner-border-sm me-2"></span>Buscando...</li>';
            sugerencias.classList.remove('d-none');
            try {
                ultimaPeticion = Date.now();
                const c = map.getCenter();
                const url = `${NOMINATIM}/search?format=jsonv2&addressdetails=1&limit=6&countrycodes=pe&accept-language=es`
                    + `&viewbox=${c.lng - 1},${c.lat + 1},${c.lng + 1},${c.lat - 1}&q=${encodeURIComponent(q)}`;
                const res = await fetch(url, { signal: controller.signal, headers: { 'Accept': 'application/json' } });
                mostrarSugerencias(res.ok ? await res.json() : []);
            } catch (e) {
                if (e.name !== 'AbortError') mostrarSugerencias([]);
            }
        }, 700);
    });
    buscar.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') { // no enviar el formulario al buscar
            e.preventDefault();
            const primero = sugerencias.querySelector('.cf-sug-item');
            if (primero) primero.dispatchEvent(new MouseEvent('mousedown'));
        }
        if (e.key === 'Escape') ocultarSugerencias();
    });
    buscar.addEventListener('blur', () => setTimeout(ocultarSugerencias, 150));

    // ---------------- Mi ubicación ----------------
    $('btnMiUbicacion').addEventListener('click', function () {
        if (!navigator.geolocation) {
            if (window.Swal) Swal.fire({ icon: 'info', title: 'No disponible', text: 'Su navegador no permite obtener la ubicación.' });
            return;
        }
        const btn = this;
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
        navigator.geolocation.getCurrentPosition(pos => {
            const latlng = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            map.setView(latlng, 17);
            ubicar(latlng, true);
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-location-crosshairs"></i>';
        }, () => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-location-crosshairs"></i>';
            if (window.Swal) Swal.fire({ icon: 'warning', title: 'Ubicación no disponible', text: 'Permita el acceso a la ubicación o marque el punto en el mapa.' });
        }, { enableHighAccuracy: true, timeout: 10000 });
    });

    // ---------------- Limpiar ----------------
    $('btnLimpiarDireccion').addEventListener('click', () => {
        direccion.value = '';
        lat.value = '';
        lng.value = '';
        if (marker) { map.removeLayer(marker); marker = null; }
        direccion.focus();
    });

    // Si se edita un cliente con dirección pero sin coordenadas, se intenta ubicarla en el mapa
    if (!tieneCoords && direccion.value.trim().length > 5) {
        fetch(`${NOMINATIM}/search?format=jsonv2&limit=1&countrycodes=pe&q=${encodeURIComponent(direccion.value)}`)
            .then(r => r.ok ? r.json() : [])
            .then(res => {
                if (res && res[0]) {
                    const latlng = { lat: parseFloat(res[0].lat), lng: parseFloat(res[0].lon) };
                    map.setView(latlng, 16);
                    ponerMarcador(latlng);
                }
            })
            .catch(() => { });
    }

    // El mapa se dibuja dentro de un contenedor con animación: recalcular tamaño
    setTimeout(() => map.invalidateSize(), 300);
});
