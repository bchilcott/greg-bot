package zone.greggle.gregbot.mission.summary;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionMember;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.entity.SubscriberRepository;
import zone.greggle.gregbot.mission.MissionUtil;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class MissionSummaryUtil {

    private static final Logger logger = LoggerFactory.getLogger(MissionSummaryUtil.class);

    @Autowired
    private JDAContainer jdaContainer;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    MissionRepository missionRepository;
    
    @Autowired
    MissionEditorUtil missionEditorUtil;

    @Autowired
    MissionUtil missionUtil;

    public void sendSummary(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild !=  null;

        TextChannel missionChannel = guild.getTextChannelById(mission.getMissionChannelID());
        assert missionChannel != null;

        Message summary = missionChannel.sendMessage(createSummaryEmbed(mission)).complete();
        addReactions(summary, mission.getAvailableRoles().size() > 0);
        mission.setSummaryMessageID(summary.getIdLong());
    }

    public void updateSummary(Mission mission) {
        Message summaryMessage = Objects.requireNonNull(jdaContainer.getGuild()
                .getTextChannelById(mission.getMissionChannelID()))
                .retrieveMessageById(mission.getSummaryMessageID()).complete();
        summaryMessage.editMessage(createSummaryEmbed(mission)).queue();
    }
    
    public void createRoleSelector(Mission mission, Member applicant) {
        MissionMember registeredMember = null;
        for (MissionMember member : mission.getMembers()) {
            if (member.getDiscordID().equals(applicant.getIdLong())) {
                registeredMember = member;
            }
        }
        if (registeredMember != null) {
            applicant.getUser().openPrivateChannel().queue(dm -> {
                EmbedBuilder eb = new EmbedBuilder();
                StringBuilder descBuilder = new StringBuilder();
                descBuilder.append("Type the name of a role from the list below:");
                descBuilder.append("```");
                descBuilder.append(missionUtil.buildRolesString(mission));
                descBuilder.append("```");

                eb.setTitle("Role Selection - " + mission.getName());
                eb.setDescription(descBuilder.toString());
                eb.setFooter("GREG Bot by Scythern#5601");
                dm.sendMessage(eb.build()).queue(message -> {
                        mission.setSelectingRole(applicant.getIdLong(), true);
                        missionRepository.save(mission);
                        logger.info("Sent role selector to " + applicant.getUser().getName());
                });
            });
        }
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
                    mList.append("\n - ").append(member.getUser().getName());
                    if (!missionMember.getMissionRole().equals("Not Selected"))
                        mList.append(String.format(" (%s)", missionMember.getMissionRole()));
                } else {
                    members.remove(missionMember);
                }
            }
        } else {
            mList.append("This mission has no registered attendees!");
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

        StringBuilder optionsBuilder = new StringBuilder();
        optionsBuilder.append("✅ - Attending");
        optionsBuilder.append("\n❌ - Not Attending");
        if (mission.getAvailableRoles().size() > 0) optionsBuilder.append("\n⚔ - Select Role");

        eb.addField("Register Attendance",
                optionsBuilder.toString(),
                false);
        if (mission.getImage() != null) eb.setImage(mission.getImage());
        eb.setFooter("GREG Bot by @Scythern#5601");
        return eb.build();
    }

    private static void addReactions(Message message, boolean addRoleSelect) {
        message.addReaction("U+2705").queue();
        message.addReaction("U+274c").queue();
        if (addRoleSelect) message.addReaction("U+2694").queue();
    }
}
