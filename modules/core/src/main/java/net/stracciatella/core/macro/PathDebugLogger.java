package net.stracciatella.core.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A simple logger to output bot movement data to a CSV file for analysis.
 * Creates a new file in the main Minecraft directory for each run.
 */
public class PathDebugLogger {

    private BufferedWriter writer;
    private long tickCounter = 0;

    /**
     * Starts a new logging session. Creates a file and writes the header and initial path data.
     * @param startPos The starting position of the bot.
     * @param goalPos The final destination.
     * @param plannedPath The full list of BlockPos the bot intends to follow.
     */
    public void startLogging(BlockPos startPos, BlockPos goalPos, List<BlockPos> plannedPath) {
        // Create a unique filename using a timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String fileName = "path_log_" + timestamp + ".csv";

        try {
            // The file will be created in the root Minecraft directory (e.g., .minecraft/)
            writer = new BufferedWriter(new FileWriter(fileName));

            // Write the CSV header
            writer.write("Tick;Type;X;Y;Z\n");

            // Log the start and goal positions
            logPosition("START", startPos.getX(), startPos.getY(), startPos.getZ());
            logPosition("GOAL", goalPos.getX(), goalPos.getY(), goalPos.getZ());

            // Log the entire planned path for reference
            for (BlockPos pos : plannedPath) {
                logPosition("PLANNED_PATH", pos.getX(), pos.getY(), pos.getZ());
            }

        } catch (IOException e) {
            System.err.println("Error initializing PathDebugLogger: " + e.getMessage());
            writer = null; // Ensure writer is null if setup fails
        }
    }

    /**
     * Logs the bot's current position and its immediate target for a single game tick.
     * @param player The player entity.
     * @param currentTarget The current BlockPos the bot is moving towards.
     */
    public void logTick(LocalPlayer player, BlockPos currentTarget) {
        if (writer == null) return; // Don't do anything if the logger failed to initialize

        try {
            // Log the bot's precise current position
            logPosition("BOT_POSITION", player.getX(), player.getY(), player.getZ());
            // Log the target it's currently trying to reach
            logPosition("CURRENT_TARGET", currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
            tickCounter++;
        } catch (IOException e) {
            System.err.println("Error writing to path log: " + e.getMessage());
        }
    }

    /**
     * Closes the file writer to ensure all data is saved.
     */
    public void stopLogging() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("Error closing path log: " + e.getMessage());
            }
        }
    }

    /**
     * Helper method to format and write a line to the CSV file.
     * Uses Locale.US to ensure the decimal separator is a period (.), not a comma.
     */
    private void logPosition(String type, double x, double y, double z) throws IOException {
        writer.write(String.format(Locale.US, "%d;%s;%.4f;%.4f;%.4f\n", tickCounter, type, x, y, z));
    }
}
