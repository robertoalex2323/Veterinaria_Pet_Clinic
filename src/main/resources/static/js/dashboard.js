document.addEventListener('DOMContentLoaded', function() {
    const clearNotificationsBtn = document.getElementById('clearNotifications');

    if (clearNotificationsBtn) {
        clearNotificationsBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            localStorage.setItem('notificationHistory', '[]');
            window.location.reload(); 
        });
    };
});