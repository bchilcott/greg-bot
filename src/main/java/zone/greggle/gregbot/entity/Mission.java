package zone.greggle.gregbot.entity;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import zone.greggle.gregbot.data.EditMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document
public class Mission {

    @Id
    public String id;
    public Long hostID;
    public String shortID;
    public String name;
    public String location;
    public String summary;
    public List<String> availableRoles;
    public LocalDateTime missionDate;
    public LocalDateTime dateCreated;
    public List<MissionMember> members;
    public Long missionChannelID;
    public Long editorMessageID;
    public Long summaryMessageID;
    public Long lastPromptID;
    public Boolean published;
    public EditMode editMode;
    public Boolean previouslyPublished;

    public Mission(Long hostID) {

        this.hostID = hostID;
        this.shortID = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
        this.name = "Untitled Mission";
        this.location = "Not Specified";
        this.summary = "This mission has no summary.";
        this.availableRoles = new ArrayList<>();
        this.missionDate = LocalDateTime.now().plusHours(1);
        this.dateCreated = LocalDateTime.now();
        this.members = new ArrayList<>();
        this.missionChannelID = null;
        this.editorMessageID = null;
        this.summaryMessageID = null;
        this.lastPromptID = null;
        this.published = false;
        this.editMode = EditMode.NONE;

        this.addMember(new MissionMember(hostID));
    }

    public String getID() {
        return id;
    }

    public Long getHostID() {
        return hostID;
    }

    public String getShortID() {
        return shortID;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getAvailableRoles() {
        return this.availableRoles;
    }
    public void addRole(String role) {
        availableRoles.add(role);
    }
    public void removeLastRole() {
        availableRoles.remove(availableRoles.size() - 1);
    }

    public LocalDateTime getMissionDate() {
        return missionDate;
    }
    public void setMissionDate(LocalDateTime missionDate) {
        this.missionDate = missionDate;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public List<MissionMember> getMembers() {
        return members;
    }
    public void addMember(MissionMember member) {
        members.add(member);
    }
    public void removeMember(MissionMember member) {
        members.remove(member);
    }

    public Long getMissionChannelID() {
        return missionChannelID;
    }
    public void setMissionChannelID(Long missionChannelID) {
        this.missionChannelID = missionChannelID;
    }

    public Long getEditorMessageID() {
        return editorMessageID;
    }
    public void setEditorMessageID(Long editorMessageID) {
        this.editorMessageID = editorMessageID;
    }

    public Long getSummaryMessageID() {
        return summaryMessageID;
    }
    public void setSummaryMessageID(Long summaryMessageID) {
        this.summaryMessageID = summaryMessageID;
    }

    public Long getLastPromptID() {
        return lastPromptID;
    }
    public void setLastPromptID(Long lastPromptID) {
        this.lastPromptID = lastPromptID;
    }

    public Boolean isPublished() {
        return published;
    }
    public void setPublished(Boolean published) {
        this.published = published;
        this.previouslyPublished = true;
    }

    public EditMode getEditMode() {
        return editMode;
    }
    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Boolean wasPreviouslyPublished() {
        return previouslyPublished;
    }

    public MissionMember getMemberByID(Long discordID) {
        for (MissionMember m : members) {
            if (m.getDiscordID().equals(discordID)) return m;
        }
        return null;
    }

    public void updateMemberRole(Long discordID, String role) {
        for (MissionMember m : members) {
            if (m.getDiscordID().equals(discordID)) {
                MissionMember memberToUpdate = getMemberByID(discordID);
                memberToUpdate.setMissionRole(role);
                memberToUpdate.setSelectingRole(false);
                members.set(members.indexOf(m), memberToUpdate);
                return;
            }
        }
    }

    public void setSelectingRole(Long discordID, boolean selectingRole) {
        for (MissionMember m : members) {
            if (m.getDiscordID().equals(discordID)) {
                MissionMember memberToUpdate = getMemberByID(discordID);
                memberToUpdate.setSelectingRole(selectingRole);
                members.set(members.indexOf(m), memberToUpdate);
                return;
            }
        }
    }
}
