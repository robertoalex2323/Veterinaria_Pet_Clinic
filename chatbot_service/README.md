# Microservicio Chatbot Pet Clinic

Microservicio independiente en Flask para el widget flotante de Veterinaria Pet Clinic. Usa Gemini API para generar respuestas naturales y conserva memoria temporal por sesion.

## Que incluye

- `POST /chat`: recibe mensajes JSON y responde con Gemini.
- `POST /reset`: limpia el historial temporal de una sesion.
- `GET /health`: confirma estado del servicio y si Gemini esta activo.
- Memoria temporal en RAM con los ultimos mensajes por `session_id`.
- Deteccion de intencion usando el mensaje actual y el contexto reciente.
- Fallback local si falta la API key o Gemini falla.

## Configuracion

1. Crea una API key en Google AI Studio:
   https://aistudio.google.com/apikey

2. Copia el archivo de ejemplo:

```powershell
copy .env.example .env
```

3. Edita `.env`:

```env
GEMINI_API_KEY=tu_api_key_de_gemini
GEMINI_MODEL=gemini-2.0-flash
GEMINI_TEMPERATURE=0.8
GEMINI_MAX_OUTPUT_TOKENS=220
CHATBOT_HISTORY_LIMIT=10
CHATBOT_ALLOWED_ORIGINS=*
```

`CHATBOT_HISTORY_LIMIT=10` guarda los ultimos 10 mensajes de la sesion, contando mensajes del usuario y respuestas del bot. Puedes usar `CHATBOT_HISTORY_LIMIT=libre` o `0` para no limitar el historial en memoria mientras el servicio siga encendido.

## Ejecutar rapido

En PowerShell:

```powershell
cd C:\Users\Administrador\Documents\GitHub\Veterinaria_Pet_Clinic\chatbot_service
.\run_chatbot.ps1
```

Si PowerShell bloquea scripts, usa:

```powershell
cd C:\Users\Administrador\Documents\GitHub\Veterinaria_Pet_Clinic\chatbot_service
.\run_chatbot.bat
```

## Ejecutar manualmente en Visual Studio Code

Abre una terminal en VS Code y ejecuta:

```powershell
cd C:\Users\Administrador\Documents\GitHub\Veterinaria_Pet_Clinic\chatbot_service
py -3 -m pip install -r requirements.txt
py -3 app.py
```

Debe aparecer:

```text
Running on http://127.0.0.1:5000
```

Deja esa terminal abierta mientras usas el dashboard de Spring Boot.

## Probar

```powershell
Invoke-RestMethod http://localhost:5000/health
```

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:5000/chat `
  -ContentType "application/json" `
  -Body '{"session_id":"demo","message":"Brayan perro manana tarde"}'
```

## Contrato JSON

Entrada:

```json
{
  "session_id": "web-123",
  "message": "Mi perro no quiere comer"
}
```

Salida:

```json
{
  "response": "Respuesta natural para mostrar en el widget",
  "intent": "symptoms",
  "session_id": "web-123",
  "history_size": 2,
  "provider": "gemini"
}
```
