package de.gamingkaetzchen.synccord.tickets;

import java.util.List;

import de.gamingkaetzchen.synccord.Synccord;
import de.gamingkaetzchen.synccord.util.Lang;

public class TicketQuestion {

    private final int inputLimit;
    private final List<String> questions;

    public TicketQuestion(int inputLimit, List<String> questions) {

        // Debug-Ausgabe bei Erstellung
        if (isDebug()) {
            Synccord.getInstance().getLogger().info(
                    Lang.get("debug_ticket_question_created")
                            .replace("%limit%", String.valueOf(inputLimit))
                            .replace("%questions%", questions.toString())
            );
        }

        this.inputLimit = inputLimit;
        this.questions = questions;
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
