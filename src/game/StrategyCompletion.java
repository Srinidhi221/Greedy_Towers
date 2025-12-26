package game;

import java.util.*;

public class StrategyCompletion {
 private GameState state;

 public StrategyCompletion(GameState state) {
     this.state = state;
 }

// public int[] findBestMove() {
//     int size = state.getSize();
//     double bestScore = -Double.MAX_VALUE;
//     int bestRow = -1, bestCol = -1;
//     String bestExplanation = "";
//
//     for (int r = 0; r < size; r++) {
//         for (int c = 0; c < size; c++) {
//             if (state.getGrid()[r][c] == 0) {
//                 CellEvaluation eval = evaluateCompletion(r, c);
//                 if (eval.score > bestScore) {
//                     bestScore = eval.score;
//                     bestRow = r;
//                     bestCol = c;
//                     bestExplanation = eval.explanation;
//                 }
//             }
//         }
//     }
//
//     if (bestRow == -1) return null;
//
//     int bestValue = findLegalValue(bestRow, bestCol);
//     if (bestValue == -1) return null;
//
//     state.setCpuReasoningExplanation(bestExplanation);
//     return new int[]{bestRow, bestCol, bestValue};
// }
 
 
 public int[] findBestMove() {
	    int size = state.getSize();
	    double bestScore = -Double.MAX_VALUE;
	    int bestRow = -1, bestCol = -1, bestValue = -1;
	    String bestExplanation = "";

	    // Check all cells
	    for (int r = 0; r < size; r++) {
	        for (int c = 0; c < size; c++) {
	            if (state.getGrid()[r][c] == 0) {
	                CellEvaluation eval = evaluateCompletion(r, c);
	                
	                // ⭐ NEW: Try ALL legal values for this cell, pick best one
	                for (int v = 1; v <= size; v++) {
	                    if (!state.getGraph().hasConflict(state.getGrid(), r, c, v)) {
	                        // Calculate visibility risk for this specific value
	                        double visibilityRisk = calculateVisibilityRisk(r, c, v);
	                        
	                        // ⭐ GREEDY FLAW: Still prioritize completion, but subtract risk
	                        // (Should avoid risk entirely, but greedy = locally optimal)
	                        double adjustedScore = eval.score - (visibilityRisk * 0.3);  // Only 30% penalty
	                        
	                        if (adjustedScore > bestScore) {
	                            bestScore = adjustedScore;
	                            bestRow = r;
	                            bestCol = c;
	                            bestValue = v;
	                            
	                            // Update explanation with risk info
	                            bestExplanation = String.format(
	                                "【COMPLETION GREEDY】\n" +
	                                "════════════════════════════\n" +
	                                "📍 Cell: (%d,%d) = %d\n" +
	                                "📏 Row empty: %d → priority %.1f\n" +
	                                "📏 Col empty: %d → priority %.1f\n" +
	                                "🎯 Completion bonus: %.1f\n" +
	                                "⚠️  Visibility risk: %.1f\n" +
	                                "📈 ADJUSTED SCORE: %.1f\n" +
	                                "════════════════════════════\n" +
	                                "STRATEGY: Rush to complete!\n" +
	                                "%s",
	                                r, c, v,
	                                state.countEmptyInRow(r), 100.0 / (state.countEmptyInRow(r) + 1),
	                                state.countEmptyInColumn(c), 100.0 / (state.countEmptyInColumn(c) + 1),
	                                eval.score - adjustedScore + visibilityRisk * 0.3,
	                                visibilityRisk,
	                                adjustedScore,
	                                visibilityRisk > 0 ? "⚠️ HIGH PENALTY RISK!" : "✓ Safe move"
	                            );
	                        }
	                    }
	                }
	            }
	        }
	    }

	    if (bestRow == -1 || bestValue == -1) return null;

	    state.setCpuReasoningExplanation(bestExplanation);
	    return new int[]{bestRow, bestCol, bestValue};
	}

 private CellEvaluation evaluateCompletion(int row, int col) {
     int emptyRow = state.countEmptyInRow(row);
     int emptyCol = state.countEmptyInColumn(col);

     double rowPriority = 100.0 / (emptyRow + 1);
     double colPriority = 100.0 / (emptyCol + 1);
     double bonus = 0.0;
     if (emptyRow == 1) bonus += 50.0;
     if (emptyCol == 1) bonus += 50.0;
     if (emptyRow == 1 && emptyCol == 1) bonus += 100.0;

     double finalScore = rowPriority + colPriority + bonus;

     String explanation = String.format(
         "【COMPLETION GREEDY】\n" +
         "════════════════════════════\n" +
         "📍 Cell: (%d,%d)\n" +
         "📏 Row empty: %d → priority %.1f\n" +
         "📏 Col empty: %d → priority %.1f\n" +
         "🎯 Completion bonus: %.1f\n" +
         "📈 TOTAL SCORE: %.1f\n" +
         "════════════════════════════\n" +
         "STRATEGY: Rush to finish rows & columns!",
         row, col, emptyRow, rowPriority, emptyCol, colPriority, bonus, finalScore
     );

     return new CellEvaluation(row, col, finalScore, explanation);
 }
 
 /**
  * Check if placing a value will complete a row/column with WRONG clues
  * Returns: penalty risk score (higher = more dangerous)
  */
 private double calculateVisibilityRisk(int row, int col, int value) {
     double risk = 0.0;
     
     // Simulate placing the value
     int[][] grid = state.getGrid();
     int originalValue = grid[row][col];
     grid[row][col] = value;
     
     // Check if row would be complete
     boolean rowComplete = state.isRowComplete(row);
     if (rowComplete) {
         // Will this violate visibility clues?
         if (!state.validateRowVisibility(row)) {
             risk += 15.0;  // -15 lives penalty risk!
         }
     }
     
     // Check if column would be complete
     boolean colComplete = state.isColumnComplete(col);
     if (colComplete) {
         // Will this violate visibility clues?
         if (!state.validateColumnVisibility(col)) {
             risk += 15.0;  // -15 lives penalty risk!
         }
     }
     
     // Restore original value
     grid[row][col] = originalValue;
     
     return risk;
 }


 public double evaluateCell(int row, int col) {
     if (state.getGrid()[row][col] != 0) return 0.0;
     return evaluateCompletion(row, col).score;
 }
}