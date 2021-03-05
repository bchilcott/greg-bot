package zone.greggle.gregbot.mission;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.JDAContainer;

import java.awt.*;
import java.util.EnumSet;
import java.util.Objects;

@Component
public class MissionManagerUtil {

    private static final Logger logger = LoggerFactory.getLogger(MissionManagerUtil.class);

    public TextChannel replyChannel;

    @Value("${mission.manager.message}")
    String managerMessageID;

    @Value("${mission.manager.channel}")
    String managerChannelID;

    @Autowired
    JDAContainer jdaContainer;

    public void create(Guild guild, TextChannel replyChannel) {
        this.replyChannel = replyChannel;

        Role creatorRole = guild.createRole().setName("Mission Creator").complete();
        TextChannel managerChannel = createManagerChannel(guild);
        Category missionCategory = guild.createCategory("Missions").setPosition(999).complete();

        managerChannel.sendMessage(createManagerEmbed()).queue(m -> {
            m.addReaction("📝").queue();
            m.addReaction("🔔").queue();
            m.addReaction("🔕").queue();

            createConfirmation(replyChannel, missionCategory, m, creatorRole);
        });
    }

    public void updateManagerMessage(Guild guild) {
        if (managerMessageID != null) {
            try {
                TextChannel managerChannel = Objects.requireNonNull(guild.getTextChannelById(managerChannelID));
                managerChannel.editMessageById(managerMessageID, createManagerEmbed()).queue();
            } catch (NullPointerException e) {
                logger.error("Cannot update Mission Manager as it doesn't exist");
            }
        }
    }

    private TextChannel createManagerChannel(Guild guild) {
        return guild.createTextChannel("mission-manager")
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_WRITE))
                .setParent(guild.createCategory("GREG BOT").setPosition(0).complete())
                .complete();
    }

    private MessageEmbed createManagerEmbed() {
        EmbedBuilder managerBuilder = new EmbedBuilder();

        managerBuilder.setTitle("Mission Manager");
        managerBuilder.setColor(Color.ORANGE);
        managerBuilder.setDescription(
                "Select an option from below:" +
                "```" +
                "\n📝 - Create a New Mission" +
                "\n🔔 - Subscribe to Mission Alerts" +
                "\n🔕 - Unsubscribe from Mission Alerts" +
                "```"
        );

        return managerBuilder.build();
    }

    private void createConfirmation(TextChannel replyChannel, Category category, Message managerMessage, Role creatorRole) {
        EmbedBuilder confBuilder = new EmbedBuilder();
        confBuilder.setTitle("Manager Created 🚀");
        confBuilder.setDescription("You must now restart the bot with the following settings to complete setup:" +
                "```" +
                "\nGuild ID:           " + replyChannel.getGuild().getId() +
                "\nCategory ID:        " + category.getId() +
                "\nManager ID:         " + managerMessage.getId() +
                "\nCreator Role ID:    " + creatorRole.getId() +
                "\nManager Channel ID: " + managerMessage.getTextChannel().getId() +
                "```");
        replyChannel.sendMessage(confBuilder.build()).queue();
    }

}
