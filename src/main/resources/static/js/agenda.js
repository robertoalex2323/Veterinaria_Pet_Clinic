document.addEventListener('DOMContentLoaded', () => {
    const calendarGrid = document.getElementById('calendarGrid');
    const monthYearDisplay = document.getElementById('monthYearDisplay');
    const prevMonthBtn = document.getElementById('prevMonth');
    const nextMonthBtn = document.getElementById('nextMonth');
    const selectedDateInput = document.getElementById('selectedDate');

    let currentDate = new Date(selectedDateInput.value + 'T12:00:00');
    if (isNaN(currentDate.getTime())) {
        currentDate = new Date();
    }

    let currentMonth = currentDate.getMonth();
    let currentYear = currentDate.getFullYear();
    const selectedIsoDate = selectedDateInput.value;

    function renderCalendar(month, year) {
        calendarGrid.innerHTML = '';

        const firstDay = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        const monthNames = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
        monthYearDisplay.textContent = `${monthNames[month]} ${year}`;

        // Empty slots before 1st day
        for (let i = 0; i < firstDay; i++) {
            const emptyDiv = document.createElement('div');
            emptyDiv.classList.add('calendar-day', 'empty');
            calendarGrid.appendChild(emptyDiv);
        }

        const today = new Date();

        for (let i = 1; i <= daysInMonth; i++) {
            const dayDiv = document.createElement('div');
            dayDiv.classList.add('calendar-day');
            dayDiv.textContent = i;

            const isoDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;

            if (isoDate === selectedIsoDate) {
                dayDiv.classList.add('selected');
            }

            if (i === today.getDate() && month === today.getMonth() && year === today.getFullYear()) {
                dayDiv.classList.add('today');
            }

            dayDiv.addEventListener('click', () => {
                window.location.href = `/recepcionista/agenda?fecha=${isoDate}`;
            });

            calendarGrid.appendChild(dayDiv);
        }
    }

    if (prevMonthBtn && nextMonthBtn) {
        prevMonthBtn.addEventListener('click', () => {
            currentMonth--;
            if (currentMonth < 0) {
                currentMonth = 11;
                currentYear--;
            }
            renderCalendar(currentMonth, currentYear);
        });

        nextMonthBtn.addEventListener('click', () => {
            currentMonth++;
            if (currentMonth > 11) {
                currentMonth = 0;
                currentYear++;
            }
            renderCalendar(currentMonth, currentYear);
        });
    }

    renderCalendar(currentMonth, currentYear);

    // --- Inteligencia para el cálculo automático de duración ---
    const horaInicioInput = document.getElementById('horaInicio');
    const horaFinInput = document.getElementById('horaFin');
    const duracionInput = document.getElementById('duracionTurno');
    const formGenerar = document.querySelector('form[action*="/horarios/generar"]');

    if (horaInicioInput && horaFinInput && duracionInput) {
        const calcularDiferencia = () => {
            const inicio = horaInicioInput.value;
            const fin = horaFinInput.value;

            if (inicio && fin) {
                const [h1, m1] = inicio.split(':').map(Number);
                const [h2, m2] = fin.split(':').map(Number);

                const minutosInicio = (h1 * 60) + m1;
                const minutosFin = (h2 * 60) + m2;
                const diferencia = minutosFin - minutosInicio;

                const submitBtn = formGenerar ? formGenerar.querySelector('button[type="submit"]') : null;

                if (diferencia > 0) {
                    duracionInput.value = diferencia;
                    
                    // El sistema es flexible: el recepcionista decide la duración según la cita
                    // Solo validamos que la diferencia sea positiva
                    duracionInput.style.borderColor = "#ced4da";
                    if (submitBtn) submitBtn.disabled = false;
                    
                } else {
                    duracionInput.value = '';
                    duracionInput.style.borderColor = "#ced4da";
                    if (submitBtn) submitBtn.disabled = true;
                }
            }
        };

        horaInicioInput.addEventListener('change', calcularDiferencia);
        horaFinInput.addEventListener('change', calcularDiferencia);
    }
});