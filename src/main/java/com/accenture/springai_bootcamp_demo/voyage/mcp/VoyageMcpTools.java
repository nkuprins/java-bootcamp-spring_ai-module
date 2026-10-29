package com.accenture.springai_bootcamp_demo.voyage.mcp;

import com.accenture.springai_bootcamp_demo.voyage.dto.InterviewDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.TransmissionDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipEvent;
import com.accenture.springai_bootcamp_demo.voyage.service.InterviewService;
import com.accenture.springai_bootcamp_demo.voyage.service.MemoryService;
import com.accenture.springai_bootcamp_demo.voyage.service.ShipSimulation;
import com.accenture.springai_bootcamp_demo.voyage.service.TransmissionService;
import com.accenture.springai_bootcamp_demo.voyage.service.VoyageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * Mission Control's powers as MCP tools, so any MCP client (Claude Code, an
 * IDE, another agent) can play Mission Control. Like the REST controller this
 * is only an adapter: every tool calls the same service, so the same rules apply.
 */
@Component
@RequiredArgsConstructor
public class VoyageMcpTools {

    private static final int DEFAULT_LOG_ENTRIES = 20;
    private static final int RECALLED_MEMORIES = 5;
    private static final int MAX_TEXT_LENGTH = 500;

    private final VoyageService voyageService;
    private final ShipSimulation shipSimulation;
    private final TransmissionService transmissionService;
    private final InterviewService interviewService;
    private final MemoryService memoryService;

    @McpTool(name = "startVoyage", description = "Launch a new voyage of the generation ship Long Voyage with its "
            + "six crew members. Returns the voyage id that every other tool needs.")
    public VoyageDto startVoyage() {
        return voyageService.createVoyage();
    }

    @McpTool(name = "getShipStatus", description = "The ship's turn, oxygen, power, hull and food (0-100), whether "
            + "the voyage is lost, and where each crew member is.",
            annotations = @McpAnnotations(readOnlyHint = true))
    public VoyageDto getShipStatus(@McpToolParam(description = "Voyage id") String voyageId) {
        return voyageService.getVoyage(voyageId);
    }

    @McpTool(name = "readShipLog", description = "The latest entries of the ship's log: status, events, crew "
            + "actions, private thoughts and the narrator's summary of each hour.",
            annotations = @McpAnnotations(readOnlyHint = true))
    public List<LogEntryDto> readShipLog(
            @McpToolParam(description = "Voyage id") String voyageId,
            @McpToolParam(description = "How many of the latest entries to return, default 20", required = false)
            Integer lastEntries) {
        List<LogEntryDto> log = voyageService.getLog(voyageId);
        int count = lastEntries == null || lastEntries < 1 ? DEFAULT_LOG_ENTRIES : lastEntries;
        return log.subList(Math.max(0, log.size() - count), log.size());
    }

    @McpTool(name = "runTurn", description = "Let one hour pass: the ship uses up oxygen and food, delayed "
            + "transmissions arrive, every crew member acts, and the hour is logged. Takes 10-30 seconds.")
    public ShipUpdateDto runTurn(@McpToolParam(description = "Voyage id") String voyageId) {
        return shipSimulation.runTurn(voyageId);
    }

    @McpTool(name = "triggerEvent", description = "Make something happen on the ship. Crew in that room see it, "
            + "everyone else hears the alarm.")
    public ShipUpdateDto triggerEvent(
            @McpToolParam(description = "Voyage id") String voyageId,
            @McpToolParam(description = "HULL_BREACH, POWER_FAILURE, CROP_BLIGHT or STRANGE_SIGNAL") ShipEvent type,
            @McpToolParam(description = "Room where it happens; defaults to the room where it fits best",
                    required = false) Location location) {
        return shipSimulation.triggerEvent(voyageId, type, location);
    }

    @McpTool(name = "sendTransmission", description = "Send a message from Earth. It arrives "
            + "after 4 turns and only the communications officer reads it; whether others learn it is up to the crew.")
    public TransmissionDto sendTransmission(
            @McpToolParam(description = "Voyage id") String voyageId,
            @McpToolParam(description = "The message, at most 500 characters") String message) {
        return transmissionService.send(voyageId, requireText(message, "message", MAX_TEXT_LENGTH));
    }

    @McpTool(name = "interviewCrewMember", description = "Ask one crew member a question over a private channel. "
            + "They answer in character from what they know, may evade or lie, and remember the conversation.")
    public InterviewDto interviewCrewMember(
            @McpToolParam(description = "Voyage id") String voyageId,
            @McpToolParam(description = "Crew member id, from getShipStatus") Long crewMemberId,
            @McpToolParam(description = "The question, at most 500 characters") String question) {
        return interviewService.interview(voyageId, crewMemberId, requireText(question, "question", MAX_TEXT_LENGTH));
    }

    @McpTool(name = "recallMemories", description = "What a crew member would remember about a topic, best first, "
            + "with the relevance, recency and importance behind each memory's score.",
            annotations = @McpAnnotations(readOnlyHint = true))
    public List<MemoryDto> recallMemories(
            @McpToolParam(description = "Voyage id") String voyageId,
            @McpToolParam(description = "Crew member id, from getShipStatus") Long crewMemberId,
            @McpToolParam(description = "The topic to remember, e.g. 'the hull breach'") String topic) {
        String query = requireText(topic, "topic", MAX_TEXT_LENGTH);
        return memoryService.getMemories(voyageId, crewMemberId, query, RECALLED_MEMORIES);
    }

    // The REST endpoints get this from @Valid on their request records; MCP calls skip that layer
    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not exceed " + maxLength + " characters");
        }
        return value;
    }
}
