package zone.greggle.gregbot.mission;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class MissionSummaryCreator {

    private static final Logger logger = LoggerFactory.getLogger(MissionSummaryCreator.class);

    @Autowired
    private JDAContainer jdaContainer;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    MissionRepository missionRepository;

    public void sendSummary(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild !=  null;

        TextChannel missionChannel = guild.getTextChannelById(mission.getMissionChannelID());
        assert missionChannel != null;

        Message summary = missionChannel.sendMessage(createSummaryEmbed(mission)).complete();
        addReactions(summary);
        mission.setSummaryMessageID(summary.getIdLong());
        sendMissionAlerts(mission);
    }

    public void updateSummary(Mission mission) {
        Message summaryMessage = Objects.requireNonNull(jdaContainer.getGuild()
                .getTextChannelById(mission.getMissionChannelID()))
                .retrieveMessageById(mission.getSummaryMessageID()).complete();
        summaryMessage.editMessage(createSummaryEmbed(mission)).queue();
    }

    private MessageEmbed createSummaryEmbed(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild !=  null;

        StringBuilder mList = new StringBuilder();
        List<MissionMember> members = mission.getMembers();
        if (members.size() > 0) {
            mList.append(String.format("Registered Attendees (%s):", members.size()));
            for (MissionMember missionMember : members) {
                Member member  = guild.retrieveMemberById(missionMember.getDiscordID()).complete();
                if (member != null) {
                    mList.append("\n - ");
                    mList.append(member.getUser().getName());
                } else {
                    members.remove(missionMember);
                }
            }
        } else {
            mList.append("\nThis mission has no registered attendees!");
        }


        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(mission.getName() + " (#" + mission.getShortID() + ")");
        eb.appendDescription("Created by " + guild.retrieveMemberById(mission.getHostID()).complete().getAsMention());
        eb.appendDescription("```" +
                String.format("\nMission Name: %s", mission.getName()) +
                String.format("\nStart Time (UTC): %s", mission.getMissionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))) +
                String.format("\nLocation: %s", mission.getLocation()) +
                "\n\nMission Summary:" +
                String.format("\n%s", mission.getSummary()) + "\n\n" +
                mList.toString() +
                "```" );
        eb.addField("Register Attendance",
                "✅ - Attending" +
                "\n❌ - Not Attending",
                false);
        eb.setFooter("GREG Bot by @Scythern#5601");
        return eb.build();
    }

    private static void addReactions(Message message) {
        message.addReaction("U+2705").queue();
        message.addReaction("U+274c").queue();
    }

    private void sendMissionAlerts(Mission mission) {
        logger.info("Sending mission alert to subscribers");
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
                        });
                    });
                }
            });
        }
    }
}
