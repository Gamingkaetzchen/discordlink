package de.gamingkaetzchen.synccord.tickets;

import java.util.Collections;
import java.util.List;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;

public class TicketQuestion {

    private final int inputLimit;
    private final List<String> questions;

    public TicketQuestion(int inputLimit, List<String> questions) {

        // Sicherheit: null vermeiden
        this.questions = questions != null ? questions : Collections.emptyList();
        this.inputLimit = Math.max(1, inputLimit); // Minimum 1 Zeichen erzwingen

        // Debug-Ausgabe
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_ticket_question_created")
                            .replace("%limit%", String.valueOf(this.inputLimit))
                            .replace("%questions%", String.join(" | ", this.questions))
            );
        }
    }

    public int getInputLimit() {
        return inputLimit;
    }

    public List<String> getQuestions() {
        return questions;
    }

    private boolean isDebug() {
        return Synccord.getInstance().getConfig().getBoolean("debug", false);
    }
}
