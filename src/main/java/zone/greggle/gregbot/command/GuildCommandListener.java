package zone.greggle.gregbot.command;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zone.greggle.gregbot.data.EditMode;
import zone.greggle.gregbot.entity.Mission;
import zone.greggle.gregbot.entity.MissionRepository;
import zone.greggle.gregbot.mission.MissionManagerUtil;
import zone.greggle.gregbot.mission.MissionUtil;
import zone.greggle.gregbot.mission.editor.MissionEditorCreator;
import zone.greggle.gregbot.mission.editor.MissionEditorUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Component
public class GuildCommandListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GuildCommandListener.class);

    @Value("${mission.setup.required}")
    private Boolean setupRequired;

    @Value("${mission.publish.category}")
    private String  missionPublishCategory;

    @Autowired
    private MissionManagerUtil missionManagerUtil;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private MissionEditorCreator missionEditorCreator;

    @Autowired
    private MissionEditorUtil missionEditorUtil;

    @Autowired
    private MissionUtil missionUtil;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String[] messageArgs = event.getMessage().getContentRaw().split(" ");

        if (messageArgs[0].equalsIgnoreCase("!delete")) {
            TextChannel channel = event.getTextChannel();
            Mission mission = missionRepository.findByMissionChannelID(channel.getIdLong());
            if (event.getAuthor().getIdLong() != mission.getHostID()) {
                missionEditorUtil.sendErrorMessage("Invalid Permissions",
                        "You need to be the mission creator to do that!",
                        channel);
                return;
            }
            missionUtil.deleteMission(mission);
        }

        if (messageArgs[0].equalsIgnoreCase("!edit")) {
            event.getMessage().delete().queue();

            TextChannel channel = event.getTextChannel();
            Mission mission = missionRepository.findByMissionChannelID(channel.getIdLong());

            if (!mission.isPublished()) return;

            if (event.getAuthor().getIdLong() != mission.getHostID()) {
                missionEditorUtil.sendErrorMessage("Invalid Permissions",
                        "You need to be the mission creator to do that!",
                        channel);
                return;
            }

            missionUtil.editMission(mission);
        }

        if (setupRequired && messageArgs[0].equalsIgnoreCase("!setup")) {
            this.setupRequired = false;
            event.getMessage().delete().queue();
            missionManagerUtil.create(event.getGuild(), event.getTextChannel());
        }

        if (Objects.requireNonNull(event.getTextChannel().getParent()).getId().equals(missionPublishCategory)) {
            List<Mission> unpublishedMissions = missionRepository.findByPublishedIsFalseAndEditModeIsNot(EditMode.NONE);
            for (Mission mission : unpublishedMissions) {
                if (mission.getMissionChannelID() != event.getTextChannel().getIdLong()) break;
                if (mission.getHostID() != Objects.requireNonNull(event.getMember()).getIdLong()) break;
                boolean success = true;

                switch (mission.getEditMode()) {
                    case NAME:
                        mission.setName(event.getMessage().getContentRaw());
                        String newChannelName = mission.getName().replace(" ", "-");
                        event.getTextChannel().getManager().setName(newChannelName).queue();
                        break;
                    case SUMMARY:
                        mission.setSummary(event.getMessage().getContentRaw());
                        break;
                    case LOCATION:
                        mission.setLocation(event.getMessage().getContentRaw());
                        break;
                    case START_DATE:
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                            LocalDateTime newDate = LocalDateTime.parse(event.getMessage().getContentRaw(), formatter);

                            if (newDate.isBefore(LocalDateTime.now())) {
                                success = false;
                                missionEditorUtil.sendErrorMessage("Invalid Date", "Please enter a date in the future!", event.getTextChannel());
                            } else {
                                mission.setMissionDate(newDate);
                            }
                        } catch (DateTimeParseException e) {
                            missionEditorUtil.sendErrorMessage("Invalid Date", "Please enter a date in the format described above!", event.getTextChannel());
                            success = false;
                        }
                        break;
                }

                event.getMessage().delete().queue();
                if (success) {
                    missionUtil.resetEditMode(mission);
                    missionEditorCreator.updateEditorMessage(mission);
                };
            }
        }

    }
}
