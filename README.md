# GVGAI Neuroevolution Agent (Game 68)

This project implements an agent for the **GVGAI (General Video Game AI)** framework using **neuroevolution** to learn how to play maze-based games (similar to Pacman).

⚠️ **Important:** This repository only contains the agent code.  
To run it, you must first install the GVGAI framework.

---

## 🧠 Overview

The agent uses a neural network whose weights are optimized through an evolutionary algorithm.

- The network receives information about the environment (map, enemies, food, etc.)
- It outputs an action
- Individuals are evaluated by playing the game
- The best individuals are selected and evolved over generations

---

## 📦 Structure

This repository only includes the agent classes:

- `Agente.java` → interface with GVGAI
- `Brain.java` → neural network and decision logic
- `Map.java` → internal representation of the environment
- `Evaluator.java` → fitness calculation
- `Evolution.java` → evolutionary algorithm
- `TrainMain.java` → training entry point
- `Testing.java` → agent execution
- `GameFactory.java`
- `Individual.java`
- `Scenario.java`

---

## ⚙️ Requirements

You need to install GVGAI:

- Download the framework from its official repository
- Import it as a Java project (e.g. in Eclipse or IntelliJ)

This code is designed to run **inside the GVGAI project**, not as a standalone application.

---

## 🚀 How to use

1. Download the GVGAI framework  
2. Copy the `miAgente` folder into the project (e.g. inside `src/`)  
3. Make sure the project compiles correctly  

---

## 🏋️ Training

Run:

```bash
TrainMain.java
