document.getElementById("loginForm").addEventListener("submit", function(e) {
  e.preventDefault();

  const user = document.getElementById("user").value;
  const pass = document.getElementById("pass").value;
  const msg = document.getElementById("msg");

  if (user === "admin" && pass === "1234") {
    msg.style.color = "green";
    msg.textContent = "Login correcto ✔";
    alert("Bienvenido a Pet Clinic");
  } else {
    msg.style.color = "red";
    msg.textContent = "Usuario o contraseña incorrectos";
  }
});