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
import zone.greggle.gregbot.entity.*;
import zone.greggle.gregbot.mission.MissionSummaryCreator;
import zone.greggle.gregbot.mission.MissionUtil;
import zone.greggle.gregbot.mission.editor.MissionEditorCreator;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;

import java.util.List;
import java.util.Objects;

@Component
public class GuildReactionListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GuildReactionListener.class);

    @Value("${mission.manager.message}")
    String managerMessageID;

    @Value("${mission.publish.category}")
    String missionPublishCategoryID;

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
    MissionSummaryCreator missionSummaryCreator;

    @Autowired
    MissionUtil missionUtil;

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Message message = Objects.requireNonNull(event.retrieveMessage().complete());
        boolean botSentMessage = Objects.requireNonNull(message.getMember()).getUser().isBot();

        if (Objects.requireNonNull(event.getUser()).isBot() || !botSentMessage) return;
        event.getReaction().removeReaction(Objects.requireNonNull(event.getUser())).queue();

        logger.debug("Identifying reaction on message " + event.getMessageId());

//        Reactions on Mission Manager
        if (event.getMessageId().equals(managerMessageID)) {
            handleManagerReaction(event);
            return;
        }

//        Reactions on a Mission Editor
        List<Mission> unpublishedMissions = missionRepository.findByPublishedIs(false);
        for (Mission mission : unpublishedMissions) {
            if (event.getMessageIdLong() ==  mission.getEditorMessageID()) {
                if (handleEditorReaction(event, mission)) return;
            } else if (event.getReaction().getReactionEmote().getAsCodepoints().equals("U+274c") &&
                    event.getTextChannel().getIdLong() == mission.getMissionChannelID()) {
                missionUtil.resetEditMode(mission);
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

    private boolean handleEditorReaction(MessageReactionAddEvent event, Mission mission) {

        if(!Objects.requireNonNull(event.getMember()).getRoles()
                .contains(event.getGuild().getRoleById(missionCreatorRoleID)))
            return false;

        missionEditorUtil.handleReaction(event.getReactionEmote().getAsCodepoints(), mission);
        return true;
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
                missionSummaryCreator.updateSummary(mission);
                logger.info(String.format("%s registered for mission #%s",
                        Objects.requireNonNull(event.getUser()).getName(), mission.getShortID()));
                break;
            case "U+274c": // Cross - unregister user
                List<MissionMember> members = mission.getMembers();
                MissionMember thisMember = mission.getMemberByID(event.getUserIdLong());
                if (thisMember == null) return;

                members.remove(thisMember);
                mission.setMembers(members);

                missionRepository.save(mission);
                missionSummaryCreator.updateSummary(mission);
                logger.info(String.format("%s unregistered for mission #%s",
                        Objects.requireNonNull(event.getUser()).getName(), mission.getShortID()));
                break;
        }
    }

}
