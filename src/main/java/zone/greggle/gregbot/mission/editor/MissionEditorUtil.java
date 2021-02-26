package zone.greggle.gregbot.mission.editor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.data.EditMode;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.MissionSummaryCreator;
import zone.greggle.gregbot.scheduling.AlertScheduler;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class MissionEditorUtil {

    private static final Logger logger = LoggerFactory.getLogger(MissionEditorUtil.class);

    @Autowired
    private JDAContainer jdaContainer;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    MissionSummaryCreator missionSummaryCreator;

    @Autowired
    AlertScheduler alertScheduler;

    public void handleReaction(String reactionCode, Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;

        boolean deleted = false;
        TextChannel missionChannel = Objects.requireNonNull(guild.getTextChannelById(mission.getMissionChannelID()));
        logger.debug("Handling reaction code " + reactionCode);

        if (!reactionCode.equals("U+274c") && !reactionCode.equals("U+2705")) {
            if (mission.getEditMode() != EditMode.NONE) resetEditMode(mission);
        }

        switch (reactionCode) {
            case "U+1f4ac": // Title
                mission.setEditMode(EditMode.NAME);
                sendEditPrompt(":speech_balloon:  Edit Mission Name", "Type a new name for the mission:",
                        mission);
                break;

            case "U+1f4c6": // Date and Time
                mission.setEditMode(EditMode.START_DATE);
                sendEditPrompt(":date:  Edit Start Date and Time (UTC)", "Type a new date and time" +
                        "for the mission, in the format `DD/MM/YYYY HH:MM`:", mission);
                break;

            case "U+1f5fa": // Location
                mission.setEditMode(EditMode.LOCATION);
                sendEditPrompt(":map:  Edit Mission Location", "Type a new location for the mission:",
                        mission);
                break;

            case "U+1f4d6": // Summary
                mission.setEditMode(EditMode.SUMMARY);
                sendEditPrompt(":book:  Edit Mission Summary", "Type a new short summary for the mission." +
                        "More details can be sent to this channel once the mission is published:",
                        mission);
                break;

            case "U+274c": // Cancel
                missionChannel.delete().queue();
                missionRepository.deleteByShortID(mission.getShortID());
                logger.info("Deleted " + mission.getName() + " #" + mission.getShortID());
                deleted = true;
                break;

            case "U+2705": // Publish
                try {
                    missionChannel.retrieveMessageById(mission.getEditorMessageID()).queue(m -> m.delete().queue());
                    if (mission.getLastPromptID() != null && mission.getEditMode() != EditMode.NONE) { // If prompt is open
                        missionChannel.retrieveMessageById(mission.getLastPromptID()).queue(m -> m.delete().queue());
                    }
                    missionChannel.putPermissionOverride(guild.getPublicRole()).setAllow(Permission.VIEW_CHANNEL).queue();
                    missionSummaryCreator.sendSummary(mission);
                    alertScheduler.registerNewAlert(mission);

                    mission.setEditMode(EditMode.NONE);
                    mission.setPublished(true);
                    logger.info("Published " + mission.getName() + " #" + mission.getShortID());

                } catch (Exception e) {
                    sendErrorMessage("Error Publishing Mission",
                            "```" + e.getMessage() + "```", missionChannel);
                    logger.error("Error publishing mission #" + mission.getShortID(), e);
                }
                break;
        }

        if (!deleted) {
            missionRepository.save(mission);
            logger.debug(String.format("Edit mode set to %s on mission #%s", mission.getEditMode().name(),  mission.getShortID()));
        }
    }

    public void resetEditMode(Mission mission) {
        mission.setEditMode(EditMode.NONE);
        missionRepository.save(mission);
        Objects.requireNonNull(jdaContainer.getGuild().getTextChannelById(mission.getMissionChannelID()))
            .retrieveMessageById(mission.getLastPromptID()).queue(m -> {
                m.delete().queue();
            });
    }

    private void sendEditPrompt(String heading, String prompt, Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;
        TextChannel missionChannel = Objects.requireNonNull(guild.getTextChannelById(mission.getMissionChannelID()));

        EmbedBuilder eb = new EmbedBuilder().setTitle(heading).setDescription(prompt);
        missionChannel.sendMessage(eb.build()).queue(m -> {
            m.addReaction("U+274c").queue();
            mission.setLastPromptID(m.getIdLong());
            missionRepository.save(mission);
        });
    }

    public void sendErrorMessage(String heading, String message, TextChannel channel) {
        EmbedBuilder eb = new EmbedBuilder().setTitle("Error: " + heading).setDescription(message).setColor(Color.RED);
        channel.sendMessage(eb.build()).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
    }
}
