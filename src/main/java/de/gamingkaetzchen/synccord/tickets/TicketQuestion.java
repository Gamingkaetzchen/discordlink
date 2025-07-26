package de.gamingkaetzchen.synccord.tickets;

import java.util.List;

public class TicketQuestion {

    private final int inputLimit;
    private final List<String> questions;

    public TicketQuestion(int inputLimit, List<String> questions) {
        this.inputLimit = inputLimit;
        this.questions = questions;
    }

    public int getInputLimit() {
        return inputLimit;
    }

    public List<String> getQuestions() {
        return questions;
    }
}
