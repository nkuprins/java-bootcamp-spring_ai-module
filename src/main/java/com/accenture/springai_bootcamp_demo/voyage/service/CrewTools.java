package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipSystem;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Announce;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Move;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Repair;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Rest;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Say;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * The tools one crew member gets for one turn. They only check the rules and
 * remember the chosen action; the ship is changed later by Java, after the
 * model call. Whatever a tool returns is read by the model, so refusals say
 * why and what to do instead.
 */
class CrewTools {

    // Annotation texts must be compile-time constants, so the room list is written out
    private static final String ROOMS = "bridge, engineering, hydroponics bay, medbay, comms array, mess hall, crew quarters";
    private static final int MAX_MESSAGE_LENGTH = 300;

    // A written call starts with the tool name, maybe after `call`, `{` or `"name":`; "Did they say..." is just speech.
    // Possessive quantifiers (*+) never give characters back, so a failed match can't backtrack
    private static final Pattern WRITTEN_TOOL_NAME = Pattern.compile(
            "^\\s*+(?:call\\s++)?\\{?\\s*+(?:\"name\"\\s*+:\\s*+)?\"?(moveTo|say|announce|repair|rest)\\b",
            Pattern.CASE_INSENSITIVE);
    // Only finds where a named value starts; the value itself is read by quotedValue, so no repeated alternation
    private static final Pattern WRITTEN_ARGUMENT_KEY = Pattern.compile("\"(?:room|system|message)\"\\s*+:\\s*+\"");

    private final CrewSituation situation;
    private CrewAction chosen;
    private String lastRefusal;

    CrewTools(CrewSituation situation) {
        this.situation = situation;
    }

    Optional<CrewAction> chosenAction() {
        return Optional.ofNullable(chosen);
    }

    Optional<String> lastRefusal() {
        return Optional.ofNullable(lastRefusal);
    }

    @Tool(description = "Walk to another room. Uses your action for this turn.")
    public String moveTo(@ToolParam(description = "The room to go to, one of: " + ROOMS) String room) {
        Optional<Location> destination = parse(Location.class, room, Location::label);
        if (destination.isEmpty()) {
            return refuse("There is no room called '" + room + "'. Rooms: " + ROOMS + ".");
        }
        if (destination.get() == situation.location()) {
            return refuse("You are already in " + situation.location().label() + ". Choose something else.");
        }
        return choose(new Move(destination.get()), "You walk to " + destination.get().label() + ".");
    }

    @Tool(description = "Say something out loud to the people in your room. Uses your action for this turn.")
    public String say(@ToolParam(description = "What you say, one or two sentences") String message) {
        if (situation.isAlone()) {
            return refuse("Nobody is here to hear you. Move to another room, or do something else.");
        }
        if (message == null || message.isBlank()) {
            return refuse("You opened your mouth but said nothing. Give the message you want to say.");
        }
        if (situation.wouldRepeat(message)) {
            return refuse("You said exactly that last time. Say something new, or do something else.");
        }
        return choose(new Say(shorten(message)), "You said it. Everyone in the room heard you.");
    }

    @Tool(description = "Captain only: speak to the whole crew over the ship's intercom. Uses your action for this turn.")
    public String announce(@ToolParam(description = "Your announcement, one or two sentences") String message) {
        if (!situation.who().isCaptain()) {
            return refuse("Only the captain may use the intercom. Talk to people in person instead.");
        }
        if (message == null || message.isBlank()) {
            return refuse("The intercom crackles, but you said nothing. Give the announcement you want to make.");
        }
        if (situation.wouldRepeat(message)) {
            return refuse("You announced exactly that last time. Say something new, or do something else.");
        }
        return choose(new Announce(shorten(message)), "Your voice carries through every room of the ship.");
    }

    @Tool(description = "Repair a ship system. Hull and power can only be repaired in engineering, "
            + "food and oxygen only in the hydroponics bay. Uses your action for this turn.")
    public String repair(@ToolParam(description = "The system to repair: hull, power, food or oxygen") String system) {
        Optional<ShipSystem> target = parse(ShipSystem.class, system, ShipSystem::label);
        if (target.isEmpty()) {
            return refuse("There is no system called '" + system + "'. Systems: hull, power, food, oxygen.");
        }
        Location workshop = target.get().repairLocation();
        if (workshop != situation.location()) {
            return refuse("You can only repair the " + target.get().label() + " in " + workshop.label()
                    + ". Call moveTo to go there first.");
        }
        if (situation.ship().level(target.get()) >= Voyage.MAX_LEVEL) {
            return refuse("The " + target.get().label() + " is already in perfect condition. Do something else.");
        }
        return choose(new Repair(target.get()), "You spend the hour repairing the " + target.get().label() + ".");
    }

    @Tool(description = "Rest for the hour. Uses your action for this turn.")
    public String rest() {
        return choose(new Rest(), "You rest.");
    }

    /**
     * Small models often write a tool call as text instead of making it, e.g.
     * {@code repair {"system": "hull"}} or {@code {"name": say, "parameters": {"message": "..."}}}.
     * If the reply starts like that, run the same tool, so the same rules apply.
     *
     * @return what the tool answered, or empty if the reply doesn't look like a tool call
     */
    Optional<String> followWrittenCall(String reply) {
        Matcher toolName = WRITTEN_TOOL_NAME.matcher(reply);
        if (!toolName.find()) {
            return Optional.empty();
        }
        String argument = writtenArgument(reply.substring(toolName.end()));
        return Optional.of(switch (toolName.group(1).toLowerCase()) {
            case "moveto" -> moveTo(argument);
            case "say" -> say(argument);
            case "announce" -> announce(argument);
            case "repair" -> repair(argument);
            default -> rest();
        });
    }

    // Prefer a named value like "room": "engineering"; otherwise take the words after the tool name
    private static String writtenArgument(String afterToolName) {
        Matcher key = WRITTEN_ARGUMENT_KEY.matcher(afterToolName);
        if (key.find()) {
            return quotedValue(afterToolName, key.end());
        }
        return afterToolName.replaceAll("[{}\"]", "").replaceFirst("^[\\s,:]+", "").strip();
    }

    // Reads up to the closing quote; a backslash keeps the next character, so \" stays part of the value
    private static String quotedValue(String text, int start) {
        StringBuilder value = new StringBuilder();
        int i = start;
        while (i < text.length() && text.charAt(i) != '"') {
            if (text.charAt(i) == '\\' && i + 1 < text.length()) {
                i++;
            }
            value.append(text.charAt(i));
            i++;
        }
        return value.toString();
    }

    private String choose(CrewAction action, String result) {
        if (chosen != null) {
            return "You have already acted this turn. Do not call more tools.";
        }
        chosen = action;
        return result;
    }

    private String refuse(String reason) {
        lastRefusal = reason;
        return reason;
    }

    private static String shorten(String message) {
        String trimmed = message.replaceAll("\\s+", " ").strip();
        return trimmed.length() <= MAX_MESSAGE_LENGTH ? trimmed : trimmed.substring(0, MAX_MESSAGE_LENGTH);
    }

    // Small models write "the Bridge", "BRIDGE" or "comms_array", so compare loosely against name and label
    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String text, Function<E, String> label) {
        String wanted = normalize(text);
        return Arrays.stream(type.getEnumConstants())
                .filter(value -> wanted.equals(normalize(value.name())) || wanted.equals(normalize(label.apply(value))))
                .findFirst();
    }

    private static String normalize(String text) {
        return text.strip().toLowerCase().replace('_', ' ').replaceFirst("^the ", "");
    }
}
