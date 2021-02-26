package zone.greggle.gregbot.mission;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.EnumSet;

@Component
public class MissionManagerUtil {

    public Guild guild;
    public TextChannel replyChannel;

    public void create(Guild guild, TextChannel replyChannel) {
        this.guild = guild;
        this.replyChannel = replyChannel;

        Role creatorRole = this.guild.createRole().setName("Mission Creator").complete();
        TextChannel managerChannel = createManagerChannel();
        Message managerMessage = createManagerMessage(managerChannel);
        Category missionCategory = guild.createCategory("Missions").setPosition(999).complete();
        createConfirmation(replyChannel, missionCategory, managerMessage, creatorRole);
    }

    private TextChannel createManagerChannel() {
        return this.guild.createTextChannel("mission-manager")
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.MESSAGE_WRITE))
                .setParent(this.guild.createCategory("GREG BOT").setPosition(0).complete())
                .complete();
    }

    private Message createManagerMessage(TextChannel managerChannel) {
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

        Message message = managerChannel.sendMessage(managerBuilder.build()).complete();
        message.addReaction("📝").queue();
        message.addReaction("🔔").queue();
        message.addReaction("🔕").queue();
        return message;
    }

    private void createConfirmation(TextChannel replyChannel, Category category, Message managerMessage, Role creatorRole) {
        EmbedBuilder confBuilder = new EmbedBuilder();
        confBuilder.setTitle("Manager Created 🚀");
        confBuilder.setDescription("You must now restart the bot with the following settings to complete setup:" +
                "```" +
                "\nGuild ID:        " + replyChannel.getGuild().getId() +
                "\nCategory ID:     " + category.getId() +
                "\nManager ID:      " + managerMessage.getId() +
                "\nCreator Role ID: " + creatorRole.getId() +
                "```");
        replyChannel.sendMessage(confBuilder.build()).queue();
    }

}
