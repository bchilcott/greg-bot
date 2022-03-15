package zone.greggle.gregbot.mission;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.data.EditMode;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.entity.Subscriber;
import zone.greggle.gregbot.entity.SubscriberRepository;
import zone.greggle.gregbot.mission.editor.MissionEditorCreator;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;
import zone.greggle.gregbot.mission.summary.MissionSummaryUtil;
import zone.greggle.gregbot.scheduling.MissionAlertScheduler;
import zone.greggle.gregbot.scheduling.MissionEndScheduler;
import zone.greggle.gregbot.scheduling.MissionStartScheduler;

import java.util.Objects;

@Component
public class MissionUtil {

    private static final Logger logger = LoggerFactory.getLogger(MissionUtil.class);

    @Autowired
    JDAContainer jdaContainer;

    @Autowired
    MissionSummaryUtil missionSummaryUtil;

    @Autowired
    MissionAlertScheduler missionAlertScheduler;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    MissionEditorUtil missionEditorUtil;

    @Autowired
    MissionEditorCreator missionEditorCreator;

    @Autowired
    SubscriberRepository subscriberRepository;

    @Autowired
    MissionEndScheduler missionEndScheduler;

    @Autowired
    MissionStartScheduler missionStartScheduler;

    public void publishMission(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;

        TextChannel missionChannel = Objects.requireNonNull(guild.getTextChannelById(mission.getMissionChannelID()));

        missionChannel.retrieveMessageById(mission.getEditorMessageID()).queue(m -> m.delete().queue());
        if (mission.getLastPromptID() != null && mission.getEditMode() != EditMode.NONE) { // If prompt is open
            missionChannel.retrieveMessageById(mission.getLastPromptID()).queue(m -> m.delete().queue());
        }
        missionChannel.putPermissionOverride(guild.getPublicRole())
                .setAllow(Permission.VIEW_CHANNEL)
                .setDeny(Permission.MESSAGE_WRITE)
                .queue();
        missionSummaryUtil.sendSummary(mission);
        missionAlertScheduler.registerNewAlert(mission);
        missionStartScheduler.schedulePresence(mission);
        missionEndScheduler.scheduleDelete(mission);

        mission.setEditMode(EditMode.NONE);
        if (!mission.wasPreviouslyPublished()) sendPublishAlerts(mission);

        mission.setPublished(true);
        logger.info("Published " + mission.getName() + " #" + mission.getShortID());
    }

    public void resetEditMode(Mission mission) {
        mission.setEditMode(EditMode.NONE);
        missionRepository.save(mission);
        Objects.requireNonNull(jdaContainer.getGuild().getTextChannelById(mission.getMissionChannelID()))
                .retrieveMessageById(mission.getLastPromptID()).queue(m -> {
            m.delete().queue();
        });
    }

    public void deleteMission(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        Presence presence = jdaContainer.getJDA().getPresence();
        assert guild != null;

        TextChannel missionChannel = Objects.requireNonNull(guild.getTextChannelById(mission.getMissionChannelID()));

        missionAlertScheduler.unregisterAlert(mission);
        missionEndScheduler.unregisterDelete(mission);
        missionStartScheduler.unregisterTask(mission);
        missionRepository.deleteById(mission.getID());
        missionChannel.delete().queue();
        presence.setActivity(null);
        logger.info("Deleted " + mission.getName() + " #" + mission.getShortID());
    }

    public void editMission(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;

        mission.setPublished(false);
        missionRepository.save(mission);

        TextChannel missionChannel = Objects.requireNonNull(guild.getTextChannelById(mission.getMissionChannelID()));
        missionChannel.retrieveMessageById(mission.getSummaryMessageID()).queue(m -> m.delete().queue());
        missionEditorCreator.sendEditorMessage(mission, missionChannel);
        logger.info("Editing " + mission.getName() + " #" + mission.getShortID());
    }

    public String buildRolesString(Mission mission) {
        String roles = "";
        if (mission.getAvailableRoles().size() > 0) {
            StringBuilder roleListBuilder = new StringBuilder();
            roleListBuilder.append("\n\nAvailable Roles:");
            for (String role : mission.getAvailableRoles()) {
                roleListBuilder.append("\n - ").append(role);
            }
            roles = roleListBuilder.toString();
        }
        return roles;
    }

    private void sendPublishAlerts(Mission mission) {
        for (Subscriber subscriber : subscriberRepository.findAll()) {
            if (subscriber.getDiscordID().equals(mission.getHostID())) return;
            jdaContainer.getGuild().retrieveMemberById(subscriber.getDiscordID()).queue(subMember -> {
                if (subMember == null) {
                    subscriberRepository.deleteByDiscordID(subscriber.getDiscordID());
                } else {
                    subMember.getUser().openPrivateChannel().queue(dm -> {
                        jdaContainer.getJDA().retrieveUserById(mission.getHostID()).queue(host -> {
                            dm.sendMessage(String.format(
                                    "A new mission was published by %s: %s.",
                                    host.getName(),
                                    mission.getName()
                            )).queue();
                            logger.debug("Sending mission alert to " + subMember.getUser().getName());
                        });
                    });
                }
            });
        }
    }
}
