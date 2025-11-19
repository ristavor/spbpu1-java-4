# Ant Colony Optimization Algorithm - Interactive Visualization

## 📋 Project Description

An interactive visualization of the Ant Colony Optimization (ACO) algorithm for solving the Traveling Salesman Problem (TSP). Implemented in Java with Swing GUI, this project demonstrates a metaheuristic optimization algorithm inspired by the foraging behavior of ants in nature.

## 🎯 Algorithm Overview

### Core Concepts

**Graph Environment:**
- Two-dimensional bidirectional graph representing the environment
- Graph nodes represent ant positions
- Graph edges have weights denoting distances between nodes
- Fully connected graph (each node connected to every other node)

**Ant Agents:**
- Software agents that traverse the graph
- Each ant maintains a tabu list of visited nodes
- An ant visits each node only once
- Ants deposit pheromones on traversed edges

**Initial Distribution:**
- Ant population is uniformly distributed across graph nodes

### Mathematical Model

**Probability of Selecting Next Node:**

The movement of an ant depends on the probability of selecting the next node according to the formula:

```
         [τ(r,u)]^α · [η(r,u)]^β
P(r,u) = ─────────────────────────
         Σ [τ(r,s)]^α · [η(r,s)]^β
         s∈J
```

Where:
- `τ(r,u)` — pheromone intensity on the edge between nodes r and u
- `η(r,u) = 1/d(r,u)` — heuristic information (inverse distance)
- `d(r,u)` — distance between nodes r and u
- `α` — algorithm parameter describing pheromone significance
- `β` — algorithm parameter describing distance significance
- `J` — set of unvisited nodes

**Pheromone Update:**

After ant k completes its tour through all nodes, the path length `L_k` is calculated, and pheromones are updated:

```
Δτ_k(r,u) = Q / L_k
```

Where:
- `Q` — algorithm parameter (constant)
- `L_k` — tour length of ant k

**Key principle:** A short path receives high pheromone intensity. A long path receives low pheromone intensity.

**Pheromone Evaporation:**

```
τ(r,u) ← (1 - ρ) · τ(r,u) + Σ Δτ_k(r,u)
                              k
```

Where:
- `ρ` — evaporation coefficient (0 < ρ < 1)

## 🚀 Features

### Visualization
- **Graph:** Nodes and edges displayed in 2D space
- **Pheromones:** Edge color and thickness indicate pheromone intensity (blue → red = low → high)
- **Ant Routes:** Each ant has a unique color and leaves a colored trail
- **Best Route:** Displayed as a bright green thick line over all paths
- **Edge Labels:** Show distance (d) and pheromone quantity (τ)

### Interactive Controls
- **Start/Pause:** Automatic simulation with adjustable speed
- **Step:** Step-by-step algorithm execution
- **Reset:** Restart with a new random configuration
- **Node Count:** Adjust number of graph nodes (4-40)
- **Ant Count:** Adjust number of ants (1-200)

### Information Panel
- Current iteration number
- Best found route length
- Algorithm parameter values (α, β, ρ)
- Legend explaining visual elements

## ⚙️ Algorithm Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| **α (Alpha)** | 1.0 | Pheromone significance in probability formula |
| **β (Beta)** | 5.0 | Distance significance in probability formula |
| **ρ (Rho)** | 0.5 | Pheromone evaporation coefficient (0 < ρ < 1) |
| **Q** | 100.0 | Constant for calculating pheromone contribution |
| **Initial Pheromone** | 0.1 | Initial intensity on each edge |
| **Default Nodes** | 18 | Number of graph nodes |
| **Default Ants** | 25 | Number of ants in population |

## 🛠️ Technical Requirements

- **Java:** 25+ (uses `record` for Node)
- **GUI:** Swing (javax.swing)
- **Dependencies:** Standard Java library only (no external dependencies)

## 📦 Project Structure

```
spbpu1-java-4/
├── src/
│   └── AntColonyDemo.java    # Complete implementation in a single file
├── spbpu1-java-4.iml
└── README.md
```

## 🎮 How to Run

### Option 1: Compile and Run from Command Line

```powershell
# Compile
javac src/AntColonyDemo.java

# Run
java -cp src AntColonyDemo
```

### Option 2: Run from IDE (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA
2. Navigate to `src/AntColonyDemo.java`
3. Right-click → Run 'AntColonyDemo.main()'

## 📊 How to Use

1. **Start Simulation:**
   - Click "Start" for automatic execution
   - Use "Step" for step-by-step mode

2. **Adjust Parameters:**
   - Change number of nodes (4-40) — graph will be recreated
   - Change number of ants (1-200)
   - Changes are applied immediately

3. **Observe Results:**
   - Monitor iteration number and best route length
   - Watch how pheromones concentrate on good paths
   - Green line shows the best found route

4. **Reset:**
   - Click "Reset" to restart with a new random configuration

## 🔬 Algorithm Workflow

### Phase 1: Initialization
1. Create graph with random node placement
2. Calculate distances between all node pairs
3. Initialize pheromones with initial value
4. Distribute ants across graph nodes

### Phase 2: Tour Construction
1. Each ant selects next node based on probability
2. Ant moves and adds node to its tour
3. Node is marked as visited (tabu list)
4. Process repeats until all nodes are visited

### Phase 3: Pheromone Update
1. Evaporation: all pheromones reduced by factor (1-ρ)
2. Each ant deposits pheromone on its tour
3. Contribution is inversely proportional to tour length
4. Update best found solution

### Phase 4: New Iteration
1. Create new generation of ants
2. Repeat phases 2-3
3. Gradual convergence to optimal solution

## 🎨 Visual Elements

| Element | Description |
|---------|-------------|
| **White circles** | Graph nodes |
| **Colored edges** | Pheromone intensity (blue → red = low → high) |
| **Edge thickness** | More pheromone = thicker line |
| **Colored dots** | Ants at current positions (each with unique color) |
| **Colored thin lines** | Current ant tours |
| **Bright green thick line** | Best found route |
| **d/τ labels** | Distance and pheromone on edge |

## 🧪 Implementation Features

- **Single-file architecture:** All code in one file for simplicity
- **Record for Node:** Using modern Java features
- **Step-by-step simulation:** Ants move one node at a time per step
- **Unique colors:** Each ant gets a random bright color
- **Normalized coordinates:** Nodes placed in [0.1, 0.9] to avoid edges
- **Zero-division protection:** Handling edge cases

## 📚 Theoretical Background

The Ant Colony Optimization algorithm was proposed by Marco Dorigo in 1992 and belongs to the class of metaheuristic algorithms based on the behavior of ant colonies in nature. The key idea is stigmergy (indirect interaction through environment modification).

**ACO Advantages:**
- Does not require gradient information
- Works well for combinatorial problems
- Can find approximate solutions for NP-hard problems
- Natural parallelization

**Applications:**
- Traveling Salesman Problem (TSP)
- Network routing
- Schedule planning
- Logistics optimization

## 👨‍💻 Author

Project created as part of the "Ant Algorithm" module assignment.

## 📄 License

This project is created for educational purposes and may be freely used for learning and research.

---

**Enjoy the simulation! 🐜**

