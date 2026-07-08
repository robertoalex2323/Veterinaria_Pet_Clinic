(() => {
  document.addEventListener('DOMContentLoaded', () => {
    const metodo = document.getElementById('metodoPago');
    const qrMetodoPago = document.getElementById('qrMetodoPago');
    const qrImg = document.getElementById('qrImg');

    if (!metodo || !qrMetodoPago || !qrImg) return;

    const getQr = (value) => {
      if (value === 'Yape') return '/Imagen/Iconos/qr-yape.png';
      if (value === 'Plin') return '/Imagen/Iconos/qr-plin.png';
      return null;
    };

    const render = () => {
      const qr = getQr(metodo.value);
      if (!qr) {
        qrMetodoPago.style.display = 'none';
        qrImg.src = '';
        return;
      }
      qrMetodoPago.style.display = 'block';
      qrImg.src = qr;
    };

    metodo.addEventListener('change', render);
    render();
  });
})();

