"""
Script de entrenamiento para el Modelo Predictivo de Triaje y Diagnostico Veterinario
Veterinaria Pet Clinic - Piura, Peru.

Entrena un modelo con Scikit-learn (Pipeline: TF-IDF + RandomForestClassifier)
para predecir la categoria de triaje clinico, urgencia, diagnostico presuntivo y confianza.
Genera el archivo .pkl y guarda las metricas (Accuracy, Matriz de Confusion, Reporte).
"""

import json
import os
import joblib
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.pipeline import Pipeline
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "modelo_triaje_clinico.pkl")
METRICS_PATH = os.path.join(BASE_DIR, "metricas_modelo.json")

# Dataset de entrenamiento clinico veterinario para triaje en Piura
# Incluye variedad de sintomas en espanol, coloquiales y tecnicos, con sus categorias y patologias
CASOS_CLINICOS = [
    # --- EMERGENCIA_CRITICA (Nivel 1 - Rojo) ---
    ("Perro", 3.0, 39.8, "perro convulsiona repetidamente, tiene espuma en la boca y no responde a estimulos", "EMERGENCIA_CRITICA", "Sindrome Convulsivo / Estado Epileptico"),
    ("Gato", 2.0, 35.8, "gato atropellado en la avenida, sangrado abundante por hocico y no puede respirar", "EMERGENCIA_CRITICA", "Politraumatismo con Shock Hipovolemico"),
    ("Perro", 5.0, 38.5, "perro ingirio veneno para ratas hace media hora, vomita espuma y saliva constantemente", "EMERGENCIA_CRITICA", "Intoxicacion Aguda por Rodenticida"),
    ("Perro", 8.0, 37.0, "abdomen muy hinchado duro como tambor, intenta vomitar y no puede, no se puede parar", "EMERGENCIA_CRITICA", "Sindrome de Dilatacion-Torsion Volvulo Gastrico (GDV)"),
    ("Gato", 4.0, 39.0, "gato macho puja en la caja de arena gritando de dolor, no sale nada de orina, pene morado", "EMERGENCIA_CRITICA", "Obstruccion Uretral Felina / FLUTD Severo"),
    ("Perro", 1.0, 41.2, "cachorro con shock termico por calor en Piura, jadear desesperado lengua morada y colapso", "EMERGENCIA_CRITICA", "Golpe de Calor Hipertermico Severo"),
    ("Perro", 7.0, 36.5, "perro desmayado colapso repentino, encias completamente blancas palidas y respiracion muy debil", "EMERGENCIA_CRITICA", "Shock Hemorragico o Insuficiencia Cardiorrespiratoria"),
    ("Gato", 1.0, 38.2, "gatito convulsiona despues de ponerle una pipeta para perro, temblores musculares intensos", "EMERGENCIA_CRITICA", "Intoxicacion por Permetrinas"),
    ("Perro", 4.0, 37.5, "perro mordido por otro perro grande en el cuello, herida profunda sangrando a borbotones", "EMERGENCIA_CRITICA", "Hemorragia Arterial / Herida Punzopenetrante Cervical"),
    ("Perro", 6.0, 38.0, "se atraganto con un hueso de pollo, se asfixia, lengua azulada cianotica, intenta toser desesperado", "EMERGENCIA_CRITICA", "Obstruccion de Vias Aereas Superiores"),
    ("Perro", 2.0, 39.6, "convulsiones cada 15 minutos, rigidez de extremidades y desorientacion total", "EMERGENCIA_CRITICA", "Estatus Epileptico / Encefalopatia"),
    ("Gato", 5.0, 35.5, "gato completamente hipotermico no reacciona, cianosis en mucosas y pulso casi imperceptible", "EMERGENCIA_CRITICA", "Colapso Circulatorio Inminente"),

    # --- URGENCIA_ALTA (Nivel 2 - Naranja) ---
    ("Perro", 0.4, 39.9, "cachorro de 4 meses vomita liquido amarillo y tiene diarrea sanguinolenta fetida con olor a pescado podrido", "URGENCIA_ALTA", "Sospecha de Parvovirosis Canina"),
    ("Perro", 0.6, 40.2, "perrito sin vacunas con fiebre muy alta, flema verde en ojos y nariz, tos y decaimiento extremo", "URGENCIA_ALTA", "Sospecha de Distemper (Moquillo Canino)"),
    ("Perro", 3.0, 38.8, "perro cojea de la pata trasera izquierda tras caer de la azotea, mucho dolor al tocar y quejido fuerte", "URGENCIA_ALTA", "Sospecha de Fractura Osea o Luxacion Coxofemoral"),
    ("Gato", 3.0, 39.5, "gato mordido por otro gato, tiene absceso hinchado caliente en la mejilla que supura pus con sangre", "URGENCIA_ALTA", "Absceso Infeccioso Facial Severo"),
    ("Perro", 5.0, 39.4, "vomitos continuos mas de 6 veces en el dia, diarrea liquida no retiene agua y deshidratacion moderada", "URGENCIA_ALTA", "Gastroenteritis Severa con Deshidratacion"),
    ("Perro", 2.0, 39.7, "orina con sangre abundante color rojo oscuro, dolor abdominal marcado y fiebre", "URGENCIA_ALTA", "Infeccion Urinaria Aguda / Hematuria"),
    ("Gato", 0.5, 40.0, "gatito bebe con diarrea amarilla profusa, hipotermia inicial y rechazo absoluto del biberon", "URGENCIA_ALTA", "Panleucopenia Felina / Enteritis Viral"),
    ("Perro", 4.0, 38.6, "ojo muy rojo cerrado con secrecion mucosa abundante y lesion blanquecina en cornea", "URGENCIA_ALTA", "Ulcera Corneal Aguda"),
    ("Perro", 1.5, 39.8, "fiebre alta de 40 grados, petequias puntitos rojos en abdomen y garrapatas adheridas en Piura", "URGENCIA_ALTA", "Ehrlichiosis Canina Aguda"),
    ("Perro", 6.0, 39.2, "herida abierta en el lomo llena de gusanos gusanera activa miasis y muy mal olor", "URGENCIA_ALTA", "Miasis Cutanea Severa con Infeccion"),
    ("Gato", 2.0, 39.3, "gato vomita bilis repetidamente, dolor agudo en abdomen y letargo pronunciado", "URGENCIA_ALTA", "Sospecha de Cuerpo Extrano Intestinal"),
    ("Perro", 0.8, 40.1, "diarrea liquida con estrias de sangre fresca, inapetencia y deshidratacion marcada", "URGENCIA_ALTA", "Gastroenteritis Infecciosa"),

    # --- PRIORIDAD_MEDIA (Nivel 3 - Amarillo) ---
    ("Perro", 4.0, 38.7, "perro con tos seca carrasposa como si tuviera algo atorado en la garganta tras visitar parque", "PRIORIDAD_MEDIA", "Traqueobronquitis Infecciosa (Tos de las Perreras)"),
    ("Gato", 3.0, 38.9, "gato estornuda con frecuencia, legañas transparentes en ojos y secrecion nasal leve", "PRIORIDAD_MEDIA", "Rinotraqueitis Felina Leve / Complejo Respiratorio"),
    ("Perro", 2.0, 38.5, "vomito una sola vez ayer comida sin digerir, esta un poco desganado pero toma agua normalmente", "PRIORIDAD_MEDIA", "Indigestion Dietetica Leve"),
    ("Perro", 7.0, 38.4, "cojera leve en pata delantera al levantarse por la mañana, luego de caminar un rato mejora", "PRIORIDAD_MEDIA", "Osteoartrosis Degenerativa Cronica"),
    ("Perro", 1.0, 38.9, "diarrea pastosa con mucosidad desde ayer, sin sangre, animo estable y bebe agua", "PRIORIDAD_MEDIA", "Parasitosis Intestinal / Giardiasis"),
    ("Gato", 5.0, 38.6, "se limpia excesivamente el lomo, tiene zonas sin pelo alopécicas y pequenas costras", "PRIORIDAD_MEDIA", "Dermatitis Acarina o Alergia Leve"),
    ("Perro", 3.0, 38.8, "sacude mucho la cabeza hacia un lado, oreja caliente con cera marron oscura y olor vinagre", "PRIORIDAD_MEDIA", "Otitis Externa Bacteriana o por Levaduras"),
    ("Gato", 4.0, 38.7, "conjuntivitis en ambos ojos, parpadea con frecuencia pero come bien", "PRIORIDAD_MEDIA", "Conjuntivitis Felina Infecciosa"),
    ("Perro", 5.0, 38.6, "inapetencia moderada desde hace dos dias, come la mitad de su porcion y duerme mas tiempo", "PRIORIDAD_MEDIA", "Sindrome Febril o Malestar General"),
    ("Perro", 8.0, 38.3, "bulto o nodulo blando debajo de la piel en el costado del pecho, no duele al tacto", "PRIORIDAD_MEDIA", "Sospecha de Lipoma o Neoplasia Benigna"),
    ("Gato", 6.0, 38.8, "tose ocasionalmente con cuello extendido como queriendo botar bolas de pelo", "PRIORIDAD_MEDIA", "Tricobezoares (Bolas de Pelo) / Asma Felino Leve"),
    ("Perro", 2.5, 38.9, "se lame constantemente la pata entre los dedos, piel enrojecida e inflamada", "PRIORIDAD_MEDIA", "Pododermatitis Interdigital"),

    # --- CONSULTA_GENERAL (Nivel 4 - Verde) ---
    ("Perro", 2.0, 38.4, "el perro tiene muchas pulgas y garrapatas en las orejas y lomo, se rasca ocasionalmente", "CONSULTA_GENERAL", "Ectoparasitosis (Pulgas y Garrapatas)"),
    ("Gato", 1.0, 38.5, "consulta de rutina para aplicacion de vacuna antirrabica y desparasitacion trimestral", "CONSULTA_GENERAL", "Control Preventivo y Plan de Vacunacion"),
    ("Perro", 0.3, 38.6, "primera consulta de cachorro sano, revision general, carnet de vacunacion y corte de uñas", "CONSULTA_GENERAL", "Evaluacion Pediatrica Preventiva"),
    ("Perro", 5.0, 38.3, "mal aliento sarro amarillento en los dientes y encias un poco rojas, come croquetas bien", "CONSULTA_GENERAL", "Enfermedad Periodontal Grado 1-2"),
    ("Gato", 2.0, 38.4, "evaluacion prequirurgica para esterilizacion o castracion de mascota sana", "CONSULTA_GENERAL", "Evaluacion Prequirurgica Electiva"),
    ("Perro", 4.0, 38.5, "caida de pelo estacional abundante por muda de pelaje, piel sana sin lesiones ni picazon", "CONSULTA_GENERAL", "Muda Estacional Fisiologica"),
    ("Perro", 3.0, 38.4, "necesito baño medicado y corte higienico en la peluqueria canina", "CONSULTA_GENERAL", "Servicio de Grooming / Estetica Canina"),
    ("Gato", 3.0, 38.5, "desparasitacion preventiva interna, botó una pequeña lombriz en heces duras", "CONSULTA_GENERAL", "Plan Antiparasitario Preventivo"),
    ("Perro", 6.0, 38.3, "control de peso y cambio de dieta a alimento senior bajo en calorias", "CONSULTA_GENERAL", "Asesoria Nutricional Clinica"),
    ("Perro", 1.5, 38.5, "desea certificado de salud de viaje para transporte aereo nacional", "CONSULTA_GENERAL", "Certificacion Medico-Veterinaria de Viaje"),
    ("Gato", 2.5, 38.4, "chequeo general anual de gato casero completamente asintomatico", "CONSULTA_GENERAL", "Chequeo Preventivo Anual"),
    ("Perro", 2.0, 38.5, "manchas oscuras en la panza sin picor, piel sana parece pigmentacion natural", "CONSULTA_GENERAL", "Hiperpigmentacion Fisiologica Cutanea"),
]

# Ampliacion sintetica de casos variando terminologia para enriquecer el vocabulario
VARIACIONES = [
    # Emergencias
    ("convulsiona sin parar, rigidez y espuma", "EMERGENCIA_CRITICA", "Sindrome Convulsivo Agudo"),
    ("atropellado por moto, no mueve las patas traseras y sangra", "EMERGENCIA_CRITICA", "Traumatismo Medular / Shock"),
    ("comio veneno o raticida, pupilas dilatadas y colapso", "EMERGENCIA_CRITICA", "Intoxicacion Aguda"),
    ("ahogo asfixia con objeto extrano, encias moradas", "EMERGENCIA_CRITICA", "Obstruccion Respiratoria Aguda"),
    ("estomago inflado y duro, intenta regurgitar y quejido constante", "EMERGENCIA_CRITICA", "Torsion Gastrica Inminente"),
    ("desmayado inconsciente en el piso, golpe de calor en Piura", "EMERGENCIA_CRITICA", "Golpe de Calor Severo"),

    # Urgencias
    ("parvovirus confirmado por sintomas, vomito con sangre y olor fetido", "URGENCIA_ALTA", "Parvovirus Canino Agudo"),
    ("distemper moquillo sospecha, secrecion purulenta y fiebre 40", "URGENCIA_ALTA", "Distemper Canino"),
    ("patita rota con deformidad tras caida, llanto de dolor", "URGENCIA_ALTA", "Fractura Osea Traumatica"),
    ("fiebre 40.5 erlichia garrapatas apatia total no come", "URGENCIA_ALTA", "Ehrlichiosis Canina Aguda"),
    ("vomitos frecuentes mas de 5 veces y deshidratado", "URGENCIA_ALTA", "Gastroenteritis Severa"),
    ("ojo con herida o corte, parpado cerrado e hinchado", "URGENCIA_ALTA", "Trauma Ocular / Ulcera"),

    # Prioridad media
    ("tos seca tipo bocina de ganso tos de las perreras", "PRIORIDAD_MEDIA", "Traqueobronquitis Infecciosa"),
    ("rascado continuo de oreja con pus o cerumen oscuro", "PRIORIDAD_MEDIA", "Otitis Externa"),
    ("diarrea blanda sin sangre dos dias, come poco", "PRIORIDAD_MEDIA", "Enteritis Leve"),
    ("cojea levemente al caminar pero apoya la pata", "PRIORIDAD_MEDIA", "Claducacion Leve"),
    ("estornudos frecuentes y lagrimeo en gatito", "PRIORIDAD_MEDIA", "Complejo Respiratorio Felino"),
    ("perdida de apetito decaido desde ayer", "PRIORIDAD_MEDIA", "Malestar General Inespecifico"),

    # Consulta general
    ("vacunacion sextuple y rabia al dia", "CONSULTA_GENERAL", "Plan de Vacunacion Preventivo"),
    ("pulgas y garrapatas revision de pelaje", "CONSULTA_GENERAL", "Ectoparasitosis Canina"),
    ("corte de uñas y limpieza dental profilaxis", "CONSULTA_GENERAL", "Profilaxis y Cuidado General"),
    ("chequeo rutinario mascota sana", "CONSULTA_GENERAL", "Consulta Preventiva General"),
    ("desparasitacion en pastillas preventiva", "CONSULTA_GENERAL", "Desparasitacion Interna"),
    ("bano antipulgas y peluqueria", "CONSULTA_GENERAL", "Higiene y Estetica"),
]

def preparar_datos():
    textos = []
    categorias = []
    diagnosticos = []

    for especie, edad, temp, sintoma, cat, diag in CASOS_CLINICOS:
        texto_completo = f"{especie} de {edad} anios, temp {temp}C: {sintoma}"
        textos.append(texto_completo)
        categorias.append(cat)
        diagnosticos.append(diag)

    # Agregar variaciones
    for sintoma, cat, diag in VARIACIONES:
        for especie in ["Perro", "Gato"]:
            textos.append(f"{especie}: {sintoma}")
            categorias.append(cat)
            diagnosticos.append(diag)

    return textos, categorias, diagnosticos

def entrenar_y_evaluar():
    textos, categorias, diagnosticos = preparar_datos()

    X = textos
    y = categorias

    # Split estratificado train / test (80% entrenamiento, 20% evaluacion)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )

    # Pipeline: Extraccion TF-IDF n-gramas + Clasificador Random Forest
    pipeline = Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 2),
            max_features=400,
            strip_accents="unicode",
            lowercase=True
        )),
        ("classifier", RandomForestClassifier(
            n_estimators=120,
            max_depth=12,
            random_state=42,
            class_weight="balanced"
        ))
    ])

    print("Entrenando modelo de triaje clinico con Scikit-learn...")
    pipeline.fit(X_train, y_train)

    # Prediccion sobre test set
    y_pred = pipeline.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    clases = sorted(list(set(y)))
    cm = confusion_matrix(y_test, y_pred, labels=clases)
    report = classification_report(y_test, y_pred, labels=clases, output_dict=True)

    print(f"\n==========================================")
    print(f"RESULTADOS DE EVALUACION DEL MODELO (ML)")
    print(f"==========================================")
    print(f"Exactitud global (Accuracy): {accuracy * 100:.2f}%\n")
    print("Reporte de Clasificacion:")
    print(classification_report(y_test, y_pred, labels=clases))

    print("Matriz de Confusion:")
    print("Clases:", clases)
    print(cm)

    # Guardar modelo entrenado
    metadata = {
        "clases": clases,
        "diagnosticos_conocidos": dict(zip(textos, diagnosticos)),
        "n_samples": len(X),
        "n_features": 400,
        "accuracy": accuracy,
        "timestamp": "2026-11-22T10:45:00Z"
    }

    joblib.dump({"pipeline": pipeline, "metadata": metadata}, MODEL_PATH)
    print(f"\nModelo guardado exitosamente en: {MODEL_PATH}")

    # Guardar metricas en JSON para reporte academico
    metricas_guardadas = {
        "accuracy": round(accuracy, 4),
        "total_muestras": len(X),
        "muestras_entrenamiento": len(X_train),
        "muestras_evaluacion": len(X_test),
        "clases": clases,
        "matriz_confusion": cm.tolist(),
        "reporte_clasificacion": report
    }
    with open(METRICS_PATH, "w", encoding="utf-8") as f:
        json.dump(metricas_guardadas, f, indent=2, ensure_ascii=False)
    print(f"Metricas guardadas en: {METRICS_PATH}")

if __name__ == "__main__":
    entrenar_y_evaluar()
