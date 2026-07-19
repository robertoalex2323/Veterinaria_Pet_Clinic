import os
import json
import random
import re
import unicodedata
import urllib.error
import urllib.request
import uuid
from collections import defaultdict, deque
from datetime import datetime, timezone

from dotenv import load_dotenv
from flask import Flask, jsonify, request
from flask_cors import CORS


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(BASE_DIR, ".env"))

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": os.getenv("CHATBOT_ALLOWED_ORIGINS", "*")}})


SMILE = "\U0001F60A"
PAWS = "\U0001F43E"
DOG = "\U0001F436"
CAT = "\U0001F431"
SYRINGE = "\U0001F489"
BATH = "\U0001F6C1"
WARNING = "\u26A0\uFE0F"

SYSTEM_PROMPT = """
Eres el asistente virtual profesional de Veterinaria Pet Clinic.
Atiendes como una recepcionista veterinaria experta: natural, empatica, clara y contextual.
Tu objetivo es orientar, recopilar datos utiles y derivar a atencion veterinaria cuando corresponda.

Capacidades:
- Orientas sobre vacunas, desparasitacion, alimentacion, esterilizacion, banos, grooming, horarios, mascotas y citas.
- Si el usuario quiere una cita, recopilas nombre del cliente, mascota, fecha u horario y motivo de consulta.
- Si faltan datos, preguntas solo lo necesario.
- Usas el historial reciente y los datos recordados de la sesion para no pedir lo mismo dos veces.

Reglas clinicas:
- No inventes diagnosticos medicos definitivos.
- No indiques medicamentos, dosis ni tratamientos sin evaluacion veterinaria.
- En convulsiones, intoxicacion, sangrado, dificultad respiratoria, atropello, desmayo, dolor intenso o colapso, recomienda atencion veterinaria inmediata.

Estilo:
- Responde en espanol, con tono calido, profesional y breve.
- Suena humano, no robotico.
- Maximo 3 oraciones cortas, salvo que el usuario pida detalle.
- Usa emojis con moderacion.
- No digas que una cita ya quedo registrada en el sistema; solo ayuda a coordinarla.
""".strip()


GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "").strip()
if GEMINI_API_KEY == "tu_api_key_de_gemini":
    GEMINI_API_KEY = ""
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.0-flash").strip()
GEMINI_TEMPERATURE = float(os.getenv("GEMINI_TEMPERATURE", "0.8"))
GEMINI_TIMEOUT = int(os.getenv("GEMINI_TIMEOUT", "20"))
GEMINI_MAX_OUTPUT_TOKENS = int(os.getenv("GEMINI_MAX_OUTPUT_TOKENS", "220"))
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080").rstrip("/")


def parse_history_limit(value):
    text = str(value or "10").strip().lower()
    if text in {"0", "libre", "unlimited", "ilimitado", "none", "sin_limite"}:
        return None

    try:
        limit = int(text)
    except ValueError:
        return 10

    return max(limit, 2)


MAX_HISTORY_MESSAGES = parse_history_limit(os.getenv("CHATBOT_HISTORY_LIMIT", "10"))

chat_histories = defaultdict(lambda: deque(maxlen=MAX_HISTORY_MESSAGES))
session_memory = defaultdict(dict)
APP_VERSION = "2026-07-19-chatbot-diagnostico-v5"


INTENT_KEYWORDS = {
    "emergency": (
        "emergencia", "urgencia", "convulsion", "convulsiona", "sangra", "sangrado",
        "no respira", "ahoga", "intoxic", "veneno", "atropell", "desmayo",
        "no se levanta", "dolor intenso", "respirar", "atropello", "colapso",
    ),
    "appointment": (
        "cita", "agendar", "agenda", "reservar", "reserva", "manana", "tarde",
        "noche", "hoy", "consulta", "turno", "lunes", "martes", "miercoles",
        "jueves", "viernes", "sabado", "domingo",
    ),
    "vaccines": ("vacuna", "vacunas", "vacunacion", "rabia", "parvovirus", "triple"),
    "baths": ("bano", "banos", "banio", "banios", "peluqueria", "grooming"),
    "deworming": ("desparasitar", "desparasitacion", "parasito", "pulga", "garrapata"),
    "sterilization": ("esterilizar", "esterilizacion", "castrar", "castracion"),
    "system_data": ("cuantas citas", "citas hay", "mascotas registradas", "clientes existen", "clientes registrados", "resumen del sistema"),
    "hours": ("horario", "hora", "atienden", "abren", "cierran"),
    "feeding": ("alimento", "alimentacion", "comida", "croquetas", "dieta", "comer"),
    "symptoms": (
        "no come", "no quiere comer", "vomita", "vomito", "diarrea", "decaido",
        "tos", "cojea", "dolor", "fiebre", "sangre", "heces", "temblor",
        "rasca", "picazon", "picor",
    ),
    "pets": ("perro", "perros", "perrito", "perritos", "gato", "gatos", "gatito", "gatitos", "conejo", "hamster", "cobayo"),
    "greeting": ("hola", "buenos dias", "buenas tardes", "buenas noches"),
    "thanks": ("gracias", "muchas gracias", "te agradezco"),
    "farewell": ("adios", "chau", "hasta luego", "nos vemos"),
}

RESPONSE_BANK = {
    "greeting": [
        f"Hola {SMILE} Bienvenido a Veterinaria Pet Clinic. Cuentame, en que puedo ayudarte hoy con tu mascota?",
        f"Hola, que gusto atenderte {SMILE}. Puedo ayudarte con citas, vacunas, banos, horarios o cuidados para tu mascota.",
        f"Hola {PAWS} Soy el asistente de Pet Clinic. Dime que necesita tu mascota y te oriento con gusto.",
    ],
    "general": [
        f"Claro {SMILE}. Para ayudarte mejor, cuentame si tu consulta es sobre una cita, vacunas, banos, alimentacion o algun sintoma.",
        "Entiendo. Dame un poquito mas de detalle sobre tu mascota y lo que esta pasando, asi puedo orientarte mejor.",
        f"Con gusto te ayudo {PAWS}. Puedes preguntarme por horarios, servicios, citas o cuidados basicos.",
    ],
    "appointment": [
        f"Claro, te ayudo a coordinar la cita {SMILE}. Indicame nombre del cliente, tipo de mascota, horario de preferencia y motivo de consulta.",
        "Perfecto, podemos avanzar con la cita. Necesitaria nombre del cliente, mascota, horario aproximado y el motivo de la visita.",
        f"Si deseas agendar, pasame los datos principales {PAWS}: nombre, mascota, dia u horario, y motivo de consulta.",
    ],
    "symptoms": [
        f"Lamento que tu mascota este asi {PAWS}. No puedo dar un diagnostico por chat, pero si no come por mas de 24 horas, vomita, esta muy decaida o tiene dolor, lo mejor es una consulta veterinaria.",
        "Entiendo tu preocupacion. Observa si hay vomitos, diarrea, fiebre, dolor o decaimiento; si empeora o continua, conviene que la revise un veterinario.",
        f"Pobrecito {PAWS}. Puede deberse a varias causas, por eso es mejor no asumir un diagnostico. Si hay signos fuertes o persistentes, agenda una consulta cuanto antes.",
    ],
    "emergency": [
        f"{WARNING} Eso puede ser una emergencia. Si hay convulsiones, sangrado, intoxicacion, dificultad para respirar o no puede levantarse, llevalo a una veterinaria de inmediato.",
        "Lamento mucho que esten pasando por eso. Por los signos que mencionas, lo mas seguro es buscar atencion veterinaria urgente.",
    ],
    "vaccines": [
        f"Muy buena decision cuidar sus vacunas {SYRINGE}. El calendario depende de edad, especie e historial; si tienes su cartilla, podemos revisar que refuerzos necesita.",
        "Las vacunas ayudan a prevenir enfermedades importantes. Para orientarte bien, dime si es perro o gato y que edad tiene.",
    ],
    "baths": [
        f"Si, podemos orientarte con banos y cuidado de higiene {BATH}. Si tiene picazon, irritacion o mal olor fuerte, conviene revisarlo antes para cuidar su piel.",
        "Para banos, lo ideal es usar productos aptos para mascotas. La frecuencia depende del pelaje, piel y rutina de tu mascota.",
    ],
    "deworming": [
        "La desparasitacion es importante y debe ajustarse a edad, peso y especie. Lo ideal es indicarla con esos datos para evitar dosis incorrectas.",
        f"Podemos orientarte con desparasitacion interna y externa {PAWS}. Dime que mascota tienes y su peso aproximado.",
    ],
    "sterilization": [
        "La esterilizacion puede ayudar a prevenir camadas no planificadas y algunos problemas de salud. Para orientarte mejor, dime si es perro o gato, su edad y si ya tuvo alguna evaluacion veterinaria.",
        "Podemos ayudarte a coordinar una evaluacion para esterilizacion. Lo ideal es revisar edad, peso, estado general y vacunas antes de programarla.",
    ],
    "system_data": [
        "Puedo revisar el resumen del sistema si Spring Boot esta encendido. Dame un momento y te comparto solo los datos disponibles.",
    ],
    "hours": [
        f"Con gusto {SMILE}. Nuestro horario habitual es de lunes a sabado de 8:00 a.m. a 7:00 p.m.",
        "Atendemos normalmente de lunes a sabado de 8:00 a.m. a 7:00 p.m. Si es una urgencia, conviene comunicarse directamente con la clinica.",
    ],
    "feeding": [
        "La alimentacion depende de especie, edad, peso y salud. En general, evita chocolate, cebolla, uvas, huesos cocidos y comida muy condimentada.",
        "Con gusto te oriento. Dime si es perro, gato u otra mascota, su edad aproximada y si tiene algun problema de salud.",
    ],
    "pets": [
        f"Claro {DOG}{CAT}. Atendemos perros, gatos y otras mascotas pequenas. Podemos ayudarte con consultas, vacunas, banos y desparasitacion.",
        "Si, trabajamos con mascotas comunes como perros y gatos. Dime que mascota tienes y que necesitas para orientarte mejor.",
    ],
    "thanks": [
        f"Con mucho gusto {SMILE}. Estoy aqui para ayudarte cuando necesites orientacion para tu mascota.",
        "De nada. Espero que tu mascota este muy bien; si tienes otra consulta, cuentame con confianza.",
    ],
    "farewell": [
        f"Hasta luego {SMILE}. Gracias por comunicarte con Veterinaria Pet Clinic.",
        "Nos vemos. Si notas algun signo preocupante en tu mascota, recuerda que lo mejor es una revision veterinaria.",
    ],
}


def normalize_text(text):
    normalized = unicodedata.normalize("NFD", text)
    without_accents = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return without_accents.lower()


def get_session_id(payload):
    session_id = str(payload.get("session_id", "")).strip()
    return session_id or str(uuid.uuid4())


def get_spring_json(path):
    url = f"{SPRING_BASE_URL}{path}"
    spring_request = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(spring_request, timeout=3) as response:
            return json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, ValueError):
        return None


def extract_session_facts(message):
    facts = {}
    name_match = re.search(r"\b(?:me llamo|soy|mi nombre es)\s+([A-Za-zÁÉÍÓÚáéíóúÑñ]{3,})", message, re.IGNORECASE)
    pet_match = re.search(r"\b(?:mi mascota se llama|mi perro se llama|mi gato se llama|se llama)\s+([A-Za-zÁÉÍÓÚáéíóúÑñ]{2,})", message, re.IGNORECASE)
    reason_match = re.search(r"\b(?:motivo|consulta|porque|por que)\s+(.+)$", message, re.IGNORECASE)

    if name_match:
        facts["cliente"] = name_match.group(1).strip().title()
    if pet_match:
        facts["mascota_nombre"] = pet_match.group(1).strip().title()
    if reason_match and len(reason_match.group(1).strip()) >= 4:
        facts["motivo"] = reason_match.group(1).strip()

    normalized = normalize_text(message)
    pet_type = re.search(r"\b(perro|perrito|gato|gatito|conejo|hamster|cobayo)\b", normalized)
    if pet_type:
        facts["mascota_tipo"] = pet_type.group(1)

    return facts


def update_session_memory(session_id, message):
    facts = extract_session_facts(message)
    if facts:
        session_memory[session_id].update(facts)


def serialize_memory(session_id):
    memory = session_memory.get(session_id, {})
    if not memory:
        return "Sin datos recordados."

    labels = {
        "cliente": "Cliente",
        "mascota_nombre": "Nombre de mascota",
        "mascota_tipo": "Tipo de mascota",
        "motivo": "Motivo",
    }
    return "\n".join(f"{labels.get(key, key)}: {value}" for key, value in memory.items())


def looks_like_appointment_data(normalized_message):
    has_pet = any(pet in normalized_message for pet in ("perro", "gato", "conejo", "hamster", "cobayo"))
    has_time = any(
        time in normalized_message
        for time in (
            "manana", "tarde", "noche", "hoy", "lunes", "martes", "miercoles",
            "jueves", "viernes", "sabado", "domingo",
        )
    )
    has_name_like_word = bool(re.search(r"\b[a-zA-Z]{3,}\b", normalized_message))
    return has_pet and has_time and has_name_like_word


def contains_keyword(text, keyword):
    if " " in keyword:
        return keyword in text

    return re.search(rf"\b{re.escape(keyword)}\b", text) is not None


def has_intent_keyword(normalized_message, intent):
    return any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS[intent])


def has_any_keyword(normalized_message, keywords):
    return any(contains_keyword(normalized_message, keyword) for keyword in keywords)


def detect_species(normalized_message):
    if has_any_keyword(normalized_message, ("perro", "perrito", "canino")):
        return "perro"
    if has_any_keyword(normalized_message, ("gato", "gatito", "felino")):
        return "gato"
    if has_any_keyword(normalized_message, ("conejo", "hamster", "cobayo")):
        return "mascota pequena"
    return "mascota"


def contextual_response(message, intent):
    normalized_message = normalize_text(message)
    species = detect_species(normalized_message)

    mentions_vaccine = has_intent_keyword(normalized_message, "vaccines")
    mentions_symptom = has_intent_keyword(normalized_message, "symptoms")
    mentions_diarrhea = has_any_keyword(normalized_message, ("diarrea", "heces blandas", "heces liquidas"))
    mentions_vomit = has_any_keyword(normalized_message, ("vomita", "vomito", "vomitos"))
    mentions_not_eating = has_any_keyword(normalized_message, ("no come", "no quiere comer", "sin apetito"))

    if mentions_vaccine and mentions_symptom:
        if mentions_diarrhea:
            return (
                f"Si tu {species} tiene diarrea, no conviene elegir una vacuna por chat ni vacunarlo mientras esta enfermo. "
                "Primero debe evaluarlo un veterinario para descartar infeccion, parasitos, alimento en mal estado u otra causa. "
                "Cuanto tiempo lleva con diarrea y ha vomitado, tiene sangre en las heces o esta decaido?"
            )
        return (
            f"Si tu {species} tiene sintomas, lo mas prudente es revisarlo antes de vacunar. "
            "Las vacunas se aplican cuando la mascota esta clinicamente estable. "
            "Cuanto tiempo lleva asi y que signos notas exactamente?"
        )

    if intent == "symptoms":
        if mentions_diarrhea and mentions_vomit:
            return (
                f"Entiendo, diarrea y vomitos juntos pueden deshidratar rapido a tu {species}. "
                "No le des medicamentos humanos; ofrece agua en pequenas cantidades y agenda una revision cuanto antes. "
                "Hay sangre, fiebre, decaimiento fuerte o es cachorro?"
            )
        if mentions_diarrhea:
            return (
                f"Entiendo. En un {species}, la diarrea puede deberse a cambio de alimento, parasitos, infeccion o algo que comio. "
                "Si dura mas de 24 horas, hay sangre, vomitos o decaimiento, conviene evaluarlo hoy. "
                "Desde cuando empezo y que edad tiene?"
            )
        if mentions_not_eating:
            return (
                f"Entiendo tu preocupacion. Si tu {species} no come, necesito saber desde cuando, si toma agua y si hay vomitos, diarrea o decaimiento. "
                "Si lleva mas de 24 horas sin comer o esta muy apagado, lo mejor es consulta veterinaria."
            )

    if intent == "vaccines":
        if species == "perro":
            return (
                "Para perros, las vacunas suelen organizarse segun edad, cartilla y refuerzos: multiple canina y rabia son referencias comunes. "
                "Para decirte cual corresponde, necesito edad, si ya tiene cartilla y cuando fue su ultima vacuna."
            )
        if species == "gato":
            return (
                "Para gatos, el plan depende de edad, estilo de vida y cartilla; suelen considerarse triple felina y rabia segun evaluacion. "
                "Dime su edad y si vive solo en casa o tambien sale."
            )

    return None


def detect_intent(message, history):
    normalized_message = normalize_text(message)
    words = re.findall(r"[a-zA-Z]+", normalized_message)
    is_short_message = len(words) <= 2

    immediate_intents = (
        "emergency", "symptoms", "greeting", "thanks", "farewell",
    )
    for intent in immediate_intents:
        if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS[intent]):
            return intent

    if any(keyword in normalized_message for keyword in INTENT_KEYWORDS["system_data"]):
        return "system_data"

    if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS["appointment"]):
        return "appointment"

    if looks_like_appointment_data(normalized_message):
        return "appointment"

    service_intents = ("vaccines", "baths", "deworming", "feeding")
    for intent in service_intents:
        if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS[intent]):
            return intent

    if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS["sterilization"]):
        return "sterilization"

    if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS["pets"]):
        return "pets"

    if any(contains_keyword(normalized_message, keyword) for keyword in INTENT_KEYWORDS["hours"]):
        return "hours"

    if not is_short_message and history:
        last_assistant = next(
            (item["content"] for item in reversed(history) if item.get("role") == "assistant"),
            "",
        )
        if "motivo de consulta" in normalize_text(last_assistant):
            return "appointment"

    return "general"


def serialize_history(history):
    if not history:
        return "Sin historial previo."

    lines = []
    for item in history:
        speaker = "Usuario" if item["role"] == "user" else "Asistente"
        lines.append(f"{speaker}: {item['content']}")

    return "\n".join(lines)


def build_user_prompt(message, intent, history, session_id):
    return f"""
Datos recordados de esta sesion:
{serialize_memory(session_id)}

Historial reciente:
{serialize_history(history)}

Intencion detectada: {intent}
Mensaje actual del usuario: {message}

Responde como recepcionista veterinaria de Pet Clinic. Mantente natural, breve y util.
Si el usuario esta dando datos para una cita, interpreta nombre, mascota, dia u horario y pregunta solo el dato que falte.
""".strip()


def generate_with_gemini(message, intent, history, session_id):
    if not GEMINI_API_KEY:
        return None

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"
    )
    payload = {
        "systemInstruction": {
            "parts": [{"text": SYSTEM_PROMPT}]
        },
        "contents": [
            {
                "role": "user",
                "parts": [{"text": build_user_prompt(message, intent, history, session_id)}],
            }
        ],
        "generationConfig": {
            "temperature": GEMINI_TEMPERATURE,
            "maxOutputTokens": GEMINI_MAX_OUTPUT_TOKENS,
        },
    }
    request_data = json.dumps(payload).encode("utf-8")
    gemini_request = urllib.request.Request(
        url,
        data=request_data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(gemini_request, timeout=GEMINI_TIMEOUT) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        app.logger.warning("Gemini no respondio correctamente. HTTP %s", error.code)
        return None
    except (urllib.error.URLError, TimeoutError, ValueError) as error:
        app.logger.warning("Gemini no disponible: %s", error)
        return None

    candidates = data.get("candidates", [])
    if not candidates:
        return None

    parts = candidates[0].get("content", {}).get("parts", [])
    text = "\n".join(part.get("text", "") for part in parts).strip()
    return text or None


def extract_appointment_details(message):
    normalized = normalize_text(message)
    details = []
    pet_match = re.search(r"\b(perro|perrito|gato|gatito|conejo|hamster|cobayo)\b", normalized)
    time_match = re.search(
        r"\b(manana|tarde|noche|hoy|lunes|martes|miercoles|jueves|viernes|sabado|domingo)\b",
        normalized,
    )
    name_match = re.search(r"\b([A-Z][a-z]{2,})\b", normalize_text(message).title())
    ignored_names = {
        "Perro", "Perrito", "Gato", "Gatito", "Conejo", "Hamster", "Cobayo",
        "Manana", "Tarde", "Noche", "Hoy", "Lunes", "Martes", "Miercoles",
        "Jueves", "Viernes", "Sabado", "Domingo", "Cita", "Consulta", "Agendar",
        "Reservar", "Hola", "Gracias",
    }

    if name_match and name_match.group(1) not in ignored_names:
        details.append(f"cliente {name_match.group(1)}")
    if pet_match:
        details.append(f"mascota {pet_match.group(1)}")
    if time_match:
        details.append(f"horario {time_match.group(1)}")

    return ", ".join(details)


def choose_response(intent, history):
    options = RESPONSE_BANK.get(intent, RESPONSE_BANK["general"])
    last_assistant = next(
        (item["content"] for item in reversed(history or []) if item.get("role") == "assistant"),
        "",
    )
    fresh_options = [option for option in options if option != last_assistant]
    return random.choice(fresh_options or options)


def fallback_response(message, intent, history=None):
    contextual = contextual_response(message, intent)
    if contextual:
        return contextual

    if intent == "emergency":
        return choose_response("emergency", history)

    if intent == "system_data":
        summary = get_spring_json("/api/chatbot/resumen")
        if not summary:
            return "Puedo ayudarte con ese dato, pero ahora no logro conectar con Spring Boot. Verifica que la aplicacion principal este encendida y vuelvo a intentarlo."
        return (
            f"Resumen actual: hoy hay {summary.get('citasHoy', 0)} citas, "
            f"{summary.get('citasPendientes', 0)} citas pendientes, "
            f"{summary.get('mascotas', 0)} mascotas registradas y "
            f"{summary.get('clientes', 0)} clientes registrados."
        )

    if intent == "appointment":
        details = extract_appointment_details(message)
        if details:
            return (
                f"Perfecto {SMILE} Tengo estos datos para coordinar la cita: {details}. Solo faltaria confirmar el motivo de consulta."
            )
        return choose_response("appointment", history)

    if intent in RESPONSE_BANK:
        return choose_response(intent, history)

    return choose_response("general", history)


def save_message(session_id, role, content):
    chat_histories[session_id].append(
        {
            "role": role,
            "content": content,
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
    )


def build_response(message, session_id):
    update_session_memory(session_id, message)
    history = chat_histories[session_id]
    intent = detect_intent(message, history)
    if intent == "system_data":
        gemini_response = None
    else:
        gemini_response = generate_with_gemini(message, intent, history, session_id)
    provider = "gemini" if gemini_response else "local-fallback"
    response = gemini_response or fallback_response(message, intent, history)

    save_message(session_id, "user", message)
    save_message(session_id, "assistant", response)

    return {
        "response": response,
        "intent": intent,
        "session_id": session_id,
        "history_size": len(chat_histories[session_id]),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "provider": provider,
    }


@app.post("/chat")
def chat():
    payload = request.get_json(silent=True) or {}
    message = str(payload.get("message", "")).strip()
    session_id = get_session_id(payload)

    if not message:
        return jsonify({"error": "El campo 'message' es obligatorio."}), 400

    try:
        return jsonify(build_response(message, session_id))
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, ValueError, KeyError) as error:
        app.logger.exception("Error generando respuesta del chatbot")
        intent = detect_intent(message, chat_histories[session_id])
        response = fallback_response(message, intent, chat_histories[session_id])
        save_message(session_id, "user", message)
        save_message(session_id, "assistant", response)

        return jsonify(
            {
                "response": response,
                "intent": intent,
                "session_id": session_id,
                "provider": "local-fallback",
                "warning": str(error),
                "timestamp": datetime.now(timezone.utc).isoformat(),
            }
        )


DIAGNOSTIC_SYSTEM_PROMPT = """
Eres un sistema de apoyo a triaje veterinario para Veterinaria Pet Clinic.
A partir de datos clinicos basicos (especie, edad, temperatura, sintomas) generas una orientacion
preliminar para el personal de recepcion, NUNCA un diagnostico medico definitivo.

Reglas clinicas:
- Usa siempre lenguaje de probabilidad ("posible", "sugiere", "podria tratarse de").
- No indiques medicamentos, dosis ni tratamientos especificos.
- Si los sintomas son compatibles con una emergencia (convulsiones, sangrado, dificultad
  respiratoria, intoxicacion, colapso, no puede levantarse), la confianza debe ser alta (85-98)
  y las recomendaciones deben priorizar atencion veterinaria inmediata.
- Si los datos son insuficientes o muy genericos, usa una confianza baja (40-60) y recomienda
  examen fisico presencial.

Formato de salida:
Responde EXCLUSIVAMENTE con un objeto JSON valido, sin texto adicional, sin backticks ni
comentarios, con exactamente esta forma:
{"titulo": "string corto", "confianza": numero_entre_0_y_100, "justificacion": "string breve", "recomendaciones": ["string", "string", "string"]}
""".strip()


def build_diagnostic_prompt(especie, edad, temperatura, sintomas):
    return f"""
Especie: {especie or "no especificada"}
Edad aproximada (anios): {edad or "no especificada"}
Temperatura corporal (C): {temperatura or "no registrada"}
Sintomas observados: {sintomas}

Genera la orientacion preliminar en el formato JSON indicado.
""".strip()


def normalize_diagnosis(data):
    if not isinstance(data, dict):
        return None

    titulo = str(data.get("titulo") or "").strip()
    justificacion = str(data.get("justificacion") or "").strip()
    recomendaciones_raw = data.get("recomendaciones")

    if not titulo or not justificacion or not isinstance(recomendaciones_raw, list):
        return None

    recomendaciones = [str(item).strip() for item in recomendaciones_raw if str(item).strip()][:4]
    if not recomendaciones:
        return None

    try:
        confianza = int(round(float(data.get("confianza", 65))))
    except (TypeError, ValueError):
        confianza = 65
    confianza = max(30, min(confianza, 98))

    return {
        "titulo": titulo,
        "confianza": confianza,
        "justificacion": justificacion,
        "recomendaciones": recomendaciones,
    }


def generate_diagnosis_with_gemini(especie, edad, temperatura, sintomas):
    if not GEMINI_API_KEY:
        return None

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        f"{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"
    )
    payload = {
        "systemInstruction": {"parts": [{"text": DIAGNOSTIC_SYSTEM_PROMPT}]},
        "contents": [
            {
                "role": "user",
                "parts": [{"text": build_diagnostic_prompt(especie, edad, temperatura, sintomas)}],
            }
        ],
        "generationConfig": {
            "temperature": 0.4,
            "maxOutputTokens": 400,
            "responseMimeType": "application/json",
        },
    }
    request_data = json.dumps(payload).encode("utf-8")
    gemini_request = urllib.request.Request(
        url,
        data=request_data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(gemini_request, timeout=GEMINI_TIMEOUT) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        app.logger.warning("Gemini no respondio correctamente en diagnostico. HTTP %s", error.code)
        return None
    except (urllib.error.URLError, TimeoutError, ValueError) as error:
        app.logger.warning("Gemini no disponible para diagnostico: %s", error)
        return None

    candidates = data.get("candidates", [])
    if not candidates:
        return None

    parts = candidates[0].get("content", {}).get("parts", [])
    text = "\n".join(part.get("text", "") for part in parts).strip()
    if not text:
        return None

    if text.startswith("```"):
        text = re.sub(r"^```(json)?", "", text).strip()
        text = re.sub(r"```$", "", text).strip()

    try:
        parsed = json.loads(text)
    except ValueError:
        app.logger.warning("Gemini devolvio un JSON invalido en diagnostico.")
        return None

    return normalize_diagnosis(parsed)


def diagnosis_fallback(especie, sintomas):
    normalized = normalize_text(sintomas)
    especie_normalizada = normalize_text(especie or "")

    if has_intent_keyword(normalized, "emergency"):
        return {
            "titulo": "Posible cuadro de emergencia",
            "confianza": 92,
            "justificacion": (
                "Los sintomas descritos son compatibles con una urgencia veterinaria y requieren "
                "evaluacion presencial inmediata."
            ),
            "recomendaciones": [
                "Trasladar a la mascota de inmediato a la clinica.",
                "No administrar medicamentos sin indicacion veterinaria.",
                "Avisar al area de emergencias para priorizar la atencion.",
            ],
        }

    if has_any_keyword(normalized, ("vomito", "vomita", "vomitos", "diarrea")):
        es_perro = "perro" in especie_normalizada or "canino" in especie_normalizada
        return {
            "titulo": "Posible Gastroenteritis" + (" (descartar Parvovirus)" if es_perro else ""),
            "confianza": 82 if es_perro else 76,
            "justificacion": (
                "Los sintomas gastrointestinales descritos sugieren inflamacion del tracto "
                "digestivo; en cachorros no vacunados conviene descartar causas infecciosas."
            ),
            "recomendaciones": [
                "Agendar consulta lo antes posible.",
                "Suspender alimento solido por unas horas y mantener hidratacion.",
                "Observar si aparece sangre, fiebre o decaimiento marcado.",
            ],
        }

    if has_any_keyword(normalized, ("cojea", "pata", "hueso", "fractura")):
        return {
            "titulo": "Posible Traumatismo Musculoesqueletico",
            "confianza": 78,
            "justificacion": (
                "La dificultad para apoyar o mover una extremidad sugiere una lesion articular, "
                "muscular u osea."
            ),
            "recomendaciones": [
                "Mantener a la mascota en reposo y evitar que salte o corra.",
                "Agendar evaluacion con radiografia si el signo persiste.",
                "Evitar manipular la zona afectada.",
            ],
        }

    if has_any_keyword(normalized, ("rasca", "picazon", "picor", "pulga", "garrapata")):
        return {
            "titulo": "Posible Dermatitis Alergica o Parasitaria",
            "confianza": 74,
            "justificacion": (
                "El rascado frecuente y las alteraciones en piel suelen asociarse a parasitos "
                "externos o alergias."
            ),
            "recomendaciones": [
                "Revisar el pelaje en busca de pulgas o garrapatas.",
                "Agendar consulta de dermatologia.",
                "Evitar banarlo hasta la evaluacion.",
            ],
        }

    if has_any_keyword(normalized, ("fiebre", "decaido", "decaimiento", "letargo")):
        return {
            "titulo": "Posible Cuadro Infeccioso o Inflamatorio",
            "confianza": 68,
            "justificacion": (
                "La combinacion de fiebre y decaimiento indica una respuesta inmunologica activa "
                "que conviene investigar."
            ),
            "recomendaciones": [
                "Agendar consulta general en las proximas horas.",
                "Controlar la temperatura periodicamente.",
                "Mantener a la mascota hidratada y en reposo.",
            ],
        }

    return {
        "titulo": "Cuadro clinico no concluyente",
        "confianza": 55,
        "justificacion": (
            "Los sintomas descritos son generales; se requiere examen fisico para orientar el "
            "diagnostico."
        ),
        "recomendaciones": [
            "Agendar cita de revision general.",
            "Observar la evolucion durante las proximas horas.",
        ],
    }


@app.post("/diagnostico")
def diagnostico():
    payload = request.get_json(silent=True) or {}
    especie = str(payload.get("especie", "")).strip()
    edad = str(payload.get("edad", "")).strip()
    temperatura = str(payload.get("temperatura", "")).strip()
    sintomas = str(payload.get("sintomas", "")).strip()

    if not sintomas:
        return jsonify({"error": "El campo 'sintomas' es obligatorio."}), 400

    result = generate_diagnosis_with_gemini(especie, edad, temperatura, sintomas)
    provider = "gemini"
    if not result:
        result = diagnosis_fallback(especie, sintomas)
        provider = "local-fallback"

    result["provider"] = provider
    result["timestamp"] = datetime.now(timezone.utc).isoformat()
    return jsonify(result)


@app.post("/reset")
def reset():
    payload = request.get_json(silent=True) or {}
    session_id = str(payload.get("session_id", "")).strip()

    if session_id:
        chat_histories.pop(session_id, None)
        session_memory.pop(session_id, None)

    return jsonify({"status": "ok", "session_id": session_id})


@app.get("/health")
def health():
    return jsonify(
        {
            "status": "ok",
            "service": "pet-clinic-chatbot",
            "gemini_enabled": bool(GEMINI_API_KEY),
            "model": GEMINI_MODEL if GEMINI_API_KEY else None,
            "history_limit": "unlimited" if MAX_HISTORY_MESSAGES is None else MAX_HISTORY_MESSAGES,
            "spring_base_url": SPRING_BASE_URL,
            "version": APP_VERSION,
        }
    )


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    app.run(host="0.0.0.0", port=port, debug=os.getenv("FLASK_DEBUG") == "1")