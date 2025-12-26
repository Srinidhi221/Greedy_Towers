package game;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;

// ============================================================================
// MAIN GUI - Towers Puzzle Game (4x4) with 4 Greedy Strategies
// ============================================================================

public class TowersGameGUI extends JFrame {
    private static final int N = 4;
    private static final int[] TOP    = {2, 1, 4, 2};
    private static final int[] RIGHT  = {2, 1, 3, 2};
    private static final int[] BOTTOM = {2,3,1,3};
    private static final int[] LEFT   = {2, 3, 1, 2};

    private GameState gameState;
    private StrategyLives strategyLives;
    private StrategyCompletion strategyCompletion;
    private StrategyScore strategyScore;
    private StrategyMRV strategyMRV;

    private int selectedRow = -1, selectedCol = -1;

    private JButton[][] cellButtons = new JButton[N][N];
    private JButton[] valueButtons = new JButton[N];
    private JLabel statusLabel, humanScoreLabel, humanLivesLabel, cpuScoreLabel, cpuLivesLabel;
    private JPanel valueSelectionPanel;
    private JComboBox<String> strategyCombo;
    private JCheckBox heatMapToggle;
    private JTextArea reasoningArea;

    private enum Strategy {
        LIVES("Lives-Greedy (Survival)"),
        COMPLETION("Completion-Greedy (Rusher)"),
        SCORE("Score-Greedy (Gambler)"),
        MRV("Constraint-Greedy (MRV)");

        private final String name;
        Strategy(String n) { name = n; }
        public String toString() { return name; }
    }

    private Strategy currentStrategy = Strategy.LIVES;
    private boolean showHeatMap = true;
    private double[][] heatMapValues = new double[N][N];

    public TowersGameGUI() {
        setTitle("Towers Puzzle - 4×4 with 4 AI Strategies");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(245, 245, 250));

        initGame();
        initComponents();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        updateDisplay();
    }

    // ============================================================================
    // GAME INITIALIZATION
    // ============================================================================

    private void initGame() {
        gameState = new GameState(TOP, RIGHT, BOTTOM, LEFT);
        strategyLives = new StrategyLives(gameState);
        strategyCompletion = new StrategyCompletion(gameState);
        strategyScore = new StrategyScore(gameState);
        strategyMRV = new StrategyMRV(gameState);
    }

    // ============================================================================
    // GUI COMPONENTS
    // ============================================================================

    private void initComponents() {
        // Top stats panel
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 15, 8));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        humanScoreLabel = createLabel("YOU - Score: 0", new Color(30, 64, 175), new Color(219, 234, 254));
        cpuScoreLabel = createLabel("CPU - Score: 0", new Color(127, 29, 29), new Color(254, 226, 226));
        humanLivesLabel = createLabel("Lives: 100", new Color(16, 185, 129), new Color(209, 250, 229));
        cpuLivesLabel = createLabel("Lives: 100", new Color(239, 68, 68), new Color(254, 226, 226));

        topPanel.add(humanScoreLabel);
        topPanel.add(cpuScoreLabel);
        topPanel.add(humanLivesLabel);
        topPanel.add(cpuLivesLabel);
        add(topPanel, BorderLayout.NORTH);

        // Game board
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Top clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = 0;
            boardPanel.add(createClue(TOP[i]), gbc);
        }

        // Board with left/right clues
        for (int r = 0; r < N; r++) {
            gbc.gridx = 0; gbc.gridy = r + 1;
            boardPanel.add(createClue(LEFT[r]), gbc);

            for (int c = 0; c < N; c++) {
                final int row = r, col = c;
                JButton btn = new JButton("");
                btn.setPreferredSize(new Dimension(90, 90));  // Larger for 4x4
                btn.setFont(new Font("Arial", Font.BOLD, 36));
                btn.setBackground(Color.WHITE);
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createLineBorder(new Color(180,180,180), 2));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> handleCellClick(row, col));
                cellButtons[r][c] = btn;
                gbc.gridx = c + 1; gbc.gridy = r + 1;
                boardPanel.add(btn, gbc);
            }

            gbc.gridx = N + 1; gbc.gridy = r + 1;
            boardPanel.add(createClue(RIGHT[r]), gbc);
        }

        // Bottom clues
        for (int i = 0; i < N; i++) {
            gbc.gridx = i + 1; gbc.gridy = N + 1;
            boardPanel.add(createClue(BOTTOM[i]), gbc);
        }
        add(boardPanel, BorderLayout.CENTER);

        // Right control panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        rightPanel.setPreferredSize(new Dimension(300, 0));

        JLabel stratLabel = new JLabel("CPU Strategy:");
        stratLabel.setFont(new Font("Arial", Font.BOLD, 14));
        stratLabel.setAlignmentX(LEFT_ALIGNMENT);

        strategyCombo = new JComboBox<>();
        for (Strategy s : Strategy.values()) strategyCombo.addItem(s.toString());
        strategyCombo.setMaximumSize(new Dimension(280, 35));
        strategyCombo.setAlignmentX(LEFT_ALIGNMENT);
        strategyCombo.addActionListener(e -> {
            currentStrategy = Strategy.values()[strategyCombo.getSelectedIndex()];
            updateHeatMap();
            updateDisplay();
        });

        heatMapToggle = new JCheckBox("Show Heat Map", true);
        heatMapToggle.setFont(new Font("Arial", Font.BOLD, 13));
        heatMapToggle.setOpaque(false);
        heatMapToggle.setAlignmentX(LEFT_ALIGNMENT);
        heatMapToggle.addActionListener(e -> {
            showHeatMap = heatMapToggle.isSelected();
            updateDisplay();
        });

        JButton resetBtn = new JButton("New Game");
        resetBtn.setFont(new Font("Arial", Font.BOLD, 15));
        resetBtn.setBackground(new Color(79, 70, 229));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFocusPainted(false);
        resetBtn.setMaximumSize(new Dimension(280, 45));
        resetBtn.setAlignmentX(LEFT_ALIGNMENT);
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetBtn.addActionListener(e -> resetGame());

        rightPanel.add(stratLabel);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(strategyCombo);
        rightPanel.add(Box.createVerticalStrut(12));
        rightPanel.add(heatMapToggle);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(resetBtn);
        rightPanel.add(Box.createVerticalStrut(25));

        // CPU Reasoning Area
        JLabel reasonLabel = new JLabel("CPU Reasoning:");
        reasonLabel.setFont(new Font("Arial", Font.BOLD, 14));
        reasonLabel.setAlignmentX(LEFT_ALIGNMENT);

        reasoningArea = new JTextArea(14, 25);
        reasoningArea.setEditable(false);
        reasoningArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reasoningArea.setLineWrap(true);
        reasoningArea.setWrapStyleWord(true);
        reasoningArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        reasoningArea.setText("Select a strategy and watch the CPU think...");

        JScrollPane reasonScroll = new JScrollPane(reasoningArea);
        reasonScroll.setMaximumSize(new Dimension(280, 220));
        reasonScroll.setAlignmentX(LEFT_ALIGNMENT);

        rightPanel.add(reasonLabel);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(reasonScroll);
        rightPanel.add(Box.createVerticalStrut(20));

        // Value selection panel
        valueSelectionPanel = new JPanel();
        valueSelectionPanel.setLayout(new BoxLayout(valueSelectionPanel, BoxLayout.Y_AXIS));
        valueSelectionPanel.setOpaque(false);
        valueSelectionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(59,130,246), 3),
            "Select Value", 0, 0, new Font("Arial", Font.BOLD, 14), new Color(59,130,246)));
        valueSelectionPanel.setVisible(false);

        JPanel valGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        valGrid.setOpaque(false);
        for (int i = 0; i < N; i++) {
            final int val = i + 1;
            JButton btn = new JButton(String.valueOf(val));
            btn.setPreferredSize(new Dimension(70, 70));
            btn.setFont(new Font("Arial", Font.BOLD, 32));
            btn.setBackground(new Color(79, 70, 229));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleValueClick(val));
            valueButtons[i] = btn;
            valGrid.add(btn);
        }

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 13));
        cancelBtn.setBackground(new Color(239, 68, 68));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setMaximumSize(new Dimension(280, 40));
        cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> {
            selectedRow = -1; selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            updateDisplay();
        });

        valueSelectionPanel.add(Box.createVerticalStrut(10));
        valueSelectionPanel.add(valGrid);
        valueSelectionPanel.add(Box.createVerticalStrut(15));
        valueSelectionPanel.add(cancelBtn);
        valueSelectionPanel.add(Box.createVerticalStrut(10));

        rightPanel.add(valueSelectionPanel);
        add(rightPanel, BorderLayout.EAST);

        // Bottom status
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 20, 15));
        statusLabel = new JLabel("Your turn! Click an empty cell.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String txt, Color fg, Color bg) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        l.setForeground(fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg.brighter(), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        return l;
    }

    private JLabel createClue(int v) {
        JLabel l = new JLabel(String.valueOf(v), SwingConstants.CENTER);
        l.setFont(new Font("Arial", Font.BOLD, 22));
        l.setForeground(new Color(79, 70, 229));
        l.setPreferredSize(new Dimension(50, 50));
        return l;
    }

    // ============================================================================
    // USER INTERACTION
    // ============================================================================

    private void handleCellClick(int r, int c) {
        if (!gameState.isHumanTurn() || gameState.isGameOver() || gameState.getGrid()[r][c] != 0) {
            return;
        }
        selectedRow = r;
        selectedCol = c;
        showValueSelection();
        updateDisplay();
    }

    private void showValueSelection() {
        for (int i = 0; i < N; i++) {
            int val = i + 1;
            //boolean legal = !gameState.getGraph().hasConflict(gameState.getGrid(), selectedRow, selectedCol, val);
            valueButtons[i].setEnabled(true);
            valueButtons[i].setBackground(new Color(79, 70, 229));
        }
        valueSelectionPanel.setVisible(true);
        statusLabel.setText("Choose a value for cell (" + (selectedRow+1) + "," + (selectedCol+1) + ")");
    }

//    private void handleValueClick(int val) {
//        if (selectedRow == -1) return;
//
//        gameState.makeMove(selectedRow, selectedCol, val, true);
//        selectedRow = -1;
//        selectedCol = -1;
//        valueSelectionPanel.setVisible(false);
//        gameState.setHumanTurn(false);
//        updateDisplay();
//
//        if (checkGameEnd()) return;
//
//        Timer delay = new Timer(600, e -> {
//            updateHeatMap();
//            animateHeatMap(0);
//        });
//        delay.setRepeats(false);
//        delay.start();
//    }
    
    
//    private void handleValueClick(int val) {
//        if (selectedRow == -1) return;
//
//        // ⭐ ADD THIS: Check for deadlock BEFORE allowing move
//        if (gameState.checkForDeadlock(true)) {
//            statusLabel.setText("You have no legal moves! -5 lives, skipping turn");
//            selectedRow = -1;
//            selectedCol = -1;
//            valueSelectionPanel.setVisible(false);
//            gameState.setHumanTurn(false);
//            updateDisplay();
//            
//            Timer delay = new Timer(1500, e -> {
//                if (!checkGameEnd()) {
//                    updateHeatMap();
//                    animateHeatMap(0);
//                }
//            });
//            delay.setRepeats(false);
//            delay.start();
//            return;
//        }
//
//        // Rest of existing code...
//        gameState.makeMove(selectedRow, selectedCol, val, true);
//        selectedRow = -1;
//        selectedCol = -1;
//        valueSelectionPanel.setVisible(false);
//        gameState.setHumanTurn(false);
//        updateDisplay();
//
//        if (checkGameEnd()) return;
//
//        Timer delay = new Timer(600, e -> {
//            updateHeatMap();
//            animateHeatMap(0);
//        });
//        delay.setRepeats(false);
//        delay.start();
//    }
    
    
    private void handleValueClick(int val) {
        if (selectedRow == -1) return;

        // Check for deadlock BEFORE allowing move
        if (gameState.checkForDeadlock(true)) {
            statusLabel.setText("You have no legal moves! -5 lives, skipping turn");
            selectedRow = -1;
            selectedCol = -1;
            valueSelectionPanel.setVisible(false);
            gameState.setHumanTurn(false);
            updateDisplay();
            
            Timer delay = new Timer(1500, e -> {
                if (!checkGameEnd()) {
                    updateHeatMap();
                    animateHeatMap(0);
                }
            });
            delay.setRepeats(false);
            delay.start();
            return;
        }

        // ⭐ CRITICAL FIX: Capture whether move was accepted
        boolean moveAccepted = gameState.makeMove(selectedRow, selectedCol, val, true);
        
        // Clear selection
        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        
        // ⭐ ONLY switch turns if move was valid (not rejected)
        if (moveAccepted) {
            gameState.setHumanTurn(false);
        }
        
        updateDisplay();

        if (checkGameEnd()) return;

        // ⭐ Only proceed to CPU turn if move was accepted
        if (moveAccepted) {
            Timer delay = new Timer(600, e -> {
                updateHeatMap();
                animateHeatMap(0);
            });
            delay.setRepeats(false);
            delay.start();
        }
    }

    // ============================================================================
    // HEAT MAP ANIMATION & COLORS
    // ============================================================================

    private void animateHeatMap(int idx) {
        if (idx >= N * N) {
            Timer delay = new Timer(1200, e -> {
                clearHeatMap();
                if (!gameState.isGameOver()) {
                    doCPUMove();
                    gameState.setHumanTurn(true);
                    updateDisplay();
                    checkGameEnd();
                }
            });
            delay.setRepeats(false);
            delay.start();
            return;
        }

        int r = idx / N, c = idx % N;
        if (gameState.getGrid()[r][c] == 0 && showHeatMap) {
            cellButtons[r][c].setBackground(getHeatColor(heatMapValues[r][c]));
        }

        Timer t = new Timer(70, e -> animateHeatMap(idx + 1));
        t.setRepeats(false);
        t.start();
    }

    private Color getHeatColor(double h) {
        if (h < 0.01) return Color.WHITE;

        double ratio = Math.min(h, 1.0);

        return switch (currentStrategy) {
            case LIVES ->       new Color(34 + (int)(151 * ratio), 185 + (int)(31 * ratio), 95 + (int)(125 * ratio));     // Green
            case COMPLETION ->  new Color(239 + (int)(15 * ratio), 68 + (int)(82 * ratio), 68 + (int)(82 * ratio));       // Red
            case SCORE ->       new Color(255, 165 + (int)(90 * ratio), 0);                                               // Gold → Orange
            case MRV ->         new Color(130 + (int)(56 * ratio), 39, 144 + (int)(64 * ratio));                          // Purple
        };
    }

    private void clearHeatMap() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (gameState.getGrid()[r][c] == 0) {
                    cellButtons[r][c].setBackground(
                        (r == selectedRow && c == selectedCol) ? new Color(191, 219, 254) : Color.WHITE
                    );
                }
            }
        }
    }

    // ============================================================================
    // CPU MOVE
    // ============================================================================

    private void doCPUMove() {
    	
    	
        if (gameState.checkForDeadlock(false)) {
            statusLabel.setText("CPU has no legal moves! -5 lives, skipping turn");
            gameState.setHumanTurn(true);
            updateDisplay();
            return;
        }
        int[] move = switch (currentStrategy) {
            case LIVES -> strategyLives.findBestMove();
            case COMPLETION -> strategyCompletion.findBestMove();
            case SCORE -> strategyScore.findBestMove();
            case MRV -> strategyMRV.findBestMove();
        };

        if (move == null) {
            gameState.setStatusMessage("CPU has no valid moves!");
            updateDisplay();
            return;
        }

        reasoningArea.setText(gameState.getCpuReasoningExplanation());
        gameState.makeMove(move[0], move[1], move[2], false);
    }

    // ============================================================================
    // HEAT MAP CALCULATION
    // ============================================================================

    private void updateHeatMap() {
        double max = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (gameState.getGrid()[r][c] == 0) {
                    double score = switch (currentStrategy) {
                        case LIVES -> strategyLives.evaluateCell(r, c);
                        case COMPLETION -> strategyCompletion.evaluateCell(r, c);
                        case SCORE -> strategyScore.evaluateCell(r, c);
                        case MRV -> strategyMRV.evaluateCell(r, c);
                    };
                    heatMapValues[r][c] = score;
                    max = Math.max(max, score);
                } else {
                    heatMapValues[r][c] = 0;
                }
            }
        }

        if (max > 0) {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    heatMapValues[r][c] /= max;
                }
            }
        }
    }

    // ============================================================================
    // DISPLAY UPDATE
    // ============================================================================

    private void updateDisplay() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                JButton b = cellButtons[r][c];
                int val = gameState.getGrid()[r][c];
                if (val != 0) {
                    b.setText(String.valueOf(val));
                    b.setBackground(new Color(79, 70, 229));
                    b.setForeground(Color.WHITE);
                    b.setEnabled(false);
                } else {
                    b.setText("");
                    if (r == selectedRow && c == selectedCol) {
                        b.setBackground(new Color(191, 219, 254));
                        b.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 4));
                    } else {
                        b.setBackground(showHeatMap && heatMapValues[r][c] > 0.01 ?
                            getHeatColor(heatMapValues[r][c]) : Color.WHITE);
                        b.setBorder(BorderFactory.createLineBorder(new Color(180,180,180), 2));
                    }
                    b.setEnabled(gameState.isHumanTurn() && !gameState.isGameOver());
                }
            }
        }

        humanScoreLabel.setText("YOU - Score: " + gameState.getHumanScore());
        humanLivesLabel.setText("Lives: " + gameState.getHumanLives());
        cpuScoreLabel.setText("CPU - Score: " + gameState.getCpuScore());
        cpuLivesLabel.setText("Lives: " + gameState.getCpuLives());

        String msg = gameState.getStatusMessage();
        if (!msg.isEmpty()) {
            statusLabel.setText(msg);
        } else if (gameState.isHumanTurn()) {
            statusLabel.setText(selectedRow == -1 ? "Your turn! Click a cell." : "Select a value");
        } else {
            statusLabel.setText("CPU thinking (" + currentStrategy + ")...");
        }
    }

    // ============================================================================
    // GAME END & RESET
    // ============================================================================

    private boolean checkGameEnd() {
        if (gameState.isGameOver()) {
            String winner = gameState.getWinner();
            if (winner == null) winner = "Game Over";

            statusLabel.setText(winner);

            String msg = "═══ GAME OVER ═══\n\n" +
                         winner + "\n\n" +
                         "Final Stats:\n" +
                         "YOU → Score: " + gameState.getHumanScore() + " | Lives: " + gameState.getHumanLives() + "\n" +
                         "CPU → Score: " + gameState.getCpuScore() + " | Lives: " + gameState.getCpuLives();

            JOptionPane.showMessageDialog(this, msg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private void resetGame() {
        initGame();
        selectedRow = -1;
        selectedCol = -1;
        valueSelectionPanel.setVisible(false);
        reasoningArea.setText("Select a strategy and watch the CPU think...");
        updateHeatMap();
        updateDisplay();
        statusLabel.setText("New game started! Your turn.");
    }

    // ============================================================================
    // MAIN
    // ============================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TowersGameGUI().setVisible(true));
    }
}