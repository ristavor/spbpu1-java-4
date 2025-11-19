import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Визуальная демонстрация алгоритма муравьёв по ТЗ и PDF.
 * Один файл, Java 25, Swing GUI.
 */
public class AntColonyDemo extends JFrame {

    // ==== ПАРАМЕТРЫ АЛГОРИТМА (из ТЗ + типичные значения) ====

    // Начальное количество узлов графа
    private static final int INITIAL_NODE_COUNT = 18;

    // Параметры значимости фермента и расстояния в формуле вероятности
    private static final double ALPHA = 1.0;  // значимость фермента
    private static final double BETA = 5.0;   // значимость расстояния

    // Параметр ρ из PDF: коэффициент "сохранения" фермента (0 < RHO < 1)
    // τ_ij(t+1) = Δτ_ij(t) + ρ * τ_ij(t)
    private static final double RHO = 0.5;

    // Параметр Q для вычисления вклада фермента (Q / длина пути)
    private static final double Q = 100.0;

    // Начальная интенсивность фермента на каждом ребре
    private static final double INITIAL_PHEROMONE = 0.1;

    // Количество муравьёв по умолчанию
    private static final int DEFAULT_ANTS = 25;

    // Интервал таймера (мс) для автоматической симуляции
    private static final int TIMER_DELAY_MS = 120;

    // ==== GUI-поля ====
    private final GraphPanel graphPanel;
    private final JSpinner antsSpinner;
    private final Timer timer;
    private Simulation simulation;      // не final — пересоздаём при смене числа узлов
    private boolean running = false;

    // ==== КОНСТРУКТОР GUI ====

    public AntColonyDemo() {
        super("Алгоритм муравьёв (Ant Colony) - визуализация по ТЗ");

        this.simulation = new Simulation(INITIAL_NODE_COUNT, DEFAULT_ANTS);
        this.graphPanel = new GraphPanel();

        // Панель управления
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton startButton = new JButton("Старт");
        JButton pauseButton = new JButton("Пауза");
        JButton resetButton = new JButton("Сброс");
        JButton stepButton = new JButton("Шаг");

        antsSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_ANTS, 1, 200, 1));
        JSpinner nodesSpinner = new JSpinner(new SpinnerNumberModel(INITIAL_NODE_COUNT, 4, 40, 1));

        controlPanel.add(new JLabel("Узлов:"));
        controlPanel.add(nodesSpinner);
        controlPanel.add(new JLabel("Муравьёв:"));
        controlPanel.add(antsSpinner);
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stepButton);
        controlPanel.add(resetButton);

        setLayout(new BorderLayout());
        add(graphPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Таймер для автоматического шага
        timer = new Timer(TIMER_DELAY_MS, _ -> {
            simulation.step();
            graphPanel.repaint();
        });

        // Обработчики кнопок
        startButton.addActionListener(_ -> {
            if (!running) {
                running = true;
                timer.start();
            }
        });

        pauseButton.addActionListener(_ -> {
            running = false;
            timer.stop();
        });

        resetButton.addActionListener(_ -> {
            running = false;
            timer.stop();
            simulation.reset();
            graphPanel.repaint();
        });

        stepButton.addActionListener(_ -> {
            simulation.step();
            graphPanel.repaint();
        });

        antsSpinner.addChangeListener(e -> {
            int value = (Integer) ((JSpinner) e.getSource()).getValue();
            simulation.setAntCount(value);
            graphPanel.repaint();
        });

        nodesSpinner.addChangeListener(e -> {
            int newNodeCount = (Integer) ((JSpinner) e.getSource()).getValue();
            int currentAnts = (Integer) antsSpinner.getValue();
            // При желании можно ограничить муравьёв сверху числом узлов:
            // if (currentAnts > newNodeCount) {
            //     currentAnts = newNodeCount;
            //     antsSpinner.setValue(currentAnts);
            // }
            simulation = new Simulation(newNodeCount, currentAnts);
            graphPanel.repaint();
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    // ==== ТОЧКА ВХОДА ====

    static void main() {
        SwingUtilities.invokeLater(() -> {
            AntColonyDemo demo = new AntColonyDemo();
            demo.setVisible(true);
        });
    }

    // ==== ВНУТРЕННИЕ КЛАССЫ МОДЕЛИ ====

    /**
         * Узел графа (нормализованные координаты в [0;1])
         */
        record Node(double x, double y) {
    }

    /**
     * Муравей: маршрут, tabu-список, длина пути, текущий узел и свой цвет.
     */
    static class Ant {
        int currentNode;
        boolean[] visited;
        int[] tour;
        int tourIndex;
        double tourLength;
        boolean finished;
        Color color;   // уникальный цвет муравья

        Ant(int startNode, int nodeCount, Color color) {
            this.currentNode = startNode;
            this.visited = new boolean[nodeCount];
            this.tour = new int[nodeCount];
            this.tourIndex = 0;
            this.tourLength = 0.0;
            this.finished = false;
            this.color = color;

            this.visited[startNode] = true;
            this.tour[this.tourIndex++] = startNode;
        }

        boolean isRunning() {
            return !finished;
        }
    }

    /**
     * Основная логика алгоритма муравьёв: граф, расстояния, ферменты, муравьи.
     */
    static class Simulation {
        private final int nodeCount;
        private final Random random = new Random();
        private Node[] nodes;
        private double[][] distance;
        private double[][] pheromone;
        private List<Ant> ants;
        private int antCount;
        private int iteration = 0;
        private double bestLength = Double.POSITIVE_INFINITY;
        private int[] bestTour = null;

        Simulation(int nodeCount, int antCount) {
            this.nodeCount = nodeCount;
            this.antCount = antCount;
            initAll();
        }

        private void initAll() {
            initNodes();
            initDistances();
            initPheromones();
            initAnts();
            iteration = 0;
            bestLength = Double.POSITIVE_INFINITY;
            bestTour = null;
        }

        private void initNodes() {
            nodes = new Node[nodeCount];
            // Размещаем узлы в [0.1; 0.9] по X и Y, чтобы не прилипали к краям
            for (int i = 0; i < nodeCount; i++) {
                double x = 0.1 + 0.8 * random.nextDouble();
                double y = 0.1 + 0.8 * random.nextDouble();
                nodes[i] = new Node(x, y);
            }
        }

        private void initDistances() {
            distance = new double[nodeCount][nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < nodeCount; j++) {
                    if (i == j) {
                        distance[i][j] = 0.0;
                    } else {
                        double dx = nodes[i].x - nodes[j].x;
                        double dy = nodes[i].y - nodes[j].y;
                        distance[i][j] = Math.sqrt(dx * dx + dy * dy);
                    }
                }
            }
        }

        private void initPheromones() {
            pheromone = new double[nodeCount][nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < nodeCount; j++) {
                    if (i == j) {
                        pheromone[i][j] = 0.0;
                    } else {
                        pheromone[i][j] = INITIAL_PHEROMONE;
                    }
                }
            }
        }

        private void initAnts() {
            ants = new ArrayList<>();
            for (int k = 0; k < antCount; k++) {
                int startNode = k % nodeCount; // равномерное распределение по узлам

                // Случайный, но не слишком тёмный цвет с лёгкой прозрачностью
                int r = 50 + random.nextInt(206);   // 50..255
                int g = 50 + random.nextInt(206);
                int b = 50 + random.nextInt(206);
                Color color = new Color(r, g, b, 220);

                ants.add(new Ant(startNode, nodeCount, color));
            }
        }

        public void reset() {
            initAll();
        }

        public void setAntCount(int antCount) {
            if (antCount <= 0) antCount = 1;
            this.antCount = antCount;
            initAnts();
        }

        public int getNodeCount() {
            return nodeCount;
        }

        public Node getNode(int i) {
            return nodes[i];
        }

        public List<Ant> getAnts() {
            return ants;
        }

        public int getIteration() {
            return iteration;
        }

        public int[] getBestTour() {
            return bestTour;
        }

        public double getBestLength() {
            return bestLength;
        }

        public double getPheromone(int i, int j) {
            return pheromone[i][j];
        }

        public double getMaxPheromone() {
            double max = 0.0;
            for (int i = 0; i < nodeCount; i++) {
                for (int j = i + 1; j < nodeCount; j++) {
                    if (pheromone[i][j] > max) {
                        max = pheromone[i][j];
                    }
                }
            }
            return max;
        }

        public double getDistance(int i, int j) {
            return distance[i][j];
        }

        /**
         * Один шаг симуляции:
         * - если есть муравьи, которые ещё не прошли все узлы — двигаем их на 1 узел.
         * - если все муравьи завершили маршруты — обновляем фермент и порождаем новое поколение муравьёв.
         */
        public void step() {
            boolean allFinishedBefore = true;
            for (Ant ant : ants) {
                if (ant.isRunning()) {
                    allFinishedBefore = false;
                    break;
                }
            }

            // Если вдруг все уже finished, сразу делаем фазу обновления
            if (allFinishedBefore) {
                updatePheromonesAndRestart();
                return;
            }

            // Иначе двигаем каждого муравья на 1 узел (если он ещё не завершил маршрут)
            for (Ant ant : ants) {
                if (ant.isRunning()) {
                    moveAntOneStep(ant);
                }
            }

            // Проверяем, не завершили ли теперь все муравьи свои маршруты
            boolean allFinishedAfter = true;
            for (Ant ant : ants) {
                if (ant.isRunning()) {
                    allFinishedAfter = false;
                    break;
                }
            }

            if (allFinishedAfter) {
                updatePheromonesAndRestart();
            }
        }

        private void moveAntOneStep(Ant ant) {
            if (ant.tourIndex >= nodeCount) {
                ant.finished = true;
                return;
            }

            int current = ant.currentNode;

            // Формируем список доступных (ещё не посещённых) узлов
            List<Integer> allowed = new ArrayList<>();
            for (int u = 0; u < nodeCount; u++) {
                if (!ant.visited[u]) {
                    allowed.add(u);
                }
            }

            if (allowed.isEmpty()) {
                ant.finished = true;
                return;
            }

            int next = selectNextNode(current, allowed);
            if (next == -1) {
                // На всякий случай fallback: выбираем случайный допустимый
                next = allowed.get(new Random().nextInt(allowed.size()));
            }

            double dist = distance[current][next];
            ant.tourLength += dist;
            ant.currentNode = next;
            ant.visited[next] = true;
            ant.tour[ant.tourIndex++] = next;

            if (ant.tourIndex >= nodeCount) {
                ant.finished = true;
            }
        }

        /**
         * Вероятностный выбор следующего узла согласно формуле из ТЗ/PDF.
         */
        private int selectNextNode(int currentNode, List<Integer> allowed) {
            double[] desirability = new double[allowed.size()];
            double sum = 0.0;

            for (int i = 0; i < allowed.size(); i++) {
                int u = allowed.get(i);
                double tau = pheromone[currentNode][u];       // интенсивность фермента τ(r,u)
                double dist = distance[currentNode][u];       // расстояние d(r,u)
                if (dist == 0.0) dist = 1e-6;                 // защита от деления на 0

                double eta = 1.0 / dist; // η(r,u) = 1 / d(r,u)
                double value = Math.pow(tau, ALPHA) * Math.pow(eta, BETA);

                desirability[i] = value;
                sum += value;
            }

            if (sum <= 0.0) {
                // Все значения нулевые — выбираем случайный узел из allowed
                return allowed.get(new Random().nextInt(allowed.size()));
            }

            // Рулетка: вероятностный выбор на основе desirability / sum
            double r = random.nextDouble() * sum;
            double cumulative = 0.0;
            for (int i = 0; i < allowed.size(); i++) {
                cumulative += desirability[i];
                if (r <= cumulative) {
                    return allowed.get(i);
                }
            }

            // На всякий случай (из-за возможных ошибок округления)
            return allowed.getLast();
        }

        /**
         * Обновление фермента согласно PDF:
         * Δτ_ij^k(t) = Q / L_k(t)
         * τ_ij(t+1) = Σ_k Δτ_ij^k(t) + ρ * τ_ij(t)
         */
        private void updatePheromonesAndRestart() {
            // 1) Сохраняем старые значения для формулы τ_new = ρ * τ_old + Σ Δτ
            double[][] oldPheromone = new double[nodeCount][nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                System.arraycopy(pheromone[i], 0, oldPheromone[i], 0, nodeCount);
            }

            // 2) Обнуляем матрицу под новые значения (будем сюда накапливать Σ Δτ)
            for (int i = 0; i < nodeCount; i++) {
                Arrays.fill(pheromone[i], 0.0);
            }

            // 3) Добавляем вклады от всех муравьёв: Σ_k Δτ_ij^k(t) = Σ_k Q / L_k(t) по рёбрам их маршрутов
            for (Ant ant : ants) {
                if (ant.tourIndex < 2) continue; // маршрут слишком короткий

                double contribution = Q / ant.tourLength;

                for (int k = 0; k < nodeCount - 1; k++) {
                    int r = ant.tour[k];
                    int u = ant.tour[k + 1];

                    pheromone[r][u] += contribution;
                    pheromone[u][r] += contribution; // двунаправленный граф
                }

                // Обновление лучшего найденного маршрута
                if (ant.tourLength < bestLength) {
                    bestLength = ant.tourLength;
                    bestTour = Arrays.copyOf(ant.tour, nodeCount);
                }
            }

            // 4) Добавляем компонент "ρ * τ_old" для каждого ребра
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < nodeCount; j++) {
                    pheromone[i][j] += RHO * oldPheromone[i][j];
                }
            }

            // Новое поколение муравьёв
            initAnts();
            iteration++;
        }
    }

    /**
     * Панель отрисовки: граф, ферменты, муравьи, лучший маршрут.
     */
    class GraphPanel extends JPanel {

        GraphPanel() {
            setPreferredSize(new Dimension(900, 650));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Simulation simulation = AntColonyDemo.this.simulation;
            if (simulation == null) return;

            int w = getWidth();
            int h = getHeight();
            int margin = 40;

            int n = simulation.getNodeCount();

            // Преобразуем нормализованные координаты узлов в пиксели
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                Node node = simulation.getNode(i);
                xs[i] = margin + (int) (node.x * (w - 2 * margin));
                ys[i] = margin + (int) (node.y * (h - 2 * margin));
            }

            double maxTau = simulation.getMaxPheromone();

            // 1. Рисуем рёбра с учётом интенсивности фермента + подписи d / τ
            Font originalFont = g2.getFont();
            Font smallFont = originalFont.deriveFont(9f);

            if (maxTau > 0.0) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        double tau = simulation.getPheromone(i, j);
                        if (tau <= 1e-5) continue;   // мелкий порог отрисовки

                        float ratio = (float) (tau / maxTau);
                        if (ratio < 0f) ratio = 0f;
                        if (ratio > 1f) ratio = 1f;

                        // Цвет от синеватого (мало фермента) до красного (много фермента)
                        int red = 50 + (int) (205 * ratio);
                        int blue = 230 - (int) (200 * ratio);
                        int green = 60;
                        Color edgeColor = new Color(red, green, blue, 180);

                        float thickness = 1.0f + 4.0f * ratio;
                        g2.setStroke(new BasicStroke(thickness));
                        g2.setColor(edgeColor);
                        g2.drawLine(xs[i], ys[i], xs[j], ys[j]);

                        // Подписи: длина и феромон на этом ребре (маленькими числами)
                        double dist = simulation.getDistance(i, j);
                        String text = String.format(Locale.US, "%.2f / %.2f", dist, tau);
                        int tx = (xs[i] + xs[j]) / 2;
                        int ty = (ys[i] + ys[j]) / 2;

                        g2.setFont(smallFont);
                        g2.setColor(new Color(0, 0, 0, 170));
                        g2.drawString(text, tx + 2, ty - 2);
                        g2.setFont(originalFont);
                    }
                }
            }

            // 2. Текущие маршруты всех муравьёв (тонкие цветные линии – свой цвет у каждого муравья)
            for (Ant ant : simulation.getAnts()) {
                if (ant.tourIndex <= 1) continue;
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(ant.color);

                int prevIndex = ant.tour[0];
                for (int k = 1; k < ant.tourIndex; k++) {
                    int curIndex = ant.tour[k];
                    g2.drawLine(xs[prevIndex], ys[prevIndex], xs[curIndex], ys[curIndex]);
                    prevIndex = curIndex;
                }
            }

            // 3. Узлы графа (без подписей индексов)
            for (int i = 0; i < n; i++) {
                int x = xs[i];
                int y = ys[i];
                int r = 8;

                // Белый круг
                g2.setColor(Color.WHITE);
                g2.fillOval(x - r, y - r, 2 * r, 2 * r);

                // Чёрный контур
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(x - r, y - r, 2 * r, 2 * r);
            }

            // 4. Сами муравьи — точки своего цвета на текущих узлах
            for (Ant ant : simulation.getAnts()) {
                int idx = ant.currentNode;
                int ax = xs[idx];
                int ay = ys[idx];
                int r = 4;

                g2.setColor(ant.color);
                g2.fillOval(ax - r, ay - r, 2 * r, 2 * r);

                // тонкий чёрный контур для читаемости
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawOval(ax - r, ay - r, 2 * r, 2 * r);
            }

            // 5. Лучший найденный маршрут (ЯРКО-ЗЕЛЁНАЯ линия поверх всех путей)
            int[] bestTour = simulation.getBestTour();
            if (bestTour != null) {
                g2.setColor(new Color(0, 200, 0)); // яркий зелёный без прозрачности
                g2.setStroke(new BasicStroke(4.0f)); // толще других
                for (int k = 0; k < n - 1; k++) {
                    int a = bestTour[k];
                    int b = bestTour[k + 1];
                    g2.drawLine(xs[a], ys[a], xs[b], ys[b]);
                }
            }

            // 6. Информация об итерации и лучшей длине
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.0f));
            String bestText = (simulation.getBestLength() == Double.POSITIVE_INFINITY)
                    ? "-"
                    : String.format(Locale.US, "%.3f", simulation.getBestLength());

            String info = "Итерация: " + simulation.getIteration()
                    + "   Лучшая длина: " + bestText
                    + "   (α=" + ALPHA + ", β=" + BETA + ", ρ=" + RHO + ")";
            g2.drawString(info, 10, 20);

            // 7. Легенда, что что означает
            int lx = 10;
            int ly = 40;
            g2.drawString("Легенда:", lx, ly);

            // Зелёная линия — лучший маршрут
            ly += 15;
            g2.setColor(new Color(0, 200, 0));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawLine(lx, ly, lx + 25, ly);
            g2.setColor(Color.BLACK);
            g2.drawString(" - лучший найденный маршрут (зелёный, поверх всех)", lx + 30, ly + 4);

            // Цветная линия — текущие пути муравьёв
            ly += 15;
            g2.setColor(Color.MAGENTA);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(lx, ly, lx + 25, ly);
            g2.setColor(Color.BLACK);
            g2.drawString(" - текущие маршруты муравьёв (каждый муравей своим цветом)", lx + 30, ly + 4);

            // Цветные рёбра — интенсивность фермента
            ly += 15;
            g2.setColor(new Color(200, 80, 80, 180));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawLine(lx, ly, lx + 25, ly);
            g2.setColor(Color.BLACK);
            g2.drawString(" - рёбра графа, цвет/толщина = количество фермента", lx + 30, ly + 4);

            // Подписи d / τ
            ly += 15;
            g2.setFont(smallFont);
            g2.drawString("d / τ рядом с ребром: d = длина ребра, τ = количество фермента", lx, ly + 4);
            g2.setFont(originalFont);
        }
    }
}
