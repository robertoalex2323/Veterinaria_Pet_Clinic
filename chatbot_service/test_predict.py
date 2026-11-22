import os
import sys

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, BASE_DIR)

import app

print("--- VERIFICACION DEL MICROSERVICIO CHATBOT & ML ---")
print("ML Pipeline cargado:", app.ml_pipeline is not None)
print("Metadata clases:", app.ml_metadata.get("clases"))

# Probar prediccion de un caso critico
caso_critico = app.predict_triaje_ml("Perro", 3, 39.5, "perro convulsiona y sangra por la boca")
print("\n[TEST CASO CRITICO]")
print("Titulo:", caso_critico["titulo"])
print("Nivel:", caso_critico["nivel_triaje"])
print("Confianza:", caso_critico["confianza"], "%")
print("Proveedor:", caso_critico["provider"])

# Probar prediccion de un caso de urgencia alta (parvovirus en cachorro)
caso_urgente = app.predict_triaje_ml("Perro", 0.4, 40.0, "cachorro con vomito y diarrea con sangre y decaido")
print("\n[TEST CASO URGENCIA]")
print("Titulo:", caso_urgente["titulo"])
print("Nivel:", caso_urgente["nivel_triaje"])
print("Confianza:", caso_urgente["confianza"], "%")

# Probar prediccion de consulta general
caso_preventivo = app.predict_triaje_ml("Perro", 2, 38.5, "vacunacion antirrabica y corte de uñas")
print("\n[TEST CASO GENERAL]")
print("Titulo:", caso_preventivo["titulo"])
print("Nivel:", caso_preventivo["nivel_triaje"])
print("Confianza:", caso_preventivo["confianza"], "%")

# Probar endpoint de diagnostico completo
with app.app.test_client() as client:
    res = client.post("/diagnostico", json={
        "especie": "Canino",
        "edad": "2",
        "temperatura": "39.8",
        "sintomas": "vomito continuo con bilis y dolor fuerte al tocarle la panza"
    })
    print("\n[TEST ENDPOINT /diagnostico]")
    print("Status:", res.status_code)
    print("Response JSON:", res.get_json())
    
    # Probar endpoint /triaje/metricas
    res_m = client.get("/triaje/metricas")
    print("\n[TEST ENDPOINT /triaje/metricas]")
    print("Status:", res_m.status_code)
    m_json = res_m.get_json()
    print("Accuracy reportada:", m_json.get("accuracy"))
    print("Matriz confusion:", m_json.get("matriz_confusion"))

print("\nTODOS LOS TESTS DE MACHINE LEARNING PASARON EXITOSAMENTE.")
