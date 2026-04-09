# GVGAI Neuroevolution Agent Game 68

Este proyecto implementa un agente para el framework **GVGAI (General Video Game AI)** utilizando **neuroevolución** para aprender a jugar a juegos tipo laberinto (similar a Pacman).

⚠️ **Importante:** Este repositorio solo contiene el código del agente.  
Para ejecutarlo es necesario instalar previamente el framework GVGAI.

---

## 🧠 Idea general

El agente utiliza una red neuronal cuyos pesos se optimizan mediante un algoritmo evolutivo.

- La red recibe información del entorno (mapa, enemigos, comida, etc.)
- Devuelve una acción del juego
- Los individuos se evalúan jugando partidas
- Se seleccionan y evolucionan los mejores

---

## 📦 Estructura

El repositorio incluye únicamente las clases del agente:

- `Agente.java` → interfaz con GVGAI
- `Brain.java` → red neuronal y decisión de acciones
- `Map.java` → representación del entorno
- `Evaluator.java` → cálculo de fitness
- `Evolution.java` → algoritmo evolutivo
- `TrainMain.java` → entrenamiento
- `Testing.java` → uso del agente
- `GameFactory.java`
- `Individual.java`
- `Scenario.java`

---

## ⚙️ Requisitos

Necesitas tener instalado GVGAI:

- Descargar el framework desde su repositorio oficial
- Importarlo como proyecto Java (por ejemplo en Eclipse o IntelliJ)

Este código está pensado para ejecutarse **dentro del proyecto GVGAI**, no como proyecto independiente.

---

## 🚀 Cómo usarlo

1. Descarga el framework GVGAI
2. Copia la carpeta `miAgente` dentro del proyecto (por ejemplo en `src/`)
3. Asegúrate de que el proyecto compila correctamente

### Entrenamiento

Ejecuta:

TrainMain.java
