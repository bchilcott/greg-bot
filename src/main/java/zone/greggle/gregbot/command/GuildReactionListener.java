package zone.greggle.gregbot.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.data.EditMode;
import zone.greggle.gregbot.entity.*;
import zone.greggle.gregbot.mission.MissionUtil;
import zone.greggle.gregbot.mission.editor.MissionEditorCreator;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;
import zone.greggle.gregbot.mission.summary.MissionSummaryUtil;
import zone.greggle.gregbot.scheduling.DeleteScheduler;

import java.util.List;
import java.util.Objects;

@Component
public class GuildReactionListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GuildReactionListener.class);

    @Value("${mission.manager.message}")
    String managerMessageID;

    @Value("${mission.creator.role}")
    String missionCreatorRoleID;

    @Autowired
    JDAContainer jdaContainer;

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    SubscriberRepository subscriberRepository;

    @Autowired
    MissionEditorCreator missionEditorCreator;

    @Autowired
    MissionEditorUtil missionEditorUtil;

    @Autowired
    MissionSummaryUtil missionSummaryUtil;

    @Autowired
    MissionUtil missionUtil;

    @Autowired
    DeleteScheduler deleteScheduler;

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Message message = Objects.requireNonNull(event.retrieveMessage().complete());
        if (message.getMember() == null) {
            logger.trace("Ignoring reaction on irrelevant message.");
            return;
        }

        boolean botSentMessage = message.getMember().getUser().isBot();

        if (Objects.requireNonNull(event.getUser()).isBot() || !botSentMessage) return;
        event.getReaction().removeReaction(Objects.requireNonNull(event.getUser())).queue();

//        Reactions on Mission Manager
        if (event.getMessageId().equals(managerMessageID)) {
            handleManagerReaction(event);
            return;
        }

//        Reactions on a Mission Editor
        List<Mission> unpublishedMissions = missionRepository.findByPublishedIs(false);
        for (Mission mission : unpublishedMissions) {


            if (event.getMessageIdLong() ==  mission.getEditorMessageID()) {
                if(Objects.requireNonNull(event.getMember()).getRoles().contains(event.getGuild().getRoleById(missionCreatorRoleID))) {
                    missionEditorUtil.handleReaction(event.getReactionEmote().getAsCodepoints(), mission);
                } else {
                    missionEditorUtil.sendErrorMessage("Invalid Permissions",
                            "You need the Mission Creator role to do this!",
                            event.getTextChannel());
                }
                return;
            } else if (event.getReaction().getReactionEmote().getAsCodepoints().equals("U+274c") &&
                    event.getTextChannel().getIdLong() == mission.getMissionChannelID()) {
                missionUtil.resetEditMode(mission);
                return;
            }

//            Reactions on Role Editor
            if (mission.getEditMode() == EditMode.ROLES && event.getMessageIdLong() == mission.getLastPromptID()) {
                switch (event.getReaction().getReactionEmote().getAsCodepoints()) {
                    case "U+2b05":
                        if (mission.getAvailableRoles().size() > 0) mission.removeLastRole();
                        missionRepository.save(mission);
                        missionEditorCreator.updateEditorMessage(mission);
                        break;

                    case "U+2705":
                        missionUtil.resetEditMode(mission);
                        break;
                }
                return;
            }

//            Reactions on Image Editor
            if (mission.getEditMode() == EditMode.IMAGE && event.getMessageIdLong() == mission.getLastPromptID()) {
                switch (event.getReaction().getReactionEmote().getAsCodepoints()) {
                    case "U+1f5d1":
                        mission.setImage(null);
                        missionUtil.resetEditMode(mission);
                        missionEditorCreator.updateEditorMessage(mission);
                        break;

                    case "U+274c":
                        missionUtil.resetEditMode(mission);
                        break;
                }
                return;
            }
        }

//        Reactions on a Mission Summary
        List<Mission> publishedMissions = missionRepository.findByPublishedIs(true);
        for (Mission mission : publishedMissions) {
            long summaryMessageID = mission.getSummaryMessageID();
            if (event.getMessageIdLong() == summaryMessageID) {
                handleSummaryReaction(event, mission);
                return;
            }
        }

    }

    private void handleManagerReaction(MessageReactionAddEvent event) {
        Subscriber existingSubscriber = subscriberRepository.findByDiscordID(Objects.requireNonNull(event.getUser()).getIdLong());
        String emoteCode = event.getReaction().getReactionEmote().getAsCodepoints();

        switch (emoteCode) {
            case "U+1f4dd": // Notepad and Pencil
                Role creatorRole = event.getGuild().getRoleById(missionCreatorRoleID);
                if (Objects.requireNonNull(event.getMember()).getRoles().contains(creatorRole)) {
                    Mission mission = new Mission(event.getUser().getIdLong());
                    missionRepository.save(mission);
                    deleteScheduler.scheduleDelete(mission);
                    missionEditorCreator.createMissionEditor(mission);

                    logger.info(String.format("%s created mission #%s", event.getUser().getName(), mission.getShortID()));
                } else {
                    missionEditorUtil.sendErrorMessage("Invalid Permissions",
                            "You need the Mission Creator role to do this!",
                            event.getTextChannel());
                }
                break;
            case "U+1f514": // Notification Bell
                if (existingSubscriber == null) {
                    subscriberRepository.save(new Subscriber(event.getUser().getIdLong()));
                    event.getUser().openPrivateChannel().queue(channel -> {
                        channel.sendMessage("You are now **subscribed** to new mission alerts!")
                                .queue();
                    });
                    logger.info(event.getUser().getName() + " subscribed to alerts");
                }
                break;
            case "U+1f515": // Notification Cross
                if (existingSubscriber != null) {
                    subscriberRepository.deleteByDiscordID(existingSubscriber.getDiscordID());
                    event.getUser().openPrivateChannel().queue(channel -> {
                        channel.sendMessage("You have now **unsubscribed** from new mission alerts.")
                                .queue();
                    });
                    logger.info(event.getUser().getName() + " unsubscribed from alerts");
                }
                break;
        }
    }

    private void handleSummaryReaction(MessageReactionAddEvent event, Mission mission) {
        String emoteCode = event.getReaction().getReactionEmote().getAsCodepoints();

        switch (emoteCode) {
            case "U+2705": // Tick - register user
                for (MissionMember m : mission.getMembers()) {
                    if (event.getUserIdLong() == m.getDiscordID()) {
                        return;
                    }
                }
                mission.addMember(new MissionMember(event.getUserIdLong()));
                missionRepository.save(mission);
                missionSummaryUtil.updateSummary(mission);
                logger.info(String.format("%s registered for mission #%s",
                        Objects.requireNonNull(event.getUser()).getName(), mission.getShortID()));
                break;

            case "U+274c": // Cross - unregister user
                mission.removeMember(mission.getMemberByID(event.getUserIdLong()));
                missionRepository.save(mission);
                missionSummaryUtil.updateSummary(mission);
                logger.info(String.format("%s unregistered for mission #%s",
                        Objects.requireNonNull(event.getUser()).getName(), mission.getShortID()));
                break;

            case "U+2694": // Crossed Swords - Select Role
                for (Mission missionToCheck : missionRepository.findMissionsByMemberDiscordID(event.getUserIdLong())) {
                    if (missionToCheck.getMemberByID(event.getUserIdLong()).isSelectingRole()) {
                        missionToCheck.setSelectingRole(event.getUserIdLong(), false);
                        missionRepository.save(missionToCheck);
                    }
                }
                missionSummaryUtil.createRoleSelector(mission, event.getMember());
        }
    }

}
