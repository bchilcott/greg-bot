package zone.greggle.gregbot.mission.editor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;

@Component
public class MissionEditorCreator {

    private static final Logger logger = LoggerFactory.getLogger(MissionEditorCreator.class);

    @Autowired
    MissionRepository missionRepository;

    @Autowired
    JDAContainer jdaContainer;

    @Value("${mission.publish.category}")
    private Long categoryID;

    @Value("${mission.creator.role}")
    private Long creatorRole;


    public void createMissionEditor(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;

        TextChannel missionChannel = guild.createTextChannel(mission.name)
                .setParent(guild.getCategoryById(categoryID))
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(Objects.requireNonNull(guild.getRoleById(creatorRole)), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .complete();

        missionChannel.sendMessage(createEditorEmbed(mission)).queue(m -> {
            addReactions(m);
            mission.setEditorMessageID(m.getIdLong());
            mission.setMissionChannelID(m.getTextChannel().getIdLong());
            missionRepository.save(mission);
            logger.debug("Mission editor created");
        });
    }

    private MessageEmbed createEditorEmbed(Mission mission) {
        Guild guild = jdaContainer.getGuild();
        assert guild != null;

        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(mission.getName() + " (#" + mission.getShortID() + ")");

        String description = "Created by " + guild.retrieveMemberById(mission.getHostID()).complete().getAsMention() +
                "\nUse the reactions below to change mission settings:" +
                "```" +
                String.format("\nMission Name: %s", mission.getName()) +
                String.format("\nStart Time (UTC): %s", convertLocalDateTimeToString(mission.getMissionDate())) +
                String.format("\nLocation: %s", mission.getLocation()) +
                "\n\nMission Summary:" +
                String.format("\n%s", mission.getSummary()) +
                "```";

        String editFields = ":speech_balloon: Mission Name" +
                "\n:date: Mission Date/Time" +
                "\n:map: Location" +
                "\n:book: Summary";

        String publishFields = ":x: Cancel" +
                "\n :white_check_mark: Publish";

        eb.setDescription(description);
        eb.addField("Edit", editFields, true);
        eb.addField("Create", publishFields, true);
        eb.setFooter("GREG Bot by @Scythern#5601");

        return eb.build();
    }

    public void updateEditorMessage(Mission mission) {
        Message editorMessage = Objects.requireNonNull(jdaContainer.getGuild()
                .getTextChannelById(mission.getMissionChannelID()))
                .retrieveMessageById(mission.getEditorMessageID()).complete();
        editorMessage.editMessage(createEditorEmbed(mission)).queue();
    }

    private static void addReactions(Message message) {
        message.addReaction("U+1f4ac").queue(); // Speech Balloon
        message.addReaction("U+1f4c6").queue(); // Date
        message.addReaction("U+1f5fa").queue(); // Map
        message.addReaction("U+1f4d6").queue(); // Book (open)
        message.addReaction("U+274c").queue(); // Red Cross
        message.addReaction("U+2705").queue(); // Check Mark
    }

    private static String convertLocalDateTimeToString(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

}
